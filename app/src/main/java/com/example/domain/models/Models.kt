package com.example.domain.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.math.roundToInt

enum class DataSource {
    ESP32_HARDWARE,
    MANUAL,
    LEGACY_SAMPLE
}

enum class ReceiverStatus {
    STOPPED,
    RUNNING,
    ERROR
}

enum class Esp32TelemetryStatus {
    WAITING,
    CONNECTED,
    DISCONNECTED
}

enum class DemandLevel {
    HIGH,
    MEDIUM,
    LOW
}

enum class TrendDirection {
    INCREASING,
    STABLE,
    DECREASING
}

enum class NutrientRating {
    LOW,
    MODERATE,
    GOOD,
    HIGH
}

enum class AlertType {
    SOIL,
    PEST,
    DEMAND,
    CROP,
    GOVERNMENT
}

enum class AlertPriority {
    HIGH,
    MEDIUM,
    INFO
}

data class CalibrationConfig(
    val dryRaw: Int? = null,
    val wetRaw: Int? = null
) {
    val isConfigured: Boolean
        get() {
            val dry = dryRaw ?: return false
            val wet = wetRaw ?: return false
            return dry in 0..4095 && wet in 0..4095 && dry != wet
        }

    /**
     * Computes relative soil-moisture estimate (%) using:
     * 100 * (raw - dryRaw) / (wetRaw - dryRaw), clamped to 0..100.
     * Works whether wet readings produce lower ADC values than dry readings or vice versa.
     * Returns null when either reference is missing or both references are equal.
     */
    fun computeRelativeMoisturePercent(rawAdc: Int): Double? {
        if (!isConfigured || rawAdc !in 0..4095) return null
        val dry = dryRaw ?: return null
        val wet = wetRaw ?: return null
        if (dry == wet) return null
        val pct = 100.0 * (rawAdc - dry).toDouble() / (wet - dry).toDouble()
        return pct.coerceIn(0.0, 100.0)
    }

    fun computeRelativeMoistureInt(rawAdc: Int): Int? {
        return computeRelativeMoisturePercent(rawAdc)?.roundToInt()?.coerceIn(0, 100)
    }
}

data class LiveTelemetry(
    val deviceId: String,
    val sequence: Long,
    val uptimeMs: Long,
    val moistureRaw: Int,
    val calibratedMoisturePercent: Double?,
    val wifiRssiDbm: Int?,
    val firmwareVersion: String?,
    val receivedAtMs: Long
) {
    val isSensorRailWarning: Boolean
        get() = moistureRaw == 0 || moistureRaw >= 4090
}

data class Farmer(
    val name: String = "",
    val phone: String = "",
    val village: String = "",
    val district: String = "",
    val state: String = ""
) {
    val isConfigured: Boolean
        get() = name.isNotBlank()
}

data class Farm(
    val id: String = "farm_local",
    val name: String = "",
    val location: String = "",
    val areaAcres: Double? = null,
    val mainCrop: String = "",
    val activePlots: Int = 0
) {
    val isConfigured: Boolean
        get() = name.isNotBlank()
}

data class Plot(
    val id: String,
    val name: String,
    val areaAcres: Double,
    val soilType: String,
    val moisturePercent: Double? = null,
    val moistureRaw: Int? = null,
    val lastReadingTimestamp: Long? = null
)

@Entity(tableName = "soil_readings")
data class SoilReading(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val moistureRaw: Int? = null,
    val moisturePercent: Double? = null,
    val calibrated: Boolean = false,
    val deviceId: String? = null,
    val sequenceNumber: Long? = null,
    val wifiRssiDbm: Int? = null,
    val firmwareVersion: String? = null,
    val ph: Double? = null,
    val temperatureCelsius: Double? = null,
    val nitrogen: NutrientRating? = null,
    val phosphorus: NutrientRating? = null,
    val potassium: NutrientRating? = null,
    val sulfur: NutrientRating? = null,
    val ecConductivity: Double? = null,
    val isValid: Boolean = true,
    val source: DataSource = DataSource.ESP32_HARDWARE,
    val note: String = ""
) {
    val moistureDisplayInt: Int?
        get() = moisturePercent?.roundToInt()?.coerceIn(0, 100)
}

data class CropRecommendation(
    val id: String,
    val cropName: String,
    val hindiName: String,
    val teluguName: String,
    val suitabilityScore: Int?, // null if no calibrated moisture reading exists
    val isProvisionalMoistureOnly: Boolean = true,
    val measuredFactorsCount: Int = 0,
    val totalFactorsCount: Int = 4,
    val optimalMoistureRangeText: String,
    val waterRequirement: String,
    val moistureCompatibility: Int?,
    val phCompatibility: Int? = null,
    val nutrientCompatibility: Int? = null,
    val shortExplanation: String,
    val decisionReason: String,
    val season: String = "Kharif / Rabi"
)

data class WaterGuidance(
    val currentMoisturePercent: Double? = null,
    val currentMoistureRaw: Int? = null,
    val isCalibrated: Boolean = false,
    val lastScanTimestamp: Long? = null,
    val isStale: Boolean = false,
    val statusText: String = "WAITING FOR SENSOR",
    val selectedCrop: String = "",
    val recommendedAction: String = "Connect ESP32 soil-moisture sensor and complete dry/wet calibration.",
    val conditionSummary: String = "No calibrated soil-moisture reading available yet.",
    val cropRequirement: String = "Select a target crop after recording a calibrated soil-moisture scan.",
    val guidanceReason: String = "Water guidance requires a calibrated relative moisture reading from the field sensor.",
    val recentMoistureHistory: List<Double> = emptyList()
)

data class NutrientGuidance(
    val nitrogen: NutrientRating? = null,
    val phosphorus: NutrientRating? = null,
    val potassium: NutrientRating? = null,
    val sulfur: NutrientRating? = null,
    val ph: Double? = null,
    val ecConductivity: Double? = null,
    val isManuallyEntered: Boolean = false,
    val guidanceText: String = "Not connected: the current ESP32 hardware measures soil moisture only (GPIO34).",
    val organicAdvisory: String = "Enter values from an official Soil Health Card if laboratory testing has been performed.",
    val complianceStandard: String = "N, P, K, S, pH, and EC sensors are not connected to this hardware unit."
)

data class PestEvent(
    val time: String,
    val event: String,
    val severity: String = "INFO"
)

data class PestStatus(
    val sensorConnected: Boolean = false,
    val trapActive: Boolean = false,
    val monitoringOn: Boolean = false,
    val lastDetectionText: String = "Not connected / unavailable",
    val isAlertTriggered: Boolean = false,
    val trapMechanism: String = "No pest sensor hardware connected",
    val events: List<PestEvent> = emptyList()
)

data class DemandInformation(
    val cropName: String,
    val level: DemandLevel,
    val trend: TrendDirection,
    val referenceNote: String,
    val regionalNotes: String,
    val mspStatus: String,
    val isLiveMarketData: Boolean = false
)

data class GovernmentAdvisory(
    val id: String,
    val title: String,
    val issuingBody: String,
    val dateText: String,
    val summary: String,
    val badge: String,
    val actionLabel: String = "View Reference"
)

@Entity(tableName = "smart_alerts")
data class SmartAlert(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val type: AlertType,
    val title: String,
    val message: String,
    val priority: AlertPriority,
    val isRead: Boolean = false
)

data class SensorDevice(
    val deviceName: String = "ESP32 DevKit (Analog Soil Moisture)",
    val deviceId: String? = null,
    val receiverEnabled: Boolean = false,
    val receiverStatus: ReceiverStatus = ReceiverStatus.STOPPED,
    val receiverPort: Int = 8080,
    val receiverError: String? = null,
    val detectedPhoneIp: String? = null,
    val manualPhoneIpOverride: String = "",
    val esp32Status: Esp32TelemetryStatus = Esp32TelemetryStatus.WAITING,
    val wifiRssiDbm: Int? = null,
    val firmwareVersion: String? = null,
    val lastSequence: Long? = null,
    val lastReceivedAtMs: Long? = null,
    val lastMoistureRaw: Int? = null,
    val lastCalibratedPercent: Double? = null,
    val batteryStatus: String = "Not available",
    val solarStatus: String = "Not connected",
    val probeStatus: String = "1x Analog Resistive Soil-Moisture Sensor (GPIO34 / ADC1)"
) {
    val effectivePhoneIp: String
        get() = manualPhoneIpOverride.trim().ifEmpty { detectedPhoneIp ?: "IPv4 not detected" }

    val telemetryEndpointUrl: String
        get() {
            val host = manualPhoneIpOverride.trim().ifEmpty { detectedPhoneIp ?: "<PHONE_IP>" }
            return "http://$host:$receiverPort/api/telemetry"
        }

    val isEsp32Connected: Boolean
        get() = esp32Status == Esp32TelemetryStatus.CONNECTED
}

@Entity(tableName = "farm_decisions")
data class FarmDecision(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val farmName: String,
    val recommendedCrop: String,
    val suitabilityPercent: Int?,
    val soilSummary: String,
    val waterSummary: String,
    val nutrientSummary: String,
    val pestSummary: String,
    val demandSummary: String,
    val notes: String = ""
)
