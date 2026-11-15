package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.localization.AppLanguage
import com.example.localization.Strings
import com.example.ui.screens.AlertCenterScreen
import com.example.ui.screens.CropIntelligenceScreen
import com.example.ui.screens.DemandIntelligenceScreen
import com.example.ui.screens.FarmComparisonScreen
import com.example.ui.screens.FarmSetupScreen
import com.example.ui.screens.FinalDecisionScreen
import com.example.ui.screens.GovernmentAdvisoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NutrientGuidanceScreen
import com.example.ui.screens.PestMonitoringScreen
import com.example.ui.screens.ProfileSettingsScreen
import com.example.ui.screens.ScanScreen
import com.example.ui.screens.SensorDeviceScreen
import com.example.ui.screens.SoilAnalysisScreen
import com.example.ui.screens.SoilHistoryScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.WaterGuidanceScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.viewmodel.KrishivaViewModel

enum class KrishivaScreen {
    SPLASH,
    WELCOME,
    FARM_SETUP,
    HOME,
    SCAN,
    SOIL_ANALYSIS,
    CROP_INTELLIGENCE,
    WATER_GUIDANCE,
    NUTRIENT_GUIDANCE,
    PEST_MONITORING,
    DEMAND_INTELLIGENCE,
    GOVERNMENT_ADVISORY,
    ALERT_CENTER,
    SENSOR_DEVICE,
    SOIL_HISTORY,
    FARM_COMPARISON,
    FINAL_DECISION,
    PROFILE_SETTINGS
}

data class BottomNavTab(
    val screen: KrishivaScreen,
    val labelProvider: (AppLanguage) -> String,
    val icon: ImageVector,
    val tag: String
)

@Composable
fun KrishivaAppShell(viewModel: KrishivaViewModel) {
    var currentScreen by rememberSaveable { mutableStateOf(KrishivaScreen.SPLASH) }

    val language by viewModel.language.collectAsStateWithLifecycle()
    val farmer by viewModel.farmer.collectAsStateWithLifecycle()
    val farm by viewModel.farm.collectAsStateWithLifecycle()
    val plots by viewModel.plots.collectAsStateWithLifecycle()
    val latestSoil by viewModel.latestReading.collectAsStateWithLifecycle()
    val allSoilReadings by viewModel.allReadings.collectAsStateWithLifecycle()
    val liveTelemetry by viewModel.latestTelemetry.collectAsStateWithLifecycle()
    val calibration by viewModel.calibration.collectAsStateWithLifecycle()
    val receiverStatus by viewModel.receiverStatus.collectAsStateWithLifecycle()
    val esp32Status by viewModel.esp32Status.collectAsStateWithLifecycle()
    val deviceToken by viewModel.deviceToken.collectAsStateWithLifecycle()
    val selectedCropForPlan by viewModel.selectedCropForDetails.collectAsStateWithLifecycle()
    val waterGuidance by viewModel.waterGuidance.collectAsStateWithLifecycle()
    val nutrientGuidance by viewModel.nutrientGuidance.collectAsStateWithLifecycle()
    val pestStatus by viewModel.pestStatus.collectAsStateWithLifecycle()
    val alerts by viewModel.allAlerts.collectAsStateWithLifecycle()
    val unreadAlertCount by viewModel.unreadAlertsCount.collectAsStateWithLifecycle()
    val decisions by viewModel.allDecisions.collectAsStateWithLifecycle()
    val sensorDevice by viewModel.sensorDevice.collectAsStateWithLifecycle()
    val scanStage by viewModel.scanStage.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()

    val effectiveSoil = viewModel.currentEffectiveReading()
    val recommendations = viewModel.getCropSuitability(effectiveSoil)
    val marketDemands = viewModel.getRegionalDemands()
    val govAdvisories = viewModel.getGovernmentAdvisories()

    val showBottomBar = currentScreen !in listOf(
        KrishivaScreen.SPLASH,
        KrishivaScreen.WELCOME,
        KrishivaScreen.FARM_SETUP
    )

    if (currentScreen != KrishivaScreen.HOME &&
        currentScreen != KrishivaScreen.SPLASH &&
        currentScreen != KrishivaScreen.WELCOME
    ) {
        BackHandler {
            currentScreen = KrishivaScreen.HOME
        }
    }

    val tabs = listOf(
        BottomNavTab(KrishivaScreen.HOME, Strings::home, Icons.Default.Home, "nav_tab_home"),
        BottomNavTab(KrishivaScreen.SCAN, Strings::scan, Icons.Default.Radar, "nav_tab_scan"),
        BottomNavTab(KrishivaScreen.CROP_INTELLIGENCE, Strings::insights, Icons.Default.Analytics, "nav_tab_insights"),
        BottomNavTab(KrishivaScreen.DEMAND_INTELLIGENCE, Strings::demand, Icons.Default.TrendingUp, "nav_tab_demand"),
        BottomNavTab(KrishivaScreen.PROFILE_SETTINGS, Strings::profile, Icons.Default.Person, "nav_tab_profile")
    )

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    tabs.forEach { tab ->
                        val isSelected = when (tab.screen) {
                            KrishivaScreen.HOME -> currentScreen in listOf(
                                KrishivaScreen.HOME,
                                KrishivaScreen.ALERT_CENTER,
                                KrishivaScreen.SOIL_HISTORY,
                                KrishivaScreen.FARM_COMPARISON
                            )
                            KrishivaScreen.SCAN -> currentScreen in listOf(
                                KrishivaScreen.SCAN,
                                KrishivaScreen.SOIL_ANALYSIS,
                                KrishivaScreen.SENSOR_DEVICE
                            )
                            KrishivaScreen.CROP_INTELLIGENCE -> currentScreen in listOf(
                                KrishivaScreen.CROP_INTELLIGENCE,
                                KrishivaScreen.WATER_GUIDANCE,
                                KrishivaScreen.NUTRIENT_GUIDANCE,
                                KrishivaScreen.PEST_MONITORING,
                                KrishivaScreen.FINAL_DECISION
                            )
                            KrishivaScreen.DEMAND_INTELLIGENCE -> currentScreen in listOf(
                                KrishivaScreen.DEMAND_INTELLIGENCE,
                                KrishivaScreen.GOVERNMENT_ADVISORY
                            )
                            KrishivaScreen.PROFILE_SETTINGS -> currentScreen == KrishivaScreen.PROFILE_SETTINGS
                            else -> false
                        }

                        val tabLabel = tab.labelProvider(language)
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentScreen = tab.screen },
                            modifier = Modifier.testTag(tab.tag),
                            icon = {
                                if (tab.screen == KrishivaScreen.HOME && unreadAlertCount > 0) {
                                    BadgedBox(
                                        badge = { Badge { Text(unreadAlertCount.toString()) } }
                                    ) {
                                        Icon(tab.icon, contentDescription = tabLabel)
                                    }
                                } else {
                                    Icon(tab.icon, contentDescription = tabLabel)
                                }
                            },
                            label = {
                                Text(
                                    text = tabLabel,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
                    when (screen) {
                        KrishivaScreen.SPLASH -> SplashScreen(
                            onFinished = { currentScreen = KrishivaScreen.WELCOME }
                        )
                        KrishivaScreen.WELCOME -> WelcomeScreen(
                            onGetStarted = { currentScreen = KrishivaScreen.FARM_SETUP },
                            onOpenDashboard = { currentScreen = KrishivaScreen.HOME }
                        )
                        KrishivaScreen.FARM_SETUP -> FarmSetupScreen(
                            currentFarmer = farmer,
                            currentFarm = farm,
                            onSaveFarm = { name, phone, village, district, state, farmName, areaAcres, mainCrop ->
                                viewModel.updateFarmerInfo(name, phone, village, district, state)
                                val loc = listOf(village, district, state).filter { it.isNotBlank() }.joinToString(", ")
                                viewModel.updateFarmInfo(farmName, loc, areaAcres, mainCrop)
                                currentScreen = KrishivaScreen.HOME
                            },
                            onSkipToDashboard = {
                                currentScreen = KrishivaScreen.HOME
                            }
                        )
                        KrishivaScreen.HOME -> HomeScreen(
                            language = language,
                            farmer = farmer,
                            farm = farm,
                            latestSoil = effectiveSoil,
                            liveTelemetry = liveTelemetry,
                            calibration = calibration,
                            receiverStatus = receiverStatus,
                            esp32Status = esp32Status,
                            sensorDevice = sensorDevice,
                            recommendations = recommendations,
                            waterGuidance = waterGuidance,
                            nutrientGuidance = nutrientGuidance,
                            unreadAlertsCount = unreadAlertCount,
                            onStartScan = { currentScreen = KrishivaScreen.SCAN },
                            onOpenSoilResults = { currentScreen = KrishivaScreen.SOIL_ANALYSIS },
                            onOpenCrops = { currentScreen = KrishivaScreen.CROP_INTELLIGENCE },
                            onOpenWater = { currentScreen = KrishivaScreen.WATER_GUIDANCE },
                            onOpenNutrients = { currentScreen = KrishivaScreen.NUTRIENT_GUIDANCE },
                            onOpenPest = { currentScreen = KrishivaScreen.PEST_MONITORING },
                            onOpenDemand = { currentScreen = KrishivaScreen.DEMAND_INTELLIGENCE },
                            onOpenGovernment = { currentScreen = KrishivaScreen.GOVERNMENT_ADVISORY },
                            onOpenAlerts = { currentScreen = KrishivaScreen.ALERT_CENTER },
                            onOpenDevice = { currentScreen = KrishivaScreen.SENSOR_DEVICE },
                            onOpenHistory = { currentScreen = KrishivaScreen.SOIL_HISTORY },
                            onOpenComparison = { currentScreen = KrishivaScreen.FARM_COMPARISON },
                            onOpenDecision = { currentScreen = KrishivaScreen.FINAL_DECISION }
                        )
                        KrishivaScreen.SCAN -> ScanScreen(
                            receiverStatus = receiverStatus,
                            esp32Status = esp32Status,
                            sensorDevice = sensorDevice,
                            scanStage = scanStage,
                            isScanning = isScanning,
                            latestSoil = effectiveSoil,
                            liveTelemetry = liveTelemetry,
                            calibration = calibration,
                            onToggleLiveConnection = { viewModel.toggleReceiverEnabled(it) },
                            onTriggerScan = {
                                viewModel.startSoilScan {
                                    currentScreen = KrishivaScreen.SOIL_ANALYSIS
                                }
                            },
                            onViewResults = { currentScreen = KrishivaScreen.SOIL_ANALYSIS },
                            onOpenDeviceSettings = { currentScreen = KrishivaScreen.SENSOR_DEVICE }
                        )
                        KrishivaScreen.SOIL_ANALYSIS -> SoilAnalysisScreen(
                            soil = effectiveSoil,
                            liveTelemetry = liveTelemetry,
                            calibration = calibration,
                            onBack = { currentScreen = KrishivaScreen.HOME },
                            onOpenCrops = { currentScreen = KrishivaScreen.CROP_INTELLIGENCE },
                            onOpenWater = { currentScreen = KrishivaScreen.WATER_GUIDANCE },
                            onOpenNutrients = { currentScreen = KrishivaScreen.NUTRIENT_GUIDANCE },
                            onOpenCalibration = { currentScreen = KrishivaScreen.SENSOR_DEVICE },
                            onSaveManualLabValues = { ph, n, p, k, s, ec ->
                                viewModel.saveManualSoilHealthCard(ph, n, p, k, s, ec)
                            }
                        )
                        KrishivaScreen.CROP_INTELLIGENCE -> CropIntelligenceScreen(
                            language = language,
                            soil = effectiveSoil,
                            calibration = calibration,
                            recommendations = recommendations,
                            selectedCropForPlan = selectedCropForPlan ?: recommendations.firstOrNull(),
                            onSelectCropForPlan = { viewModel.selectCropForExplainability(it) },
                            onProceedToDecision = { currentScreen = KrishivaScreen.FINAL_DECISION },
                            onOpenWater = { currentScreen = KrishivaScreen.WATER_GUIDANCE },
                            onOpenNutrients = { currentScreen = KrishivaScreen.NUTRIENT_GUIDANCE }
                        )
                        KrishivaScreen.WATER_GUIDANCE -> WaterGuidanceScreen(
                            calibration = calibration,
                            guidance = waterGuidance,
                            onOpenCalibration = { currentScreen = KrishivaScreen.SENSOR_DEVICE },
                            onBack = { currentScreen = KrishivaScreen.HOME }
                        )
                        KrishivaScreen.NUTRIENT_GUIDANCE -> NutrientGuidanceScreen(
                            soil = effectiveSoil,
                            guidance = nutrientGuidance,
                            onOpenSoilLabEntry = { currentScreen = KrishivaScreen.SOIL_ANALYSIS },
                            onBack = { currentScreen = KrishivaScreen.HOME }
                        )
                        KrishivaScreen.PEST_MONITORING -> PestMonitoringScreen(
                            pestStatus = pestStatus,
                            onBack = { currentScreen = KrishivaScreen.HOME }
                        )
                        KrishivaScreen.DEMAND_INTELLIGENCE -> DemandIntelligenceScreen(
                            demands = marketDemands,
                            onOpenGovernmentAdvisory = { currentScreen = KrishivaScreen.GOVERNMENT_ADVISORY },
                            onOpenFinalDecision = { currentScreen = KrishivaScreen.FINAL_DECISION }
                        )
                        KrishivaScreen.GOVERNMENT_ADVISORY -> GovernmentAdvisoryScreen(
                            advisories = govAdvisories,
                            district = farmer.district,
                            onBack = { currentScreen = KrishivaScreen.DEMAND_INTELLIGENCE }
                        )
                        KrishivaScreen.ALERT_CENTER -> AlertCenterScreen(
                            alerts = alerts,
                            onMarkRead = { viewModel.markAlertRead(it) },
                            onMarkAllRead = { viewModel.markAllAlertsRead() },
                            onBack = { currentScreen = KrishivaScreen.HOME }
                        )
                        KrishivaScreen.SENSOR_DEVICE -> SensorDeviceScreen(
                            device = sensorDevice,
                            receiverStatus = receiverStatus,
                            esp32Status = esp32Status,
                            deviceToken = deviceToken,
                            calibration = calibration,
                            liveTelemetry = liveTelemetry,
                            onToggleLiveConnection = { viewModel.toggleReceiverEnabled(it) },
                            onUpdateManualIpOverride = { viewModel.updateManualPhoneIp(it) },
                            onRegenerateToken = { viewModel.regenerateToken() },
                            onRestartReceiver = { viewModel.restartReceiver() },
                            onSaveDryCalibration = { viewModel.saveDryCalibrationFromCurrentReading(it) },
                            onSaveWetCalibration = { viewModel.saveWetCalibrationFromCurrentReading(it) },
                            onClearCalibration = { viewModel.clearCalibration() },
                            onBack = { currentScreen = KrishivaScreen.HOME }
                        )
                        KrishivaScreen.SOIL_HISTORY -> SoilHistoryScreen(
                            readings = allSoilReadings,
                            onBack = { currentScreen = KrishivaScreen.HOME }
                        )
                        KrishivaScreen.FARM_COMPARISON -> FarmComparisonScreen(
                            plots = plots,
                            onAddPlot = {
                                val nextIndex = plots.size + 1
                                viewModel.addPlot(
                                    name = "Field Plot $nextIndex",
                                    areaAcres = 1.0,
                                    soilType = "Red Loam"
                                )
                            },
                            onDeletePlot = { viewModel.deletePlot(it) },
                            onBack = { currentScreen = KrishivaScreen.HOME }
                        )
                        KrishivaScreen.FINAL_DECISION -> FinalDecisionScreen(
                            farm = farm,
                            soil = effectiveSoil,
                            selectedCrop = selectedCropForPlan ?: recommendations.firstOrNull(),
                            waterGuidance = waterGuidance,
                            nutrientGuidance = nutrientGuidance,
                            savedDecisions = decisions,
                            onSaveDecision = { viewModel.saveDecision(it) },
                            onBackToDashboard = { currentScreen = KrishivaScreen.HOME }
                        )
                        KrishivaScreen.PROFILE_SETTINGS -> ProfileSettingsScreen(
                            farmer = farmer,
                            farm = farm,
                            device = sensorDevice,
                            receiverStatus = receiverStatus,
                            esp32Status = esp32Status,
                            calibration = calibration,
                            language = language,
                            onLanguageChange = { viewModel.setLanguage(it) },
                            onToggleLiveConnection = { viewModel.toggleReceiverEnabled(it) },
                            onOpenDeviceScreen = { currentScreen = KrishivaScreen.SENSOR_DEVICE },
                            onOpenHistoryScreen = { currentScreen = KrishivaScreen.SOIL_HISTORY },
                            onOpenComparisonScreen = { currentScreen = KrishivaScreen.FARM_COMPARISON },
                            onEditFarmProfile = { currentScreen = KrishivaScreen.FARM_SETUP },
                            onAddPlot = {
                                val nextIndex = plots.size + 1
                                viewModel.addPlot(
                                    name = "Field Plot $nextIndex",
                                    areaAcres = 1.0,
                                    soilType = "Red Loam"
                                )
                            },
                            onClearHistory = { viewModel.clearRecordedHistory() }
                        )
                    }
                }
            }
        }
    }
}
