package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.repository.KrishivaRepository
import com.example.domain.engine.KrishivaDecisionEngine
import com.example.domain.models.CalibrationConfig
import com.example.domain.models.Esp32TelemetryStatus
import com.example.domain.models.ReceiverStatus
import com.example.hardware.ESP32ConnectionService
import com.example.hardware.LiveScanResult
import com.example.hardware.ScanStage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: KrishivaRepository
    private lateinit var esp32Service: ESP32ConnectionService

    private val testPort = 18080
    private val testToken = "test_secret_token_12345678"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("krishiva_live_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = KrishivaRepository(
            database = database,
            context = context
        )
        esp32Service = ESP32ConnectionService(
            initialToken = testToken,
            initialCalibration = repository.calibration.value,
            initialPort = testPort
        )
    }

    @After
    fun tearDown() {
        esp32Service.stopReceiver()
        database.close()
    }

    @Test
    fun test1_freshInstall_showsNoDemoReadingsAndNotConnectedStatus() = runBlocking {
        // 1. Fresh install / cleared app storage shows no fake soil readings, no demo alerts,
        // no demo decisions, and not-connected ESP32 status.
        val readings = repository.allSoilReadings.first()
        val alerts = repository.allAlerts.first()
        val decisions = repository.allDecisions.first()

        assertTrue("Fresh install must have 0 soil readings", readings.isEmpty())
        assertTrue("Fresh install must have 0 alerts", alerts.isEmpty())
        assertTrue("Fresh install must have 0 decisions", decisions.isEmpty())
        assertFalse(esp32Service.deviceInfo.value.isEsp32Connected)
        assertNull(esp32Service.latestTelemetry.value)
    }

    @Test
    fun test2_and_3_and_4_and_5_httpServerHealthAuthAndValidation() = runBlocking {
        val started = esp32Service.startReceiver(portToBind = testPort)
        assertTrue("Embedded HTTP server must start on testPort", started)
        assertEquals(ReceiverStatus.RUNNING, esp32Service.receiverStatus.value)
        assertEquals(Esp32TelemetryStatus.WAITING, esp32Service.esp32Status.value)

        // 2. GET /api/health returns 200 OK JSON
        val healthConn = (URL("http://127.0.0.1:$testPort/api/health").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
        }
        assertEquals(200, healthConn.responseCode)
        val healthJson = JSONObject(healthConn.inputStream.bufferedReader().readText())
        assertTrue(healthJson.getBoolean("ok"))
        assertEquals("running", healthJson.getString("status"))
        assertEquals("1.0", healthJson.getString("protocolVersion"))

        // 3. POST /api/telemetry with valid token and moistureRaw: 2450 returns 200 and updates StateFlow
        val validBody = """
            {
              "deviceId": "krishiva-esp32-01",
              "sequence": 10,
              "uptimeMs": 42000,
              "moistureRaw": 2450,
              "wifiRssiDbm": -61,
              "firmwareVersion": "1.0.0"
            }
        """.trimIndent()
        val postValid = sendPost("http://127.0.0.1:$testPort/api/telemetry", validBody, testToken)
        assertEquals(200, postValid.first)
        val telemetry = esp32Service.latestTelemetry.value
        assertNotNull(telemetry)
        assertEquals(2450, telemetry?.moistureRaw)
        assertEquals(10L, telemetry?.sequence)
        assertEquals(-61, telemetry?.wifiRssiDbm)
        assertEquals(Esp32TelemetryStatus.CONNECTED, esp32Service.esp32Status.value)

        // 4. POST /api/telemetry with wrong token returns 401 and does not overwrite StateFlow
        val wrongTokenBody = """
            {
              "deviceId": "krishiva-esp32-01",
              "sequence": 11,
              "uptimeMs": 44000,
              "moistureRaw": 1111
            }
        """.trimIndent()
        val postUnauthorized = sendPost("http://127.0.0.1:$testPort/api/telemetry", wrongTokenBody, "wrong_token")
        assertEquals(401, postUnauthorized.first)
        assertEquals("Moisture raw must remain 2450 after 401", 2450, esp32Service.latestTelemetry.value?.moistureRaw)

        // 5. POST /api/telemetry with invalid JSON or out-of-range ADC value returns 400
        val outOfRangeBody = """
            {
              "deviceId": "krishiva-esp32-01",
              "sequence": 12,
              "uptimeMs": 46000,
              "moistureRaw": 5000
            }
        """.trimIndent()
        val postOutOfRange = sendPost("http://127.0.0.1:$testPort/api/telemetry", outOfRangeBody, testToken)
        assertEquals(400, postOutOfRange.first)

        val malformedBody = "{ not_valid_json "
        val postMalformed = sendPost("http://127.0.0.1:$testPort/api/telemetry", malformedBody, testToken)
        assertEquals(400, postMalformed.first)
        assertEquals("Moisture raw must still remain 2450 after 400", 2450, esp32Service.latestTelemetry.value?.moistureRaw)
    }

    @Test
    fun test6_connectionStatusTransitionsToDisconnectedAfter10SecondsStale() = runBlocking {
        esp32Service.startReceiver(portToBind = testPort)
        val validBody = """
            {
              "deviceId": "krishiva-esp32-01",
              "sequence": 1,
              "uptimeMs": 2000,
              "moistureRaw": 2000
            }
        """.trimIndent()
        val resp = sendPost("http://127.0.0.1:$testPort/api/telemetry", validBody, testToken)
        assertEquals(200, resp.first)
        val receiptTime = esp32Service.latestTelemetry.value!!.receivedAtMs

        // Within 5 seconds -> CONNECTED
        esp32Service.refreshConnectionFreshness(nowMs = receiptTime + 5_000L)
        assertEquals(Esp32TelemetryStatus.CONNECTED, esp32Service.esp32Status.value)

        // After 10.5 seconds without packets -> DISCONNECTED
        esp32Service.refreshConnectionFreshness(nowMs = receiptTime + 10_500L)
        assertEquals(Esp32TelemetryStatus.DISCONNECTED, esp32Service.esp32Status.value)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun test7_liveScanSucceedsOnFreshPacketAndFailsOnTimeout() = runBlocking {
        esp32Service.startReceiver(portToBind = testPort)

        // Seed an older packet before scan starts
        sendPost(
            "http://127.0.0.1:$testPort/api/telemetry",
            """{"deviceId":"krishiva-esp32-01","sequence":1,"uptimeMs":1000,"moistureRaw":3000}""",
            testToken
        )

        // Start a live scan and post a fresh packet 250ms later
        val scanDeferred = async(Dispatchers.IO) {
            esp32Service.executeLiveScan(timeoutMs = 2000L)
        }
        delay(250)
        sendPost(
            "http://127.0.0.1:$testPort/api/telemetry",
            """{"deviceId":"krishiva-esp32-01","sequence":2,"uptimeMs":3000,"moistureRaw":1950}""",
            testToken
        )

        val scanResult = scanDeferred.await()
        assertTrue("Live scan must return LiveScanResult.Success", scanResult is LiveScanResult.Success)
        val success = scanResult as LiveScanResult.Success
        assertEquals(1950, success.reading.moistureRaw)
        assertEquals(ScanStage.COMPLETE, esp32Service.currentScanStage.value)

        // Now test timeout when no new packet arrives after scan starts
        val timeoutResult = esp32Service.executeLiveScan(timeoutMs = 400L)
        assertTrue("Live scan must return LiveScanResult.Timeout on timeout", timeoutResult is LiveScanResult.Timeout)
        assertEquals(ScanStage.TIMEOUT_ERROR, esp32Service.currentScanStage.value)
    }

    @Test
    fun test8_calibrationMathSupportsNormalAndInvertedDirectionsAndRejectsEqualReferences() {
        // Uncalibrated -> null
        val uncalibrated = CalibrationConfig(dryRaw = null, wetRaw = null)
        assertFalse(uncalibrated.isConfigured)
        assertNull(uncalibrated.computeRelativeMoisturePercent(2400))

        // Equal dry and wet -> invalid / null (prevents division by zero)
        val equalRefs = CalibrationConfig(dryRaw = 2000, wetRaw = 2000)
        assertFalse(equalRefs.isConfigured)
        assertNull(equalRefs.computeRelativeMoisturePercent(2000))

        // Typical resistive module where dry soil has HIGH ADC (3200) and wet soil has LOW ADC (1200)
        val invertedSensor = CalibrationConfig(dryRaw = 3200, wetRaw = 1200)
        assertTrue(invertedSensor.isConfigured)
        assertEquals(0.0, invertedSensor.computeRelativeMoisturePercent(3200)!!, 0.01)
        assertEquals(50.0, invertedSensor.computeRelativeMoisturePercent(2200)!!, 0.01)
        assertEquals(100.0, invertedSensor.computeRelativeMoisturePercent(1200)!!, 0.01)
        // Clamping outside bounds
        assertEquals(0.0, invertedSensor.computeRelativeMoisturePercent(3800)!!, 0.01)
        assertEquals(100.0, invertedSensor.computeRelativeMoisturePercent(900)!!, 0.01)

        // Normal sensor direction where dry soil has LOW ADC (1000) and wet soil has HIGH ADC (3000)
        val normalSensor = CalibrationConfig(dryRaw = 1000, wetRaw = 3000)
        assertTrue(normalSensor.isConfigured)
        assertEquals(0.0, normalSensor.computeRelativeMoisturePercent(1000)!!, 0.01)
        assertEquals(50.0, normalSensor.computeRelativeMoisturePercent(2000)!!, 0.01)
        assertEquals(100.0, normalSensor.computeRelativeMoisturePercent(3000)!!, 0.01)
    }

    @Test
    fun test9_nonMoistureHardwareFieldsRemainNullUnlessManuallyEntered() = runBlocking {
        esp32Service.startReceiver(portToBind = testPort)
        sendPost(
            "http://127.0.0.1:$testPort/api/telemetry",
            """{"deviceId":"krishiva-esp32-01","sequence":5,"uptimeMs":10000,"moistureRaw":2100}""",
            testToken
        )
        val live = esp32Service.latestTelemetry.value!!
        repository.onLivePacketReceived(live, isDuplicateSequence = false)

        val saved = repository.latestSoilReading.first()
        assertNotNull(saved)
        assertEquals(2100, saved?.moistureRaw)
        assertNull("pH must be null for ESP32 moisture-only telemetry", saved?.ph)
        assertNull("temperature must be null for ESP32 moisture-only telemetry", saved?.temperatureCelsius)
        assertNull("nitrogen must be null for ESP32 moisture-only telemetry", saved?.nitrogen)
        assertNull("phosphorus must be null for ESP32 moisture-only telemetry", saved?.phosphorus)
        assertNull("potassium must be null for ESP32 moisture-only telemetry", saved?.potassium)
        assertNull("sulfur must be null for ESP32 moisture-only telemetry", saved?.sulfur)
        assertNull("ecConductivity must be null for ESP32 moisture-only telemetry", saved?.ecConductivity)

        val suitability = KrishivaDecisionEngine.evaluateSuitability(saved)
        assertTrue("Uncalibrated reading must produce null suitability scores", suitability.all { it.suitabilityScore == null })
    }

    @Test
    fun test10_roomMigration1To2ConvertsDemoToLegacySampleAndExcludesFromLiveState() = runBlocking {
        // Insert a legacy sample row and verify DAO excludes LEGACY_SAMPLE from live queries
        val legacyRow = com.example.domain.models.SoilReading(
            timestamp = 1700000000000L,
            moistureRaw = null,
            moisturePercent = 42.0,
            calibrated = false,
            source = com.example.domain.models.DataSource.LEGACY_SAMPLE,
            note = "Legacy demo seed"
        )
        database.soilReadingDao().insertReading(legacyRow)
        val liveReadings = repository.allSoilReadings.first()
        assertTrue("LEGACY_SAMPLE rows must not be returned in live soil readings", liveReadings.isEmpty())
    }

    private fun sendPost(urlStr: String, jsonBody: String, token: String?): Pair<Int, String> {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            if (token != null) {
                setRequestProperty("X-Device-Token", token)
            }
        }
        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(jsonBody) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val body = stream?.bufferedReader()?.readText() ?: ""
        return code to body
    }
}
