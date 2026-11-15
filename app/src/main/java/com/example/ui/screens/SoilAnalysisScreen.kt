package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.models.CalibrationConfig
import com.example.domain.models.LiveTelemetry
import com.example.domain.models.NutrientRating
import com.example.domain.models.SoilReading
import com.example.ui.components.NutrientHorizontalMeter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SoilAnalysisScreen(
    soil: SoilReading?,
    liveTelemetry: LiveTelemetry?,
    calibration: CalibrationConfig,
    onBack: () -> Unit,
    onOpenCrops: () -> Unit,
    onOpenWater: () -> Unit,
    onOpenNutrients: () -> Unit,
    onOpenCalibration: () -> Unit,
    onSaveManualLabValues: (
        ph: Double?,
        nitrogen: NutrientRating?,
        phosphorus: NutrientRating?,
        potassium: NutrientRating?,
        sulfur: NutrientRating?,
        ec: Double?
    ) -> Unit
) {
    var showLabDialog by remember { mutableStateOf(false) }
    val timeFormatter = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())

    val rawAdc = liveTelemetry?.moistureRaw ?: soil?.moistureRaw
    val moisturePct = liveTelemetry?.calibratedMoisturePercent ?: soil?.moisturePercent
    val timestampMs = liveTelemetry?.receivedAtMs ?: soil?.timestamp

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Soil Intelligence Report", fontWeight = FontWeight.ExtraBold)
                        Text(
                            text = "ESP32 GPIO34 Telemetry & Soil Health Card",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .testTag("soil_analysis_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card 1: Measured Hardware Telemetry (ESP32 GPIO34)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ESP32 Hardware Measurement (GPIO34)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = soil?.source?.name ?: if (liveTelemetry != null) "ESP32_LIVE" else "NO PACKET",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Raw Soil Moisture ADC:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = if (rawAdc != null) "$rawAdc / 4095" else "Not available (no packet)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Relative Soil Moisture (%):", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = when {
                                moisturePct != null -> "${moisturePct.roundToInt()}% (Calibrated)"
                                rawAdc != null -> "Calibration required"
                                else -> "Waiting for ESP32"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (moisturePct != null) MaterialTheme.colorScheme.primary else Color(0xFFB45309)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Calibration References:", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = if (calibration.isConfigured) {
                                "Dry=${calibration.dryRaw}, Wet=${calibration.wetRaw}"
                            } else {
                                "Unset (Dry=${calibration.dryRaw ?: "--"}, Wet=${calibration.wetRaw ?: "--"})"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Device ID / Sequence:", style = MaterialTheme.typography.bodySmall)
                        val devId = liveTelemetry?.deviceId ?: soil?.deviceId
                        val seq = liveTelemetry?.sequence ?: soil?.sequenceNumber
                        Text(
                            text = if (!devId.isNullOrBlank()) "$devId (#${seq ?: 0})" else "None",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Wi-Fi Signal (RSSI):", style = MaterialTheme.typography.bodySmall)
                        val rssi = liveTelemetry?.wifiRssiDbm ?: soil?.wifiRssiDbm
                        Text(
                            text = if (rssi != null) "$rssi dBm" else "Not available",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Receipt Timestamp:", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = if (timestampMs != null) timeFormatter.format(Date(timestampMs)) else "No reading recorded",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (!calibration.isConfigured) {
                        OutlinedButton(
                            onClick = onOpenCalibration,
                            modifier = Modifier.fillMaxWidth().testTag("open_calibration_from_soil_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Router, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Calibrate Dry & Wet Reference Values")
                        }
                    }
                }
            }

            // Card 2: Non-Connected Hardware Parameters / Optional Manual Soil Health Card Entry
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Additional Soil Parameters",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "pH, temperature, EC, and N-P-K-S sensors are not connected to the ESP32. Values below are shown only if manually entered from a laboratory Soil Health Card.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Soil pH:", style = MaterialTheme.typography.bodyMedium)
                        val phVal = soil?.ph
                        Text(
                            text = if (phVal != null) String.format(Locale.US, "%.1f (Manual Lab)", phVal) else "Not connected",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Soil Temperature:", style = MaterialTheme.typography.bodyMedium)
                        val tempVal = soil?.temperatureCelsius
                        Text(
                            text = if (tempVal != null) String.format(Locale.US, "%.1f °C", tempVal) else "Not connected",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Electrical Conductivity (EC):", style = MaterialTheme.typography.bodyMedium)
                        val ecVal = soil?.ecConductivity
                        Text(
                            text = if (ecVal != null) String.format(Locale.US, "%.2f dS/m (Manual Lab)", ecVal) else "Not connected",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    NutrientHorizontalMeter(symbol = "N", name = "Nitrogen (N)", rating = soil?.nitrogen)
                    NutrientHorizontalMeter(symbol = "P", name = "Phosphorus (P)", rating = soil?.phosphorus)
                    NutrientHorizontalMeter(symbol = "K", name = "Potassium (K)", rating = soil?.potassium)
                    NutrientHorizontalMeter(symbol = "S", name = "Sulfur (S)", rating = soil?.sulfur)

                    OutlinedButton(
                        onClick = { showLabDialog = true },
                        modifier = Modifier.fillMaxWidth().testTag("enter_manual_lab_values_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enter Manual Soil Health Card Values")
                    }
                }
            }

            // Navigation to Decision Modules
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenWater,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.WaterDrop, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Irrigation", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onOpenCrops,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Eco, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Crop Match", fontWeight = FontWeight.Bold)
                }
            }

            OutlinedButton(
                onClick = onOpenNutrients,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Science, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Nutrient Guidance", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showLabDialog) {
        var phText by remember { mutableStateOf(soil?.ph?.toString() ?: "") }
        var ecText by remember { mutableStateOf(soil?.ecConductivity?.toString() ?: "") }
        var nRating by remember { mutableStateOf(soil?.nitrogen) }
        var pRating by remember { mutableStateOf(soil?.phosphorus) }
        var kRating by remember { mutableStateOf(soil?.potassium) }
        var sRating by remember { mutableStateOf(soil?.sulfur) }

        AlertDialog(
            onDismissRequest = { showLabDialog = false },
            title = { Text("Manual Soil Health Card Entry", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Enter laboratory values from your Soil Health Card. Leave blank if not measured.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = phText,
                            onValueChange = { phText = it },
                            label = { Text("pH") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = ecText,
                            onValueChange = { ecText = it },
                            label = { Text("EC (dS/m)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    NutrientRatingSelector("Nitrogen (N)", nRating) { nRating = it }
                    NutrientRatingSelector("Phosphorus (P)", pRating) { pRating = it }
                    NutrientRatingSelector("Potassium (K)", kRating) { kRating = it }
                    NutrientRatingSelector("Sulfur (S)", sRating) { sRating = it }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveManualLabValues(
                            phText.toDoubleOrNull(),
                            nRating,
                            pRating,
                            kRating,
                            sRating,
                            ecText.toDoubleOrNull()
                        )
                        showLabDialog = false
                    }
                ) {
                    Text("Save Lab Values")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLabDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NutrientRatingSelector(
    label: String,
    selected: NutrientRating?,
    onSelect: (NutrientRating?) -> Unit
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            NutrientRating.entries.forEach { rating ->
                FilterChip(
                    selected = selected == rating,
                    onClick = { onSelect(if (selected == rating) null else rating) },
                    label = { Text(rating.name) }
                )
            }
        }
    }
}
