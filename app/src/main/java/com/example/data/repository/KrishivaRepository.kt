package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.db.AppDatabase
import com.example.domain.models.AlertPriority
import com.example.domain.models.AlertType
import com.example.domain.models.CalibrationConfig
import com.example.domain.models.DataSource
import com.example.domain.models.Farm
import com.example.domain.models.FarmDecision
import com.example.domain.models.Farmer
import com.example.domain.models.GovernmentAdvisory
import com.example.domain.models.LiveTelemetry
import com.example.domain.models.NutrientGuidance
import com.example.domain.models.NutrientRating
import com.example.domain.models.PestStatus
import com.example.domain.models.Plot
import com.example.domain.models.SmartAlert
import com.example.domain.models.SoilReading
import com.example.domain.models.WaterGuidance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.util.UUID
import kotlin.math.roundToInt

class KrishivaRepository(
    private val database: AppDatabase,
    context: Context? = null
) {
    companion object {
        private const val PREFS_NAME = "krishiva_live_prefs"
        private const val KEY_DEVICE_TOKEN = "device_token"
        private const val KEY_DRY_RAW = "calibration_dry_raw"
        private const val KEY_WET_RAW = "calibration_wet_raw"
        private const val KEY_MANUAL_IP = "manual_phone_ip"
        private const val KEY_RECEIVER_ENABLED = "receiver_enabled"

        private const val KEY_FARMER_NAME = "farmer_name"
        private const val KEY_FARMER_PHONE = "farmer_phone"
        private const val KEY_FARMER_VILLAGE = "farmer_village"
        private const val KEY_FARMER_DISTRICT = "farmer_district"
        private const val KEY_FARMER_STATE = "farmer_state"

        private const val KEY_FARM_NAME = "farm_name"
        private const val KEY_FARM_LOCATION = "farm_location"
        private const val KEY_FARM_ACRES = "farm_acres"
        private const val KEY_FARM_CROP = "farm_crop"
        private const val KEY_PLOTS_JSON = "plots_json"

        const val BACKGROUND_HISTORY_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes
        const val LOW_MOISTURE_ALERT_COOLDOWN_MS = 2 * 60 * 60 * 1000L // 2 hours
        const val STALE_READING_THRESHOLD_MS = 30 * 60 * 1000L // 30 minutes

        fun generateRandomToken(length: Int = 24): String {
            val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
            val random = SecureRandom()
            val sb = StringBuilder(length)
            for (i in 0 until length) {
                sb.append(alphabet[random.nextInt(alphabet.length)])
            }
            return sb.toString()
        }
    }

    private val prefs: SharedPreferences? = context?.applicationContext
        ?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val soilReadingDao = database.soilReadingDao()
    private val alertDao = database.alertDao()
    private val farmDecisionDao = database.farmDecisionDao()

    private val _deviceToken = MutableStateFlow(loadOrCreateDeviceToken())
    val deviceToken: StateFlow<String> = _deviceToken.asStateFlow()

    private val _calibration = MutableStateFlow(loadCalibration())
    val calibration: StateFlow<CalibrationConfig> = _calibration.asStateFlow()

    private val _manualIpOverride = MutableStateFlow(prefs?.getString(KEY_MANUAL_IP, "") ?: "")
    val manualIpOverride: StateFlow<String> = _manualIpOverride.asStateFlow()

    private val _farmer = MutableStateFlow(loadFarmer())
    val farmer: StateFlow<Farmer> = _farmer.asStateFlow()

    private val _farm = MutableStateFlow(loadFarm())
    val farm: StateFlow<Farm> = _farm.asStateFlow()

    private val _plots = MutableStateFlow(loadPlots())
    val plots: StateFlow<List<Plot>> = _plots.asStateFlow()

    private val _pestStatus = MutableStateFlow(PestStatus())
    val pestStatus: StateFlow<PestStatus> = _pestStatus.asStateFlow()

    private val _nutrientGuidance = MutableStateFlow(NutrientGuidance())
    val nutrientGuidance: StateFlow<NutrientGuidance> = _nutrientGuidance.asStateFlow()

    val allSoilReadings: Flow<List<SoilReading>> = soilReadingDao.getAllReadings()
    val latestSoilReading: Flow<SoilReading?> = soilReadingDao.getLatestReading()
    val allAlerts: Flow<List<SmartAlert>> = alertDao.getAllAlerts()
    val unreadAlertsCount: Flow<Int> = alertDao.getUnreadCount()
    val allDecisions: Flow<List<FarmDecision>> = farmDecisionDao.getAllDecisions()
    val latestDecision: Flow<FarmDecision?> = farmDecisionDao.getLatestDecision()

    @Volatile
    private var lastAutoPersistedAtMs: Long = 0L

    @Volatile
    private var lastLowMoistureAlertAtMs: Long = 0L

    private fun loadOrCreateDeviceToken(): String {
        val existing = prefs?.getString(KEY_DEVICE_TOKEN, null)
        if (!existing.isNullOrBlank()) {
            return existing
        }
        val created = generateRandomToken()
        prefs?.edit()?.putString(KEY_DEVICE_TOKEN, created)?.apply()
        return created
    }

    fun regenerateDeviceToken(): String {
        val newToken = generateRandomToken()
        prefs?.edit()?.putString(KEY_DEVICE_TOKEN, newToken)?.apply()
        _deviceToken.value = newToken
        return newToken
    }

    private fun loadCalibration(): CalibrationConfig {
        val p = prefs ?: return CalibrationConfig()
        val dry = if (p.contains(KEY_DRY_RAW)) p.getInt(KEY_DRY_RAW, -1).takeIf { it in 0..4095 } else null
        val wet = if (p.contains(KEY_WET_RAW)) p.getInt(KEY_WET_RAW, -1).takeIf { it in 0..4095 } else null
        return CalibrationConfig(dryRaw = dry, wetRaw = wet)
    }

    fun saveDryReference(rawAdc: Int): CalibrationConfig {
        val clamped = rawAdc.coerceIn(0, 4095)
        prefs?.edit()?.putInt(KEY_DRY_RAW, clamped)?.apply()
        val updated = _calibration.value.copy(dryRaw = clamped)
        _calibration.value = updated
        return updated
    }

    fun saveWetReference(rawAdc: Int): CalibrationConfig {
        val clamped = rawAdc.coerceIn(0, 4095)
        prefs?.edit()?.putInt(KEY_WET_RAW, clamped)?.apply()
        val updated = _calibration.value.copy(wetRaw = clamped)
        _calibration.value = updated
        return updated
    }

    fun clearCalibration(): CalibrationConfig {
        prefs?.edit()?.remove(KEY_DRY_RAW)?.remove(KEY_WET_RAW)?.apply()
        val cleared = CalibrationConfig(dryRaw = null, wetRaw = null)
        _calibration.value = cleared
        return cleared
    }

    fun saveManualIpOverride(ip: String) {
        val trimmed = ip.trim()
        prefs?.edit()?.putString(KEY_MANUAL_IP, trimmed)?.apply()
        _manualIpOverride.value = trimmed
    }

    fun isReceiverEnabledPreference(): Boolean {
        return prefs?.getBoolean(KEY_RECEIVER_ENABLED, true) ?: true
    }

    fun setReceiverEnabledPreference(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_RECEIVER_ENABLED, enabled)?.apply()
    }

    private fun loadFarmer(): Farmer {
        val p = prefs ?: return Farmer()
        return Farmer(
            name = p.getString(KEY_FARMER_NAME, "") ?: "",
            phone = p.getString(KEY_FARMER_PHONE, "") ?: "",
            village = p.getString(KEY_FARMER_VILLAGE, "") ?: "",
            district = p.getString(KEY_FARMER_DISTRICT, "") ?: "",
            state = p.getString(KEY_FARMER_STATE, "") ?: ""
        )
    }

    private fun loadFarm(): Farm {
        val p = prefs ?: return Farm()
        val acresStr = p.getString(KEY_FARM_ACRES, null)
        val acres = acresStr?.toDoubleOrNull()
        val plotsList = loadPlots()
        return Farm(
            id = "farm_local",
            name = p.getString(KEY_FARM_NAME, "") ?: "",
            location = p.getString(KEY_FARM_LOCATION, "") ?: "",
            areaAcres = acres,
            mainCrop = p.getString(KEY_FARM_CROP, "") ?: "",
            activePlots = plotsList.size
        )
    }

    private fun loadPlots(): List<Plot> {
        val jsonStr = prefs?.getString(KEY_PLOTS_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<Plot>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    Plot(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.optString("name", ""),
                        areaAcres = obj.optDouble("areaAcres", 0.0),
                        soilType = obj.optString("soilType", ""),
                        moisturePercent = if (obj.has("moisturePercent") && !obj.isNull("moisturePercent")) {
                            obj.optDouble("moisturePercent")
                        } else null,
                        moistureRaw = if (obj.has("moistureRaw") && !obj.isNull("moistureRaw")) {
                            obj.optInt("moistureRaw")
                        } else null,
                        lastReadingTimestamp = if (obj.has("lastReadingTimestamp") && !obj.isNull("lastReadingTimestamp")) {
                            obj.optLong("lastReadingTimestamp")
                        } else null
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun persistPlots(plotsList: List<Plot>) {
        val arr = JSONArray()
        for (plot in plotsList) {
            val obj = JSONObject()
                .put("id", plot.id)
                .put("name", plot.name)
                .put("areaAcres", plot.areaAcres)
                .put("soilType", plot.soilType)
            if (plot.moisturePercent != null) obj.put("moisturePercent", plot.moisturePercent)
            if (plot.moistureRaw != null) obj.put("moistureRaw", plot.moistureRaw)
            if (plot.lastReadingTimestamp != null) obj.put("lastReadingTimestamp", plot.lastReadingTimestamp)
            arr.put(obj)
        }
        prefs?.edit()?.putString(KEY_PLOTS_JSON, arr.toString())?.apply()
    }

    fun addPlot(name: String, areaAcres: Double, soilType: String) {
        if (name.isBlank()) return
        val newPlot = Plot(
            id = "plot_${System.currentTimeMillis()}",
            name = name.trim(),
            areaAcres = areaAcres,
            soilType = soilType.trim().ifEmpty { "Unspecified" }
        )
        val updated = _plots.value + newPlot
        _plots.value = updated
        persistPlots(updated)
        _farm.value = _farm.value.copy(activePlots = updated.size)
    }

    fun deletePlot(plotId: String) {
        val updated = _plots.value.filterNot { it.id == plotId }
        _plots.value = updated
        persistPlots(updated)
        _farm.value = _farm.value.copy(activePlots = updated.size)
    }

    /**
     * Records a SoilReading in Room.
     * Deduplicates by (deviceId, sequenceNumber) so retried packets never insert duplicate rows.
     */
    suspend fun recordSoilReading(reading: SoilReading): Long {
        if (!reading.isValid) return -1L
        val devId = reading.deviceId
        val seq = reading.sequenceNumber
        if (!devId.isNullOrBlank() && seq != null) {
            val existing = soilReadingDao.findByDeviceAndSequence(devId, seq)
            if (existing != null) {
                return existing.id
            }
        }

        val id = soilReadingDao.insertReading(reading)
        lastAutoPersistedAtMs = reading.timestamp

        // Update manual nutrient guidance if this reading contains manually entered values
        if (reading.source == DataSource.MANUAL) {
            _nutrientGuidance.value = NutrientGuidance(
                nitrogen = reading.nitrogen,
                phosphorus = reading.phosphorus,
                potassium = reading.potassium,
                sulfur = reading.sulfur,
                ph = reading.ph,
                ecConductivity = reading.ecConductivity,
                isManuallyEntered = true,
                guidanceText = "Showing Soil Health Card values entered manually by farmer.",
                organicAdvisory = "Verify fertilizer scheduling with your local agricultural extension officer.",
                complianceStandard = "Entered manually from laboratory Soil Health Card (not measured by ESP32 probe)."
            )
        }

        // Trigger a low-moisture SmartAlert only from a real calibrated reading (< 30%) at most once every 2 hours
        val pct = reading.moisturePercent
        if (reading.source == DataSource.ESP32_HARDWARE && reading.calibrated && pct != null && pct < 30.0) {
            val now = reading.timestamp
            if (now - lastLowMoistureAlertAtMs >= LOW_MOISTURE_ALERT_COOLDOWN_MS) {
                lastLowMoistureAlertAtMs = now
                alertDao.insertAlert(
                    SmartAlert(
                        timestamp = now,
                        type = AlertType.SOIL,
                        title = "Low Relative Soil Moisture (${pct.roundToInt()}%)",
                        message = "ESP32 sensor (${reading.deviceId ?: "probe"}) reported ${String.format("%.1f", pct)}% calibrated relative moisture (Raw ADC: ${reading.moistureRaw}). Inspect field moisture before irrigation.",
                        priority = AlertPriority.HIGH
                    )
                )
            }
        }
        return id
    }

    /**
     * Called when a background 2-second packet arrives.
     * Avoids spamming Room on every 2-second packet: only logs a history row if not a duplicate sequence
     * and at least BACKGROUND_HISTORY_INTERVAL_MS (5 minutes) have passed since the last saved reading.
     */
    suspend fun onLivePacketReceived(telemetry: LiveTelemetry, isDuplicateSequence: Boolean) {
        if (isDuplicateSequence) return
        val now = telemetry.receivedAtMs
        if (lastAutoPersistedAtMs == 0L || now - lastAutoPersistedAtMs >= BACKGROUND_HISTORY_INTERVAL_MS) {
            val calibratedPct = telemetry.calibratedMoisturePercent
            val reading = SoilReading(
                timestamp = now,
                moistureRaw = telemetry.moistureRaw,
                moisturePercent = calibratedPct,
                calibrated = calibratedPct != null,
                deviceId = telemetry.deviceId,
                sequenceNumber = telemetry.sequence,
                wifiRssiDbm = telemetry.wifiRssiDbm,
                firmwareVersion = telemetry.firmwareVersion,
                source = DataSource.ESP32_HARDWARE,
                note = if (calibratedPct != null) {
                    "Auto telemetry log • Raw ADC: ${telemetry.moistureRaw} (${String.format("%.1f", calibratedPct)}%)"
                } else {
                    "Auto telemetry log • Raw ADC: ${telemetry.moistureRaw} (Uncalibrated)"
                }
            )
            recordSoilReading(reading)
        }
    }

    suspend fun saveManualSoilHealthCard(
        ph: Double?,
        nitrogen: NutrientRating?,
        phosphorus: NutrientRating?,
        potassium: NutrientRating?,
        sulfur: NutrientRating?,
        ec: Double?,
        latestMoistureReading: SoilReading?
    ): Long {
        val reading = SoilReading(
            timestamp = System.currentTimeMillis(),
            moistureRaw = latestMoistureReading?.moistureRaw,
            moisturePercent = latestMoistureReading?.moisturePercent,
            calibrated = latestMoistureReading?.calibrated ?: false,
            deviceId = latestMoistureReading?.deviceId,
            sequenceNumber = null,
            wifiRssiDbm = latestMoistureReading?.wifiRssiDbm,
            firmwareVersion = latestMoistureReading?.firmwareVersion,
            ph = ph,
            temperatureCelsius = null,
            nitrogen = nitrogen,
            phosphorus = phosphorus,
            potassium = potassium,
            sulfur = sulfur,
            ecConductivity = ec,
            isValid = true,
            source = DataSource.MANUAL,
            note = "Entered manually from Soil Health Card"
        )
        return recordSoilReading(reading)
    }

    fun buildWaterGuidance(
        liveTelemetry: LiveTelemetry?,
        latestStored: SoilReading?,
        history: List<SoilReading>,
        nowMs: Long = System.currentTimeMillis()
    ): WaterGuidance {
        val rawAdc = liveTelemetry?.moistureRaw ?: latestStored?.moistureRaw
        val moisturePct = liveTelemetry?.calibratedMoisturePercent ?: latestStored?.moisturePercent
        val timestamp = liveTelemetry?.receivedAtMs ?: latestStored?.timestamp
        val isCalibrated = moisturePct != null

        val recentCalibratedHistory = history
            .filter { it.calibrated && it.moisturePercent != null }
            .take(7)
            .mapNotNull { it.moisturePercent }
            .reversed()

        if (rawAdc == null && moisturePct == null) {
            return WaterGuidance(
                currentMoisturePercent = null,
                currentMoistureRaw = null,
                isCalibrated = false,
                lastScanTimestamp = null,
                isStale = false,
                statusText = "WAITING FOR SENSOR",
                selectedCrop = _farm.value.mainCrop.ifBlank { "Not selected" },
                recommendedAction = "Connect ESP32 soil-moisture sensor and perform a live scan.",
                conditionSummary = "No soil-moisture measurement has been received yet.",
                cropRequirement = "Calibrate dry and wet soil references on your target field first.",
                guidanceReason = "Water guidance is unavailable until the ESP32 sensor sends valid ADC readings.",
                recentMoistureHistory = recentCalibratedHistory
            )
        }

        val isStale = timestamp != null && (nowMs - timestamp > STALE_READING_THRESHOLD_MS)

        if (!isCalibrated || moisturePct == null) {
            return WaterGuidance(
                currentMoisturePercent = null,
                currentMoistureRaw = rawAdc,
                isCalibrated = false,
                lastScanTimestamp = timestamp,
                isStale = isStale,
                statusText = "CALIBRATION REQUIRED",
                selectedCrop = _farm.value.mainCrop.ifBlank { "Not selected" },
                recommendedAction = "Save Dry and Wet soil references in Sensor Device / Calibration.",
                conditionSummary = "Raw ADC reading ($rawAdc) received, but dry/wet references are not calibrated yet.",
                cropRequirement = "Relative moisture (%) cannot be computed from raw ADC counts without field calibration.",
                guidanceReason = "Place the sensor in representative dry soil and thoroughly watered soil from your field to set references.",
                recentMoistureHistory = recentCalibratedHistory
            )
        }

        val (status, action, summary) = when {
            moisturePct < 30.0 -> Triple(
                "LOW RELATIVE MOISTURE",
                "Soil is near your dry calibration reference. Inspect root-zone soil and consider irrigation.",
                "Calibrated relative moisture estimate is ${String.format("%.1f", moisturePct)}% (Raw ADC: ${rawAdc ?: "N/A"})."
            )
            moisturePct <= 65.0 -> Triple(
                "MODERATE RELATIVE MOISTURE",
                "Relative moisture is within a moderate range between your dry and wet references.",
                "Calibrated relative moisture estimate is ${String.format("%.1f", moisturePct)}% (Raw ADC: ${rawAdc ?: "N/A"})."
            )
            else -> Triple(
                "HIGH RELATIVE MOISTURE",
                "Soil is near your wet calibration reference. Avoid additional irrigation and check drainage.",
                "Calibrated relative moisture estimate is ${String.format("%.1f", moisturePct)}% (Raw ADC: ${rawAdc ?: "N/A"})."
            )
        }

        return WaterGuidance(
            currentMoisturePercent = moisturePct,
            currentMoistureRaw = rawAdc,
            isCalibrated = true,
            lastScanTimestamp = timestamp,
            isStale = isStale,
            statusText = if (isStale) "$status (STALE)" else status,
            selectedCrop = _farm.value.mainCrop.ifBlank { "General Field Crop" },
            recommendedAction = action,
            conditionSummary = summary,
            cropRequirement = "Relative moisture (%) is scaled between your saved dry and wet soil references (not volumetric water content).",
            guidanceReason = if (isStale) {
                "Last reading is older than 30 minutes. Run a fresh scan before making irrigation decisions."
            } else {
                "Derived directly from live ESP32 GPIO34 ADC reading ($rawAdc) and your saved dry/wet calibration."
            },
            recentMoistureHistory = recentCalibratedHistory
        )
    }

    suspend fun saveFarmDecision(decision: FarmDecision): Long {
        return farmDecisionDao.insertDecision(decision)
    }

    suspend fun markAlertAsRead(id: Long) {
        alertDao.markAsRead(id)
    }

    suspend fun markAllAlertsAsRead() {
        alertDao.markAllAsRead()
    }

    suspend fun clearAllAlerts() {
        alertDao.clearAll()
    }

    suspend fun clearRecordedHistory() {
        soilReadingDao.clearAll()
        alertDao.clearAll()
        farmDecisionDao.clearAll()
        lastAutoPersistedAtMs = 0L
        lastLowMoistureAlertAtMs = 0L
    }

    fun updateFarmerProfile(name: String, phone: String, village: String, district: String, state: String) {
        val updated = Farmer(
            name = name.trim(),
            phone = phone.trim(),
            village = village.trim(),
            district = district.trim(),
            state = state.trim()
        )
        _farmer.value = updated
        prefs?.edit()
            ?.putString(KEY_FARMER_NAME, updated.name)
            ?.putString(KEY_FARMER_PHONE, updated.phone)
            ?.putString(KEY_FARMER_VILLAGE, updated.village)
            ?.putString(KEY_FARMER_DISTRICT, updated.district)
            ?.putString(KEY_FARMER_STATE, updated.state)
            ?.apply()
    }

    fun updateFarmDetails(name: String, location: String, areaAcres: Double?, mainCrop: String) {
        val updated = _farm.value.copy(
            name = name.trim(),
            location = location.trim(),
            areaAcres = areaAcres,
            mainCrop = mainCrop.trim(),
            activePlots = _plots.value.size
        )
        _farm.value = updated
        prefs?.edit()
            ?.putString(KEY_FARM_NAME, updated.name)
            ?.putString(KEY_FARM_LOCATION, updated.location)
            ?.putString(KEY_FARM_ACRES, areaAcres?.toString() ?: "")
            ?.putString(KEY_FARM_CROP, updated.mainCrop)
            ?.apply()
    }

    fun getGovernmentAdvisories(): List<GovernmentAdvisory> {
        return listOf(
            GovernmentAdvisory(
                id = "adv_shc",
                title = "Soil Health Card (SHC) Laboratory Testing",
                issuingBody = "Department of Agriculture & Farmers Welfare (Static Reference)",
                dateText = "Reference Guideline",
                summary = "Because the portable ESP32 unit measures soil moisture only, obtain a laboratory Soil Health Card for pH, EC, organic carbon, and N-P-K-S before applying fertilizers.",
                badge = "Soil Testing Reference"
            ),
            GovernmentAdvisory(
                id = "adv_msp",
                title = "Minimum Support Price (MSP) & APMC Verification",
                issuingBody = "Commission for Agricultural Costs & Prices (Static Reference)",
                dateText = "Reference Guideline",
                summary = "Live mandi prices are not connected to this offline prototype. Verify seasonal MSP rates and daily arrivals at your local APMC mandi or official e-NAM portal.",
                badge = "Market Reference"
            ),
            GovernmentAdvisory(
                id = "adv_ipm",
                title = "Integrated Pest Management (IPM) Field Scouting",
                issuingBody = "Directorate of Plant Protection (Static Reference)",
                dateText = "Reference Guideline",
                summary = "No electronic pest sensor is connected to this unit. Perform regular visual field scouting and consult district agricultural extension bulletins.",
                badge = "Field Scouting"
            )
        )
    }
}
