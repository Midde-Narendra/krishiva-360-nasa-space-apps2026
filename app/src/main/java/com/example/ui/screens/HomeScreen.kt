package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.models.CalibrationConfig
import com.example.domain.models.CropRecommendation
import com.example.domain.models.Esp32TelemetryStatus
import com.example.domain.models.Farm
import com.example.domain.models.Farmer
import com.example.domain.models.LiveTelemetry
import com.example.domain.models.NutrientGuidance
import com.example.domain.models.ReceiverStatus
import com.example.domain.models.SensorDevice
import com.example.domain.models.SoilReading
import com.example.domain.models.WaterGuidance
import com.example.localization.AppLanguage
import com.example.localization.Strings
import com.example.ui.components.StatusPill
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    language: AppLanguage,
    farmer: Farmer,
    farm: Farm,
    latestSoil: SoilReading?,
    liveTelemetry: LiveTelemetry?,
    calibration: CalibrationConfig,
    receiverStatus: ReceiverStatus,
    esp32Status: Esp32TelemetryStatus,
    sensorDevice: SensorDevice,
    recommendations: List<CropRecommendation>,
    waterGuidance: WaterGuidance,
    nutrientGuidance: NutrientGuidance,
    unreadAlertsCount: Int,
    onStartScan: () -> Unit,
    onOpenSoilResults: () -> Unit,
    onOpenCrops: () -> Unit,
    onOpenWater: () -> Unit,
    onOpenNutrients: () -> Unit,
    onOpenPest: () -> Unit,
    onOpenDemand: () -> Unit,
    onOpenGovernment: () -> Unit,
    onOpenAlerts: () -> Unit,
    onOpenDevice: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenComparison: () -> Unit,
    onOpenDecision: () -> Unit
) {
    val activeMoistureRaw = liveTelemetry?.moistureRaw ?: latestSoil?.moistureRaw
    val activeMoisturePct = liveTelemetry?.calibratedMoisturePercent ?: latestSoil?.moisturePercent
    val lastSeenMs = liveTelemetry?.receivedAtMs ?: latestSoil?.timestamp
    val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("home_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = farm.name.ifBlank { Strings.appTitle(language) },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val locationSubtitle = listOf(farmer.name, farmer.village, farmer.district)
                    .filter { it.isNotBlank() }
                    .joinToString(" • ")
                    .ifBlank { "Tap Profile to set up farmer & farm details" }
                Text(
                    text = locationSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.clickable { onOpenDevice() }) {
                    StatusPill(receiverStatus = receiverStatus, esp32Status = esp32Status)
                }
                IconButton(
                    onClick = onOpenAlerts,
                    modifier = Modifier.testTag("home_alerts_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadAlertsCount > 0) {
                                Badge { Text(unreadAlertsCount.toString()) }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Alerts",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Hero Card — Live ESP32 Telemetry & Receiver Status
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("hero_soil_health_card"),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF0F5132),
                                Color(0xFF196F44),
                                Color(0xFF1F7A4C)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                color = Color.White.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = when (esp32Status) {
                                        Esp32TelemetryStatus.CONNECTED -> Strings.espConnectedStatus(language)
                                        Esp32TelemetryStatus.WAITING -> Strings.espWaitingStatus(language)
                                        Esp32TelemetryStatus.DISCONNECTED -> Strings.espDisconnectedStatus(language)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFA7F3D0),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = when {
                                    activeMoisturePct != null -> "${activeMoisturePct.roundToInt()}% Relative Moisture"
                                    activeMoistureRaw != null -> "Raw ADC: $activeMoistureRaw (${Strings.calibrationRequired(language)})"
                                    else -> Strings.waitingForEsp32(language)
                                },
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (lastSeenMs != null) {
                                    "Last packet: ${timeFormatter.format(Date(lastSeenMs))} • Raw ADC: ${activeMoistureRaw ?: "--"}/4095"
                                } else {
                                    "Receiver endpoint: ${sensorDevice.telemetryEndpointUrl}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.88f)
                            )
                        }

                        // Raw ADC Badge
                        Box(
                            modifier = Modifier
                                .size(78.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = activeMoistureRaw?.toString() ?: "--",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "ADC RAW",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFA7F3D0)
                                )
                            }
                        }
                    }

                    // Calibration & Hardware Status Chips (FlowRow prevents awkward text wrapping)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HeroMiniStat(
                            label = "Calibration",
                            value = if (calibration.isConfigured) {
                                "Dry ${calibration.dryRaw} / Wet ${calibration.wetRaw}"
                            } else {
                                Strings.calibrationRequired(language)
                            }
                        )
                        HeroMiniStat(
                            label = "Wi-Fi RSSI",
                            value = liveTelemetry?.wifiRssiDbm?.let { "$it dBm" }
                                ?: latestSoil?.wifiRssiDbm?.let { "$it dBm" }
                                ?: Strings.notAvailable(language)
                        )
                        HeroMiniStat(
                            label = Strings.nutrientStatus(language),
                            value = if (nutrientGuidance.isManuallyEntered) "Manual Lab" else Strings.notConnected(language)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onStartScan,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("home_scan_soil_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF0F5132)
                            )
                        ) {
                            Icon(Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = Strings.scanSoil(language),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = onOpenDevice,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("home_view_device_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Router, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ESP32 Setup", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Real-Time Soil Metrics Grid (Truthful hardware state)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Strings.todaysInsights(language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Only analog soil moisture is connected on GPIO34",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Details →",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onOpenSoilResults() }
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashboardMetricCard(
                    label = Strings.soilMoisture(language),
                    value = when {
                        activeMoisturePct != null -> "${activeMoisturePct.roundToInt()}%"
                        activeMoistureRaw != null -> "$activeMoistureRaw ADC"
                        else -> Strings.waitingForEsp32(language)
                    },
                    statusText = when {
                        activeMoisturePct != null -> waterGuidance.statusText
                        activeMoistureRaw != null -> Strings.calibrationRequired(language)
                        else -> "No ESP32 packet"
                    },
                    icon = Icons.Default.WaterDrop,
                    modifier = Modifier.weight(1f)
                )
                DashboardMetricCard(
                    label = Strings.phLevel(language),
                    value = latestSoil?.ph?.let { String.format(Locale.US, "%.1f", it) } ?: Strings.notConnected(language),
                    statusText = if (latestSoil?.ph != null) "Manual Lab Entry" else "Sensor not connected",
                    icon = Icons.Default.Science,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashboardMetricCard(
                    label = Strings.soilTemp(language),
                    value = latestSoil?.temperatureCelsius?.let { String.format(Locale.US, "%.1f°C", it) } ?: Strings.notConnected(language),
                    statusText = "Sensor not connected",
                    icon = Icons.Default.Eco,
                    modifier = Modifier.weight(1f)
                )
                DashboardMetricCard(
                    label = Strings.pestMonitoring(language),
                    value = Strings.notConnected(language),
                    statusText = "UV Trap not connected",
                    icon = Icons.Default.BugReport,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Top Crop Suitability Strip (Moisture Compatibility)
        val topCrop = recommendations.firstOrNull()
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenCrops() }
                .testTag("home_top_crop_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Eco,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (topCrop?.suitabilityScore != null) {
                            "PROVISIONAL MOISTURE COMPATIBILITY"
                        } else {
                            "CROP COMPATIBILITY REQUIRES CALIBRATED MOISTURE"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = topCrop?.let {
                            if (it.suitabilityScore != null) {
                                "${it.cropName} • ${it.suitabilityScore}% Provisional Match"
                            } else {
                                "${it.cropName} — Calibration or scan required"
                            }
                        } ?: "Scan & calibrate soil moisture first",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "N/P/K, pH, EC and temperature sensors are not connected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open Crop Suitability",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Actionable Intelligence Cards (Water + Nutrients)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenWater() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = Strings.waterGuidance(language),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = waterGuidance.statusText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = waterGuidance.recommendedAction,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenNutrients() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Science,
                            contentDescription = null,
                            tint = Color(0xFF7C3AED),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = Strings.nutrientGuidance(language),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (nutrientGuidance.isManuallyEntered) "Soil Health Card" else Strings.notConnected(language),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = nutrientGuidance.guidanceText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Field Intelligence Modules Grid
        Text(
            text = Strings.quickActions(language),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                QuickNavButton(
                    icon = Icons.Default.Router,
                    title = "ESP32 & Calibration",
                    subtitle = "Port 8080, token, dry/wet ADC",
                    onClick = onOpenDevice,
                    modifier = Modifier.weight(1f)
                )
                QuickNavButton(
                    icon = Icons.Default.History,
                    title = "Soil Log History",
                    subtitle = "Recorded ESP32 packets",
                    onClick = onOpenHistory,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                QuickNavButton(
                    icon = Icons.Default.TrendingUp,
                    title = Strings.demandIntelligence(language),
                    subtitle = "Static crop planning reference",
                    onClick = onOpenDemand,
                    modifier = Modifier.weight(1f)
                )
                QuickNavButton(
                    icon = Icons.Default.Calculate,
                    title = Strings.finalDecision(language),
                    subtitle = "Save cultivation record",
                    onClick = onOpenDecision,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                QuickNavButton(
                    icon = Icons.Default.CompareArrows,
                    title = "Plot Management",
                    subtitle = "Manage & compare plots",
                    onClick = onOpenComparison,
                    modifier = Modifier.weight(1f)
                )
                QuickNavButton(
                    icon = Icons.Default.Science,
                    title = "Soil Health Card",
                    subtitle = "Enter manual lab values",
                    onClick = onOpenSoilResults,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DashboardMetricCard(
    label: String,
    value: String,
    statusText: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HeroMiniStat(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.12f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QuickNavButton(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
