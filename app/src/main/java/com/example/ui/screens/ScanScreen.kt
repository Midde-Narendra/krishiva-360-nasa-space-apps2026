package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.models.CalibrationConfig
import com.example.domain.models.Esp32TelemetryStatus
import com.example.domain.models.LiveTelemetry
import com.example.domain.models.ReceiverStatus
import com.example.domain.models.SensorDevice
import com.example.domain.models.SoilReading
import com.example.hardware.ScanStage
import com.example.ui.components.StatusPill
import kotlin.math.roundToInt

@Composable
fun ScanScreen(
    receiverStatus: ReceiverStatus,
    esp32Status: Esp32TelemetryStatus,
    sensorDevice: SensorDevice,
    scanStage: ScanStage,
    isScanning: Boolean,
    latestSoil: SoilReading?,
    liveTelemetry: LiveTelemetry?,
    calibration: CalibrationConfig,
    onToggleLiveConnection: (Boolean) -> Unit,
    onTriggerScan: () -> Unit,
    onViewResults: () -> Unit,
    onOpenDeviceSettings: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val radarWave by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("scan_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Field Soil Moisture Scan",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "ESP32 DevKit • GPIO34 Analog Soil-Moisture Probe",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusPill(receiverStatus = receiverStatus, esp32Status = esp32Status)
        }

        // Receiver Toggle Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Phone HTTP Receiver (Port ${sensorDevice.receiverPort})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = sensorDevice.telemetryEndpointUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Switch(
                    checked = sensorDevice.receiverEnabled,
                    onCheckedChange = onToggleLiveConnection,
                    modifier = Modifier.testTag("scan_live_receiver_switch")
                )
            }
        }

        // Animated Probe Radar Visualizer
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(190.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val maxRadius = size.minDimension / 2f
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.08f),
                            radius = maxRadius,
                            center = center
                        )
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.16f),
                            radius = maxRadius * 0.7f,
                            center = center
                        )
                        if (isScanning) {
                            drawCircle(
                                color = primaryColor.copy(alpha = 1f - radarWave),
                                radius = maxRadius * radarWave,
                                center = center,
                                style = Stroke(width = 6f)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(
                                when (scanStage) {
                                    ScanStage.COMPLETE -> Color(0xFF15803D)
                                    ScanStage.TIMEOUT_ERROR, ScanStage.RECEIVER_STOPPED_ERROR -> Color(0xFFB91C1C)
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (scanStage) {
                                ScanStage.COMPLETE -> Icons.Default.CheckCircle
                                ScanStage.TIMEOUT_ERROR, ScanStage.RECEIVER_STOPPED_ERROR -> Icons.Default.ErrorOutline
                                else -> Icons.Default.Sensors
                            },
                            contentDescription = "Scan Status",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = scanStage.displayText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Insert the resistive soil-moisture probe 10–15 cm into the root zone. Live Scan waits up to 8 seconds for a fresh authenticated packet from your ESP32.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                LinearProgressIndicator(
                    progress = { scanStage.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                )
            }
        }

        // Live Packet Preview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Live Telemetry Stream (GPIO34)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Raw ADC (0–4095):", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = liveTelemetry?.moistureRaw?.toString()
                            ?: latestSoil?.moistureRaw?.toString()
                            ?: "Waiting for ESP32",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Relative Moisture (%):", style = MaterialTheme.typography.bodyMedium)
                    val pct = liveTelemetry?.calibratedMoisturePercent ?: latestSoil?.moisturePercent
                    val pctText = if (pct != null) {
                        "${pct.roundToInt()}%"
                    } else if (!calibration.isConfigured) {
                        "Calibration required"
                    } else {
                        "Waiting for ESP32"
                    }
                    Text(
                        text = pctText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (pct != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Sequence / RSSI:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = liveTelemetry?.let {
                            val rssiText = it.wifiRssiDbm?.let { r -> "$r dBm" } ?: "RSSI N/A"
                            "#${it.sequence} • $rssiText"
                        } ?: "No live packet in session",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Primary Scan Action Button
        Button(
            onClick = onTriggerScan,
            enabled = !isScanning,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("start_live_scan_button"),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Radar, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isScanning) "Waiting for Fresh ESP32 Packet..." else "Capture Live ESP32 Reading",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onViewResults,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("scan_view_results_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Soil Report & Lab Entry", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onOpenDeviceSettings,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("scan_open_device_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Router, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Calibrate / Token", fontWeight = FontWeight.Bold)
            }
        }
    }
}
