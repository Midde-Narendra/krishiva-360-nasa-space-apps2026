package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.repository.KrishivaRepository
import com.example.domain.engine.KrishivaDecisionEngine
import com.example.domain.models.CalibrationConfig
import com.example.domain.models.CropRecommendation
import com.example.domain.models.FarmDecision
import com.example.domain.models.NutrientRating
import com.example.domain.models.SoilReading
import com.example.domain.models.WaterGuidance
import com.example.hardware.ESP32ConnectionService
import com.example.hardware.LiveScanResult
import com.example.localization.AppLanguage
import com.example.localization.Strings
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KrishivaViewModel(application: Application) : AndroidViewModel(application) {

    val database = AppDatabase.getDatabase(application)
    val repository = KrishivaRepository(database, application)

    val hardwareService = ESP32ConnectionService(
        initialToken = repository.deviceToken.value,
        initialCalibration = repository.calibration.value,
        initialPort = ESP32ConnectionService.DEFAULT_PORT,
        onTelemetryAccepted = { telemetry, isDuplicate ->
            viewModelScope.launch {
                repository.onLivePacketReceived(telemetry, isDuplicate)
            }
        }
    )

    private val _language = MutableStateFlow(AppLanguage.ENGLISH)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _selectedTab = MutableStateFlow("home")
    val selectedTab: StateFlow<String> = _selectedTab.asStateFlow()

    private val _currentRoute = MutableStateFlow("splash")
    val currentRoute: StateFlow<String> = _currentRoute.asStateFlow()

    private val _selectedCropForDetails = MutableStateFlow<CropRecommendation?>(null)
    val selectedCropForDetails: StateFlow<CropRecommendation?> = _selectedCropForDetails.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    // Persistent flows
    val latestReading: StateFlow<SoilReading?> = repository.latestSoilReading
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allReadings: StateFlow<List<SoilReading>> = repository.allSoilReadings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAlerts = repository.allAlerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadAlertsCount = repository.unreadAlertsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allDecisions: StateFlow<List<FarmDecision>> = repository.allDecisions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val farmer = repository.farmer
    val farm = repository.farm
    val plots = repository.plots
    val pestStatus = repository.pestStatus
    val nutrientGuidance = repository.nutrientGuidance
    val deviceToken = repository.deviceToken
    val calibration = repository.calibration

    // Hardware flows
    val receiverStatus = hardwareService.receiverStatus
    val esp32Status = hardwareService.esp32Status
    val latestTelemetry = hardwareService.latestTelemetry
    val sensorDevice = hardwareService.deviceInfo
    val scanStage = hardwareService.currentScanStage
    val isScanning = hardwareService.isScanning

    // Derived WaterGuidance combining live telemetry, latest stored reading, and stored history
    val waterGuidance: StateFlow<WaterGuidance> = combine(
        latestTelemetry,
        latestReading,
        allReadings
    ) { live, stored, history ->
        repository.buildWaterGuidance(
            liveTelemetry = live,
            latestStored = stored,
            history = history
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WaterGuidance())

    init {
        hardwareService.updateManualIpOverride(repository.manualIpOverride.value)
        if (repository.isReceiverEnabledPreference()) {
            hardwareService.startReceiver()
        }
    }

    override fun onCleared() {
        super.onCleared()
        hardwareService.stopReceiver()
    }

    fun setLanguage(lang: AppLanguage) {
        _language.value = lang
    }

    fun navigateTo(route: String) {
        _currentRoute.value = route
        when (route) {
            "home" -> _selectedTab.value = "home"
            "scan" -> _selectedTab.value = "scan"
            "insights", "soil_analysis", "crops", "water_guidance", "nutrient_guidance", "pest_monitoring" -> _selectedTab.value = "insights"
            "demand", "government" -> _selectedTab.value = "demand"
            "profile", "history", "plots", "device", "alerts" -> _selectedTab.value = "profile"
        }
    }

    fun selectTab(tab: String) {
        _selectedTab.value = tab
        _currentRoute.value = tab
    }

    fun selectCropForExplainability(crop: CropRecommendation?) {
        _selectedCropForDetails.value = crop
    }

    fun toggleReceiverEnabled(enabled: Boolean) {
        repository.setReceiverEnabledPreference(enabled)
        if (enabled) {
            val ok = hardwareService.startReceiver()
            viewModelScope.launch {
                if (ok) {
                    _userMessage.emit("Phone HTTP receiver running on port ${hardwareService.port.value}. Waiting for ESP32 telemetry.")
                } else {
                    _userMessage.emit(sensorDevice.value.receiverError ?: "Could not start HTTP receiver on port 8080.")
                }
            }
        } else {
            hardwareService.stopReceiver()
            viewModelScope.launch {
                _userMessage.emit("Phone HTTP receiver stopped.")
            }
        }
    }

    fun restartReceiver() {
        val ok = hardwareService.reconnect()
        viewModelScope.launch {
            if (ok) {
                _userMessage.emit("Phone HTTP receiver restarted on port ${hardwareService.port.value}.")
            } else {
                _userMessage.emit(sensorDevice.value.receiverError ?: "Failed to restart receiver.")
            }
        }
    }

    fun updateManualPhoneIp(ip: String) {
        repository.saveManualIpOverride(ip)
        hardwareService.updateManualIpOverride(ip)
        viewModelScope.launch {
            _userMessage.emit(
                if (ip.isBlank()) "Using auto-detected phone IPv4 address."
                else "Updated phone IPv4 display to ${ip.trim()}."
            )
        }
    }

    fun regenerateToken() {
        val newToken = repository.regenerateDeviceToken()
        hardwareService.updateDeviceToken(newToken)
        viewModelScope.launch {
            _userMessage.emit("Generated new X-Device-Token. Update your ESP32 sketch to match.")
        }
    }

    fun saveDryCalibrationFromCurrentReading(rawOverride: Int? = null) {
        val raw = rawOverride ?: latestTelemetry.value?.moistureRaw ?: latestReading.value?.moistureRaw
        if (raw == null) {
            viewModelScope.launch {
                _userMessage.emit("No live raw ADC reading available to save as Dry reference.")
            }
            return
        }
        val updated = repository.saveDryReference(raw)
        hardwareService.updateCalibration(updated)
        viewModelScope.launch {
            _userMessage.emit("Saved Dry soil reference: Raw ADC $raw.")
        }
    }

    fun saveWetCalibrationFromCurrentReading(rawOverride: Int? = null) {
        val raw = rawOverride ?: latestTelemetry.value?.moistureRaw ?: latestReading.value?.moistureRaw
        if (raw == null) {
            viewModelScope.launch {
                _userMessage.emit("No live raw ADC reading available to save as Wet reference.")
            }
            return
        }
        val updated = repository.saveWetReference(raw)
        hardwareService.updateCalibration(updated)
        viewModelScope.launch {
            _userMessage.emit("Saved Wet soil reference: Raw ADC $raw.")
        }
    }

    fun clearCalibration() {
        val cleared = repository.clearCalibration()
        hardwareService.updateCalibration(cleared)
        viewModelScope.launch {
            _userMessage.emit("Cleared soil moisture calibration references.")
        }
    }

    fun startSoilScan(onCompleted: () -> Unit = {}) {
        if (isScanning.value) return
        viewModelScope.launch {
            when (val result = hardwareService.executeLiveScan()) {
                is LiveScanResult.Success -> {
                    repository.recordSoilReading(result.reading)
                    val pctText = result.reading.moisturePercent?.let { "${String.format("%.1f", it)}%" } ?: "Uncalibrated"
                    _userMessage.emit("Live ESP32 scan recorded: Raw ADC ${result.reading.moistureRaw} ($pctText).")
                    onCompleted()
                }
                is LiveScanResult.Timeout -> {
                    _userMessage.emit(Strings.scanTimeoutError(_language.value))
                }
                is LiveScanResult.Error -> {
                    _userMessage.emit(result.message)
                }
            }
        }
    }

    fun saveDecision(crop: CropRecommendation) {
        viewModelScope.launch {
            val activeReading = currentEffectiveReading()
            val moistureText = when {
                activeReading == null -> "No soil moisture scan recorded"
                activeReading.calibrated && activeReading.moisturePercent != null ->
                    "Calibrated relative moisture: ${String.format("%.1f", activeReading.moisturePercent)}% (Raw ADC: ${activeReading.moistureRaw ?: "N/A"})"
                activeReading.moistureRaw != null ->
                    "Raw ADC: ${activeReading.moistureRaw} (Calibration required)"
                else -> "No moisture reading"
            }

            val decision = FarmDecision(
                farmName = farm.value.name.ifBlank { "Unnamed Farm" },
                recommendedCrop = crop.cropName,
                suitabilityPercent = crop.suitabilityScore,
                soilSummary = "$moistureText • pH/Temp/EC: Not connected",
                waterSummary = waterGuidance.value.recommendedAction,
                nutrientSummary = nutrientGuidance.value.guidanceText,
                pestSummary = "No pest sensor connected",
                demandSummary = "Static reference only (live market data not configured)",
                notes = "Provisional decision based on ${crop.measuredFactorsCount} of ${crop.totalFactorsCount} soil factors."
            )
            repository.saveFarmDecision(decision)
            _userMessage.emit("Saved provisional farm decision for ${crop.cropName}.")
        }
    }

    fun saveManualSoilHealthCard(
        ph: Double?,
        nitrogen: NutrientRating?,
        phosphorus: NutrientRating?,
        potassium: NutrientRating?,
        sulfur: NutrientRating?,
        ec: Double?
    ) {
        viewModelScope.launch {
            repository.saveManualSoilHealthCard(
                ph = ph,
                nitrogen = nitrogen,
                phosphorus = phosphorus,
                potassium = potassium,
                sulfur = sulfur,
                ec = ec,
                latestMoistureReading = currentEffectiveReading()
            )
            _userMessage.emit("Saved manual Soil Health Card entries.")
        }
    }

    fun addPlot(name: String, areaAcres: Double, soilType: String) {
        repository.addPlot(name, areaAcres, soilType)
        viewModelScope.launch {
            _userMessage.emit("Added plot '$name'.")
        }
    }

    fun deletePlot(plotId: String) {
        repository.deletePlot(plotId)
        viewModelScope.launch {
            _userMessage.emit("Removed plot.")
        }
    }

    fun markAlertRead(id: Long) {
        viewModelScope.launch { repository.markAlertAsRead(id) }
    }

    fun markAllAlertsRead() {
        viewModelScope.launch { repository.markAllAlertsAsRead() }
    }

    fun clearAllAlerts() {
        viewModelScope.launch { repository.clearAllAlerts() }
    }

    fun clearRecordedHistory() {
        viewModelScope.launch {
            repository.clearRecordedHistory()
            _userMessage.emit("Cleared recorded soil, alert, and decision history.")
        }
    }

    fun updateFarmerInfo(name: String, phone: String, village: String, district: String, state: String) {
        repository.updateFarmerProfile(name, phone, village, district, state)
        viewModelScope.launch { _userMessage.emit("Farmer profile saved.") }
    }

    fun updateFarmInfo(name: String, location: String, acres: Double?, crop: String) {
        repository.updateFarmDetails(name, location, acres, crop)
        viewModelScope.launch { _userMessage.emit("Farm details saved.") }
    }

    fun currentEffectiveReading(): SoilReading? {
        val live = latestTelemetry.value
        val stored = latestReading.value
        return if (live != null && (stored == null || live.receivedAtMs >= stored.timestamp)) {
            SoilReading(
                id = stored?.id ?: 0L,
                timestamp = live.receivedAtMs,
                moistureRaw = live.moistureRaw,
                moisturePercent = live.calibratedMoisturePercent,
                calibrated = live.calibratedMoisturePercent != null,
                deviceId = live.deviceId,
                sequenceNumber = live.sequence,
                wifiRssiDbm = live.wifiRssiDbm,
                firmwareVersion = live.firmwareVersion,
                ph = stored?.ph,
                temperatureCelsius = null,
                nitrogen = stored?.nitrogen,
                phosphorus = stored?.phosphorus,
                potassium = stored?.potassium,
                sulfur = stored?.sulfur,
                ecConductivity = stored?.ecConductivity,
                isValid = true,
                source = com.example.domain.models.DataSource.ESP32_HARDWARE,
                note = "Live ESP32 telemetry"
            )
        } else {
            stored
        }
    }

    fun getCropSuitability(soil: SoilReading?): List<CropRecommendation> {
        return KrishivaDecisionEngine.evaluateSuitability(soil)
    }

    fun getRegionalDemands() = KrishivaDecisionEngine.getRegionalDemandData()
    fun getGovernmentAdvisories() = repository.getGovernmentAdvisories()
}
