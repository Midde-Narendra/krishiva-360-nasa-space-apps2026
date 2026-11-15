package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.models.CalibrationConfig
import com.example.domain.models.Esp32TelemetryStatus
import com.example.domain.models.LiveTelemetry
import com.example.domain.models.ReceiverStatus
import com.example.domain.models.SensorDevice
import com.example.ui.components.StatusPill
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorDeviceScreen(
    device: SensorDevice,
    receiverStatus: ReceiverStatus,
    esp32Status: Esp32TelemetryStatus,
    deviceToken: String,
    calibration: CalibrationConfig,
    liveTelemetry: LiveTelemetry?,
    onToggleLiveConnection: (Boolean) -> Unit,
    onUpdateManualIpOverride: (String) -> Unit,
    onRegenerateToken: () -> Unit,
    onRestartReceiver: () -> Unit,
    onSaveDryCalibration: (Int?) -> Unit,
    onSaveWetCalibration: (Int?) -> Unit,
    onClearCalibration: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showToken by remember { mutableStateOf(false) }
    var ipInput by remember(device.manualPhoneIpOverride) { mutableStateOf(device.manualPhoneIpOverride) }
    var dryInput by remember(calibration.dryRaw) { mutableStateOf(calibration.dryRaw?.toString() ?: "") }
    var wetInput by remember(calibration.wetRaw) { mutableStateOf(calibration.wetRaw?.toString() ?: "") }
    var copyFeedback by remember { mutableStateOf<String?>(null) }
    val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ESP32 Receiver & Calibration", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onRestartReceiver, modifier = Modifier.testTag("refresh_ip_button")) {
                        Icon(Icons.Default.Refresh, contentDescription = "Restart Receiver & Refresh IP")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("sensor_device_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card 1: Phone Receiver & ESP32 Telemetry Status
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Live HTTP Receiver (Port ${device.receiverPort})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Phone hosts /api/telemetry on local 2.4 GHz Wi-Fi",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = device.receiverEnabled,
                            onCheckedChange = onToggleLiveConnection,
                            modifier = Modifier.testTag("device_live_receiver_switch")
                        )
                    }

                    StatusPill(receiverStatus = receiverStatus, esp32Status = esp32Status)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Phone Receiver Status:", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = receiverStatus.name,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("ESP32 Telemetry Status:", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "${esp32Status.name} (10s freshness timeout)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (esp32Status == Esp32TelemetryStatus.CONNECTED) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color(0xFFB45309)
                            }
                        )
                    }

                    if (device.receiverError != null) {
                        Surface(
                            color = Color(0xFFFEE2E2),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = device.receiverError,
                                color = Color(0xFF991B1B),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }

            // Card 2: Network Endpoint & X-Device-Token
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Router, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ESP32 Firmware Endpoint & Token",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Text(
                        text = "Detected Phone IPv4: ${device.detectedPhoneIp ?: "Not detected on Wi-Fi/Hotspot — enter manual IP below"}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = ipInput,
                            onValueChange = { ipInput = it },
                            label = { Text("Manual Phone IPv4 Override (Optional)") },
                            placeholder = { Text("e.g. 192.168.43.1") },
                            modifier = Modifier.weight(1f).testTag("manual_ip_input"),
                            singleLine = true
                        )
                        Button(
                            onClick = { onUpdateManualIpOverride(ipInput) },
                            modifier = Modifier.testTag("apply_manual_ip_button")
                        ) {
                            Text("Apply")
                        }
                    }

                    val endpointUrl = device.telemetryEndpointUrl
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Telemetry POST Endpoint URL",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = endpointUrl,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = {
                                    copyToClipboard(context, "Endpoint URL", endpointUrl)
                                    copyFeedback = "Copied Endpoint URL"
                                },
                                modifier = Modifier.testTag("copy_endpoint_button")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Endpoint URL")
                            }
                        }
                    }

                    // Device Token Row
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "X-Device-Token Header",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (showToken) deviceToken else "•".repeat(16),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("device_token_text")
                                )
                            }
                            IconButton(
                                onClick = { showToken = !showToken },
                                modifier = Modifier.testTag("toggle_reveal_token_button")
                            ) {
                                Icon(
                                    imageVector = if (showToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showToken) "Hide Token" else "Reveal Token"
                                )
                            }
                            IconButton(
                                onClick = {
                                    copyToClipboard(context, "X-Device-Token", deviceToken)
                                    copyFeedback = "Copied X-Device-Token"
                                },
                                modifier = Modifier.testTag("copy_token_button")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Token")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onRegenerateToken,
                            modifier = Modifier.testTag("regenerate_token_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Regenerate Token")
                        }
                        if (copyFeedback != null) {
                            Text(
                                text = copyFeedback!!,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Local Network Warning
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = Color(0xFFB45309)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Prototype notice: Uses plain HTTP on your trusted local 2.4 GHz Wi-Fi / phone hotspot. Do not expose port 8080 to public untrusted networks.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                }
            }

            // Card 3: Dry / Wet Soil Calibration (GPIO34 ADC 0..4095)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Sensors, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Dry / Wet Soil Moisture Calibration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Text(
                        text = "Analog resistive soil-moisture modules vary by probe geometry and wiring direction. Place the probe in dry soil and save Dry Reference, then place in saturated wet soil and save Wet Reference.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val currentRaw = liveTelemetry?.moistureRaw ?: device.lastMoistureRaw
                    val currentPct = liveTelemetry?.calibratedMoisturePercent ?: device.lastCalibratedPercent

                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Current Live Raw ADC: ${if (currentRaw != null) "$currentRaw / 4095" else "Waiting for ESP32"}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                            val pctFormatted = if (currentPct != null) {
                                String.format(Locale.US, "%.1f%%", currentPct)
                            } else {
                                "Calibration required"
                            }
                            Text(
                                text = "Calibrated Relative Moisture (%): $pctFormatted",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (currentPct != null) MaterialTheme.colorScheme.primary else Color(0xFFB45309)
                            )
                            Text(
                                text = "Formula: clamp(100 * (raw - dryRaw) / (wetRaw - dryRaw), 0, 100)",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Capture from Live or Manual Entry
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = dryInput,
                            onValueChange = { dryInput = it },
                            label = { Text("Dry Soil ADC (0–4095)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("dry_calibration_input"),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                val rawToSave = currentRaw ?: dryInput.toIntOrNull()
                                if (rawToSave != null && rawToSave in 0..4095) {
                                    dryInput = rawToSave.toString()
                                    onSaveDryCalibration(rawToSave)
                                }
                            },
                            modifier = Modifier.testTag("save_dry_reference_button")
                        ) {
                            Text(if (currentRaw != null) "Use Live ($currentRaw)" else "Save Dry")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = wetInput,
                            onValueChange = { wetInput = it },
                            label = { Text("Wet Soil ADC (0–4095)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("wet_calibration_input"),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                val rawToSave = currentRaw ?: wetInput.toIntOrNull()
                                if (rawToSave != null && rawToSave in 0..4095) {
                                    wetInput = rawToSave.toString()
                                    onSaveWetCalibration(rawToSave)
                                }
                            },
                            modifier = Modifier.testTag("save_wet_reference_button")
                        ) {
                            Text(if (currentRaw != null) "Use Live ($currentRaw)" else "Save Wet")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val d = dryInput.toIntOrNull()
                                val w = wetInput.toIntOrNull()
                                if (d != null && d in 0..4095) onSaveDryCalibration(d)
                                if (w != null && w in 0..4095) onSaveWetCalibration(w)
                            },
                            modifier = Modifier.weight(1f).testTag("apply_typed_calibration_button")
                        ) {
                            Text("Save Typed Values")
                        }
                        OutlinedButton(
                            onClick = {
                                dryInput = ""
                                wetInput = ""
                                onClearCalibration()
                            },
                            modifier = Modifier.weight(1f).testTag("clear_calibration_button")
                        ) {
                            Text("Reset Calibration")
                        }
                    }

                    if (calibration.dryRaw != null && calibration.wetRaw != null && calibration.dryRaw == calibration.wetRaw) {
                        Text(
                            text = "Dry and Wet references cannot be equal (${calibration.dryRaw}). Please record distinct dry and wet soil readings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB91C1C),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Card 4: Hardware & Telemetry Diagnostics (Truthful limits)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Hardware Diagnostics & Limits",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    DiagRow("Board Architecture", "Classic ESP32 DevKit (2.4 GHz Wi-Fi)")
                    DiagRow("Connected Sensor", device.probeStatus)
                    DiagRow("Device ID", liveTelemetry?.deviceId ?: device.deviceId ?: "None")
                    DiagRow("Firmware Version", liveTelemetry?.firmwareVersion ?: device.firmwareVersion ?: "Not reported")
                    DiagRow("Packet Sequence", liveTelemetry?.sequence?.toString() ?: device.lastSequence?.toString() ?: "None")
                    DiagRow("ESP32 Uptime", liveTelemetry?.uptimeMs?.let { "${it / 1000}s" } ?: "Not available")
                    DiagRow("Wi-Fi RSSI", liveTelemetry?.wifiRssiDbm?.let { "$it dBm" } ?: "Not available")
                    DiagRow(
                        "Last Packet Received",
                        liveTelemetry?.receivedAtMs?.let { timeFormatter.format(Date(it)) } ?: "Never"
                    )
                    DiagRow("Battery Monitoring", device.batteryStatus)
                    DiagRow("Solar Charging Telemetry", device.solarStatus)
                    DiagRow("pH / Temp / EC / NPK / Pest", "Not connected")
                }
            }
        }
    }
}

@Composable
private fun DiagRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun copyToClipboard(context: Context, label: String, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, value))
}
