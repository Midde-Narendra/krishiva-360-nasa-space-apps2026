package com.example.hardware

import com.example.domain.models.CalibrationConfig
import com.example.domain.models.DataSource
import com.example.domain.models.Esp32TelemetryStatus
import com.example.domain.models.LiveTelemetry
import com.example.domain.models.ReceiverStatus
import com.example.domain.models.SensorDevice
import com.example.domain.models.SoilReading
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.IOException
import java.net.Inet4Address
import java.net.NetworkInterface
import java.security.MessageDigest
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

enum class ScanStage(val displayText: String, val progress: Float) {
    IDLE("Ready to scan", 0.0f),
    WAITING_FOR_PACKET("Waiting for fresh ESP32 telemetry packet on port 8080...", 0.45f),
    VALIDATING("Validating ADC reading & calibration...", 0.85f),
    COMPLETE("Live ESP32 scan recorded", 1.0f),
    TIMEOUT_ERROR("No fresh ESP32 reading received. Check Wi-Fi, IP, port and token.", 0.0f),
    RECEIVER_STOPPED_ERROR("Phone receiver is stopped. Enable Live Connection first.", 0.0f)
}

sealed class LiveScanResult {
    data class Success(val reading: SoilReading, val telemetry: LiveTelemetry) : LiveScanResult()
    data class Timeout(val message: String) : LiveScanResult()
    data class Error(val message: String) : LiveScanResult()
}

class ESP32ConnectionService(
    initialToken: String = "UNINITIALIZED_TOKEN",
    initialCalibration: CalibrationConfig = CalibrationConfig(),
    initialPort: Int = DEFAULT_PORT,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val onTelemetryAccepted: ((telemetry: LiveTelemetry, isDuplicateSequence: Boolean) -> Unit)? = null
) {
    companion object {
        const val DEFAULT_PORT = 8080
        const val PROTOCOL_VERSION = "1.0"
        const val TELEMETRY_FRESHNESS_TIMEOUT_MS = 10_000L
        const val DEFAULT_SCAN_TIMEOUT_MS = 8_000L

        fun detectLocalIpv4Address(): String? {
            return try {
                val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
                val candidates = mutableListOf<String>()
                for (intf in interfaces) {
                    if (!intf.isUp || intf.isLoopback) continue
                    val addrs = Collections.list(intf.inetAddresses)
                    for (addr in addrs) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            val ip = addr.hostAddress ?: continue
                            if (!ip.startsWith("169.254.")) {
                                candidates.add(ip)
                            }
                        }
                    }
                }
                candidates.firstOrNull { it.startsWith("192.168.") }
                    ?: candidates.firstOrNull { it.startsWith("10.") }
                    ?: candidates.firstOrNull { it.startsWith("172.") }
                    ?: candidates.firstOrNull()
            } catch (e: Exception) {
                null
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var watchdogJob: Job? = null
    private var httpServer: TelemetryHttpServer? = null
    private val serverLock = Any()

    @Volatile
    private var deviceToken: String = initialToken

    private val _port = MutableStateFlow(initialPort)
    val port: StateFlow<Int> = _port.asStateFlow()

    private val _calibration = MutableStateFlow(initialCalibration)
    val calibration: StateFlow<CalibrationConfig> = _calibration.asStateFlow()

    private val _receiverStatus = MutableStateFlow(ReceiverStatus.STOPPED)
    val receiverStatus: StateFlow<ReceiverStatus> = _receiverStatus.asStateFlow()

    private val _esp32Status = MutableStateFlow(Esp32TelemetryStatus.WAITING)
    val esp32Status: StateFlow<Esp32TelemetryStatus> = _esp32Status.asStateFlow()

    private val _latestTelemetry = MutableStateFlow<LiveTelemetry?>(null)
    val latestTelemetry: StateFlow<LiveTelemetry?> = _latestTelemetry.asStateFlow()

    private val _deviceInfo = MutableStateFlow(
        SensorDevice(
            receiverEnabled = false,
            receiverStatus = ReceiverStatus.STOPPED,
            receiverPort = initialPort,
            detectedPhoneIp = detectLocalIpv4Address(),
            esp32Status = Esp32TelemetryStatus.WAITING
        )
    )
    val deviceInfo: StateFlow<SensorDevice> = _deviceInfo.asStateFlow()

    private val _currentScanStage = MutableStateFlow(ScanStage.IDLE)
    val currentScanStage: StateFlow<ScanStage> = _currentScanStage.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // Track received sequence numbers per deviceId to avoid duplicate history inserts on retries
    private val seenDeviceSequences = ConcurrentHashMap<String, MutableSet<Long>>()
    private val packetCounter = AtomicLong(0L)

    @Volatile
    private var pendingScanDeferred: CompletableDeferred<LiveTelemetry>? = null

    @Volatile
    private var pendingScanStartedAtMs: Long = 0L

    fun updateDeviceToken(newToken: String) {
        if (newToken.isNotBlank()) {
            deviceToken = newToken
        }
    }

    fun getDeviceToken(): String = deviceToken

    fun updateCalibration(config: CalibrationConfig) {
        _calibration.value = config
        val current = _latestTelemetry.value
        if (current != null) {
            val recalibrated = config.computeRelativeMoisturePercent(current.moistureRaw)
            val updated = current.copy(calibratedMoisturePercent = recalibrated)
            _latestTelemetry.value = updated
            _deviceInfo.value = _deviceInfo.value.copy(
                lastCalibratedPercent = recalibrated
            )
        }
    }

    fun updateManualIpOverride(manualIp: String) {
        _deviceInfo.value = _deviceInfo.value.copy(
            manualPhoneIpOverride = manualIp.trim(),
            detectedPhoneIp = detectLocalIpv4Address() ?: _deviceInfo.value.detectedPhoneIp
        )
    }

    fun refreshLocalIpAddress() {
        _deviceInfo.value = _deviceInfo.value.copy(
            detectedPhoneIp = detectLocalIpv4Address()
        )
    }

    /**
     * Starts the embedded HTTP server bound to 0.0.0.0 on the configured port (default 8080).
     * Never marks ESP32 as connected merely because the phone receiver is running.
     */
    fun startReceiver(portToBind: Int = _port.value): Boolean {
        synchronized(serverLock) {
            if (httpServer?.isAlive == true && _port.value == portToBind) {
                _receiverStatus.value = ReceiverStatus.RUNNING
                refreshConnectionFreshness()
                syncDeviceState(enabled = true, error = null)
                return true
            }
            stopReceiverInternal()
            _port.value = portToBind
            return try {
                val server = TelemetryHttpServer(portToBind)
                server.start(5000, false)
                httpServer = server
                _receiverStatus.value = ReceiverStatus.RUNNING
                refreshConnectionFreshness()
                syncDeviceState(enabled = true, error = null)
                startWatchdogLoop()
                true
            } catch (e: IOException) {
                val errMsg = "Failed to bind port $portToBind: ${e.localizedMessage ?: "Port in use"}"
                _receiverStatus.value = ReceiverStatus.ERROR
                syncDeviceState(enabled = false, error = errMsg)
                false
            }
        }
    }

    /**
     * Stops the embedded HTTP receiver and clears the watchdog loop.
     */
    fun stopReceiver() {
        synchronized(serverLock) {
            stopReceiverInternal()
            _receiverStatus.value = ReceiverStatus.STOPPED
            val currentEspStatus = if (_latestTelemetry.value != null) {
                Esp32TelemetryStatus.DISCONNECTED
            } else {
                Esp32TelemetryStatus.WAITING
            }
            _esp32Status.value = currentEspStatus
            syncDeviceState(enabled = false, error = null)
        }
    }

    /**
     * Genuinely restarts the local HTTP receiver, refreshes local IPv4 detection,
     * and re-evaluates ESP32 packet freshness. Never fakes a connected boolean.
     */
    fun reconnect(): Boolean {
        synchronized(serverLock) {
            val targetPort = _port.value
            stopReceiverInternal()
            refreshConnectionFreshness()
            return startReceiver(targetPort)
        }
    }

    private fun stopReceiverInternal() {
        watchdogJob?.cancel()
        watchdogJob = null
        try {
            httpServer?.stop()
        } catch (_: Exception) {
        }
        httpServer = null
    }

    private fun startWatchdogLoop() {
        watchdogJob?.cancel()
        watchdogJob = serviceScope.launch {
            while (isActive) {
                refreshConnectionFreshness()
                delay(1_000L)
            }
        }
    }

    /**
     * Evaluates whether the latest authenticated telemetry packet was received within the last 10 seconds.
     * Can also be invoked directly in unit tests with a synthetic clock.
     */
    fun refreshConnectionFreshness(nowMs: Long = clock()): Esp32TelemetryStatus {
        val latest = _latestTelemetry.value
        val newStatus = when {
            latest == null -> Esp32TelemetryStatus.WAITING
            nowMs - latest.receivedAtMs <= TELEMETRY_FRESHNESS_TIMEOUT_MS -> Esp32TelemetryStatus.CONNECTED
            else -> Esp32TelemetryStatus.DISCONNECTED
        }
        _esp32Status.value = newStatus
        _deviceInfo.value = _deviceInfo.value.copy(
            esp32Status = newStatus,
            receiverStatus = _receiverStatus.value
        )
        return newStatus
    }

    private fun syncDeviceState(enabled: Boolean, error: String?) {
        val latest = _latestTelemetry.value
        _deviceInfo.value = _deviceInfo.value.copy(
            receiverEnabled = enabled,
            receiverStatus = _receiverStatus.value,
            receiverPort = _port.value,
            receiverError = error,
            detectedPhoneIp = detectLocalIpv4Address() ?: _deviceInfo.value.detectedPhoneIp,
            esp32Status = _esp32Status.value,
            deviceId = latest?.deviceId,
            firmwareVersion = latest?.firmwareVersion,
            wifiRssiDbm = latest?.wifiRssiDbm,
            lastSequence = latest?.sequence,
            lastReceivedAtMs = latest?.receivedAtMs,
            lastMoistureRaw = latest?.moistureRaw,
            lastCalibratedPercent = latest?.calibratedMoisturePercent
        )
    }

    /**
     * Waits for a fresh authenticated telemetry packet arriving after the scan began (up to timeoutMs, default 8s).
     * Never fabricates a reading on timeout or error.
     */
    suspend fun executeLiveScan(
        timeoutMs: Long = DEFAULT_SCAN_TIMEOUT_MS,
        onProgress: (ScanStage) -> Unit = {}
    ): LiveScanResult {
        if (_receiverStatus.value != ReceiverStatus.RUNNING) {
            val started = startReceiver(_port.value)
            if (!started) {
                _currentScanStage.value = ScanStage.RECEIVER_STOPPED_ERROR
                onProgress(ScanStage.RECEIVER_STOPPED_ERROR)
                return LiveScanResult.Error(
                    _deviceInfo.value.receiverError
                        ?: "Phone HTTP receiver is not running on port ${_port.value}."
                )
            }
        }

        _isScanning.value = true
        val scanStartMs = clock()
        pendingScanStartedAtMs = scanStartMs
        val deferred = CompletableDeferred<LiveTelemetry>()
        pendingScanDeferred = deferred

        _currentScanStage.value = ScanStage.WAITING_FOR_PACKET
        onProgress(ScanStage.WAITING_FOR_PACKET)

        val freshPacket = withTimeoutOrNull(timeoutMs) {
            deferred.await()
        }

        pendingScanDeferred = null

        if (freshPacket == null) {
            _isScanning.value = false
            _currentScanStage.value = ScanStage.TIMEOUT_ERROR
            onProgress(ScanStage.TIMEOUT_ERROR)
            refreshConnectionFreshness()
            return LiveScanResult.Timeout(
                "No fresh ESP32 reading received. Check Wi-Fi, IP, port and token."
            )
        }

        _currentScanStage.value = ScanStage.VALIDATING
        onProgress(ScanStage.VALIDATING)

        val calibratedPct = _calibration.value.computeRelativeMoisturePercent(freshPacket.moistureRaw)
        val noteText = buildString {
            append("ESP32 Live Scan • Raw ADC: ${freshPacket.moistureRaw}")
            if (calibratedPct != null) {
                append(" • Relative Moisture: ${String.format("%.1f", calibratedPct)}%")
            } else {
                append(" • Calibration required")
            }
            if (freshPacket.isSensorRailWarning) {
                append(" • Warning: sensor reading at rail limit")
            }
        }

        val reading = SoilReading(
            timestamp = freshPacket.receivedAtMs,
            moistureRaw = freshPacket.moistureRaw,
            moisturePercent = calibratedPct,
            calibrated = calibratedPct != null,
            deviceId = freshPacket.deviceId,
            sequenceNumber = freshPacket.sequence,
            wifiRssiDbm = freshPacket.wifiRssiDbm,
            firmwareVersion = freshPacket.firmwareVersion,
            ph = null,
            temperatureCelsius = null,
            nitrogen = null,
            phosphorus = null,
            potassium = null,
            sulfur = null,
            ecConductivity = null,
            isValid = true,
            source = DataSource.ESP32_HARDWARE,
            note = noteText
        )

        _isScanning.value = false
        _currentScanStage.value = ScanStage.COMPLETE
        onProgress(ScanStage.COMPLETE)

        return LiveScanResult.Success(reading, freshPacket)
    }

    fun resetIdle() {
        _currentScanStage.value = ScanStage.IDLE
        _isScanning.value = false
    }

    /**
     * Processes an incoming HTTP telemetry POST body and token.
     * Exposed internally so both NanoHTTPD and direct unit tests can verify exact status codes and JSON responses.
     */
    internal fun handleTelemetryPost(
        providedToken: String?,
        rawBody: String?
    ): Pair<NanoHTTPD.Response.Status, String> {
        if (!isTokenValid(providedToken)) {
            return NanoHTTPD.Response.Status.UNAUTHORIZED to
                JSONObject()
                    .put("ok", false)
                    .put("error", "Unauthorized: invalid or missing X-Device-Token")
                    .toString()
        }

        if (rawBody.isNullOrBlank()) {
            return NanoHTTPD.Response.Status.BAD_REQUEST to
                JSONObject()
                    .put("ok", false)
                    .put("error", "Empty request body")
                    .toString()
        }

        val json = try {
            JSONObject(rawBody)
        } catch (e: Exception) {
            return NanoHTTPD.Response.Status.BAD_REQUEST to
                JSONObject()
                    .put("ok", false)
                    .put("error", "Malformed JSON payload")
                    .toString()
        }

        if (!json.has("moistureRaw") || json.isNull("moistureRaw")) {
            return NanoHTTPD.Response.Status.BAD_REQUEST to
                JSONObject()
                    .put("ok", false)
                    .put("error", "Missing required field: moistureRaw")
                    .toString()
        }

        val moistureRaw = try {
            val rawVal = json.get("moistureRaw")
            when (rawVal) {
                is Int -> rawVal
                is Long -> rawVal.toInt()
                is Number -> {
                    val d = rawVal.toDouble()
                    if (d % 1.0 != 0.0) return badRequest("moistureRaw must be an integer")
                    d.toInt()
                }
                else -> return badRequest("moistureRaw must be an integer")
            }
        } catch (e: Exception) {
            return badRequest("Invalid moistureRaw value")
        }

        if (moistureRaw !in 0..4095) {
            return badRequest("moistureRaw out of 12-bit ADC range (0..4095): $moistureRaw")
        }

        val deviceId = json.optString("deviceId", "").trim()
        if (deviceId.isEmpty()) {
            return badRequest("Missing or blank required field: deviceId")
        }

        if (!json.has("sequence") || json.isNull("sequence")) {
            return badRequest("Missing required field: sequence")
        }
        val sequence = try {
            val seqObj = json.get("sequence")
            if (seqObj !is Number) return badRequest("sequence must be a number")
            seqObj.toLong()
        } catch (e: Exception) {
            return badRequest("Invalid sequence number")
        }
        if (sequence < 0L) {
            return badRequest("sequence must be >= 0")
        }

        val uptimeMs = if (json.has("uptimeMs") && !json.isNull("uptimeMs")) {
            val upObj = json.opt("uptimeMs")
            if (upObj !is Number || upObj.toLong() < 0L) {
                return badRequest("uptimeMs must be a non-negative integer")
            }
            upObj.toLong()
        } else {
            0L
        }

        val wifiRssiDbm: Int? = if (json.has("wifiRssiDbm") && !json.isNull("wifiRssiDbm")) {
            val rssiObj = json.opt("wifiRssiDbm")
            if (rssiObj !is Number) return badRequest("wifiRssiDbm must be an integer")
            val rssi = rssiObj.toInt()
            if (rssi !in -140..0) return badRequest("wifiRssiDbm out of valid dBm range (-140..0)")
            rssi
        } else {
            null
        }

        val firmwareVersion: String? = if (json.has("firmwareVersion") && !json.isNull("firmwareVersion")) {
            json.optString("firmwareVersion", "").trim().ifEmpty { null }
        } else {
            null
        }

        val receivedAtMs = clock()
        val calibratedMoisture = _calibration.value.computeRelativeMoisturePercent(moistureRaw)

        val telemetry = LiveTelemetry(
            deviceId = deviceId,
            sequence = sequence,
            uptimeMs = uptimeMs,
            moistureRaw = moistureRaw,
            calibratedMoisturePercent = calibratedMoisture,
            wifiRssiDbm = wifiRssiDbm,
            firmwareVersion = firmwareVersion,
            receivedAtMs = receivedAtMs
        )

        val deviceSeqSet = seenDeviceSequences.getOrPut(deviceId) {
            Collections.synchronizedSet( LinkedHashSet<Long>() )
        }
        val isDuplicateSequence = !deviceSeqSet.add(sequence)
        if (deviceSeqSet.size > 500) {
            synchronized(deviceSeqSet) {
                val iterator = deviceSeqSet.iterator()
                if (iterator.hasNext()) {
                    iterator.next()
                    iterator.remove()
                }
            }
        }

        packetCounter.incrementAndGet()
        _latestTelemetry.value = telemetry
        _esp32Status.value = Esp32TelemetryStatus.CONNECTED
        syncDeviceState(enabled = _receiverStatus.value == ReceiverStatus.RUNNING, error = null)

        val scanWaiter = pendingScanDeferred
        if (scanWaiter != null && !scanWaiter.isCompleted && receivedAtMs >= pendingScanStartedAtMs) {
            scanWaiter.complete(telemetry)
        }

        onTelemetryAccepted?.invoke(telemetry, isDuplicateSequence)

        val responseJson = JSONObject()
            .put("ok", true)
            .put("deviceId", deviceId)
            .put("sequence", sequence)
            .put("receivedAtMs", receivedAtMs)
            .put("moistureRaw", moistureRaw)
            .put("calibrated", calibratedMoisture != null)
            .put("duplicateSequence", isDuplicateSequence)
        if (calibratedMoisture != null) {
            responseJson.put("calibratedMoisturePercent", calibratedMoisture)
        } else {
            responseJson.put("calibratedMoisturePercent", JSONObject.NULL)
        }

        return NanoHTTPD.Response.Status.OK to responseJson.toString()
    }

    private fun badRequest(message: String): Pair<NanoHTTPD.Response.Status, String> {
        return NanoHTTPD.Response.Status.BAD_REQUEST to
            JSONObject()
                .put("ok", false)
                .put("error", message)
                .toString()
    }

    private fun isTokenValid(provided: String?): Boolean {
        val expected = deviceToken
        if (provided.isNullOrBlank() || expected.isBlank() || expected == "UNINITIALIZED_TOKEN") {
            return false
        }
        return MessageDigest.isEqual(
            provided.trim().toByteArray(Charsets.UTF_8),
            expected.trim().toByteArray(Charsets.UTF_8)
        )
    }

    private inner class TelemetryHttpServer(port: Int) : NanoHTTPD("0.0.0.0", port) {
        override fun serve(session: IHTTPSession): Response {
            return try {
                val uri = session.uri ?: "/"
                val method = session.method

                when {
                    method == Method.GET && uri == "/api/health" -> {
                        val freshness = refreshConnectionFreshness()
                        val body = JSONObject()
                            .put("ok", true)
                            .put("status", "running")
                            .put("protocolVersion", PROTOCOL_VERSION)
                            .put("port", listeningPort)
                            .put("esp32Status", freshness.name)
                            .put("esp32Connected", freshness == Esp32TelemetryStatus.CONNECTED)
                            .toString()
                        newFixedLengthResponse(Response.Status.OK, "application/json", body)
                    }

                    method == Method.GET && uri == "/api/telemetry/latest" -> {
                        val token = session.headers["x-device-token"] ?: session.headers["X-Device-Token"]
                        if (!isTokenValid(token)) {
                            val err = JSONObject()
                                .put("ok", false)
                                .put("error", "Unauthorized: invalid or missing X-Device-Token")
                                .toString()
                            return newFixedLengthResponse(Response.Status.UNAUTHORIZED, "application/json", err)
                        }
                        val freshness = refreshConnectionFreshness()
                        val latest = _latestTelemetry.value
                        val body = if (latest == null) {
                            JSONObject()
                                .put("ok", true)
                                .put("hasReading", false)
                                .put("esp32Status", freshness.name)
                                .toString()
                        } else {
                            JSONObject()
                                .put("ok", true)
                                .put("hasReading", true)
                                .put("esp32Status", freshness.name)
                                .put("deviceId", latest.deviceId)
                                .put("sequence", latest.sequence)
                                .put("uptimeMs", latest.uptimeMs)
                                .put("moistureRaw", latest.moistureRaw)
                                .put("calibrated", latest.calibratedMoisturePercent != null)
                                .put(
                                    "calibratedMoisturePercent",
                                    latest.calibratedMoisturePercent ?: JSONObject.NULL
                                )
                                .put("wifiRssiDbm", latest.wifiRssiDbm ?: JSONObject.NULL)
                                .put("firmwareVersion", latest.firmwareVersion ?: JSONObject.NULL)
                                .put("receivedAtMs", latest.receivedAtMs)
                                .toString()
                        }
                        newFixedLengthResponse(Response.Status.OK, "application/json", body)
                    }

                    method == Method.POST && uri == "/api/telemetry" -> {
                        val token = session.headers["x-device-token"] ?: session.headers["X-Device-Token"]
                        val files = HashMap<String, String>()
                        session.parseBody(files)
                        val postData = files["postData"] ?: ""
                        val (status, responseBody) = handleTelemetryPost(token, postData)
                        newFixedLengthResponse(status, "application/json", responseBody)
                    }

                    else -> {
                        val notFound = JSONObject()
                            .put("ok", false)
                            .put("error", "Endpoint not found: ${method.name} $uri")
                            .toString()
                        newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", notFound)
                    }
                }
            } catch (e: Exception) {
                val err = JSONObject()
                    .put("ok", false)
                    .put("error", "Internal server error")
                    .toString()
                newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", err)
            }
        }
    }
}
