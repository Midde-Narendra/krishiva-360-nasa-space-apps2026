package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.models.CalibrationConfig
import com.example.domain.models.Esp32TelemetryStatus
import com.example.domain.models.Farm
import com.example.domain.models.Farmer
import com.example.domain.models.ReceiverStatus
import com.example.domain.models.SensorDevice
import com.example.localization.AppLanguage
import com.example.ui.components.StatusPill

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileSettingsScreen(
    farmer: Farmer,
    farm: Farm,
    device: SensorDevice,
    receiverStatus: ReceiverStatus,
    esp32Status: Esp32TelemetryStatus,
    calibration: CalibrationConfig,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onToggleLiveConnection: (Boolean) -> Unit,
    onOpenDeviceScreen: () -> Unit,
    onOpenHistoryScreen: () -> Unit,
    onOpenComparisonScreen: () -> Unit,
    onEditFarmProfile: () -> Unit,
    onAddPlot: () -> Unit,
    onClearHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("profile_settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Farmer Profile & ESP32 Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold
        )

        // Farmer & Farm Profile Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = farmer.name.ifBlank { "Farmer Profile Not Configured" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = farm.name.ifBlank { "No Farm Name Set" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    OutlinedButton(
                        onClick = onEditFarmProfile,
                        modifier = Modifier.testTag("edit_farm_profile_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit")
                    }
                }

                Text(
                    text = "Location: ${
                        listOf(farmer.village, farmer.district, farmer.state)
                            .filter { it.isNotBlank() }
                            .joinToString(", ")
                            .ifBlank { "Not specified" }
                    }",
                    style = MaterialTheme.typography.bodySmall
                )
                val areaLabel = farm.areaAcres?.let { "$it Acres" } ?: "Not set"
                Text(
                    text = "Farm Area: $areaLabel • Primary Crop: ${farm.mainCrop.ifBlank { "Not set" }}",
                    style = MaterialTheme.typography.bodySmall
                )

                OutlinedButton(
                    onClick = onAddPlot,
                    modifier = Modifier.fillMaxWidth().testTag("add_plot_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Field Plot")
                }
            }
        }

        // Language Selection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Language / భాష / भाषा",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = language == lang,
                            onClick = { onLanguageChange(lang) },
                            label = { Text("${lang.nativeName} (${lang.displayName})") }
                        )
                    }
                }
            }
        }

        // Live ESP32 Connection & Calibration Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Live ESP32 Connection (Port ${device.receiverPort})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = device.telemetryEndpointUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Switch(
                        checked = device.receiverEnabled,
                        onCheckedChange = onToggleLiveConnection,
                        modifier = Modifier.testTag("profile_live_connection_switch")
                    )
                }

                StatusPill(receiverStatus = receiverStatus, esp32Status = esp32Status)

                Text(
                    text = "Calibration: ${
                        if (calibration.isConfigured) {
                            "Dry=${calibration.dryRaw} ADC, Wet=${calibration.wetRaw} ADC"
                        } else {
                            "Calibration required (Dry/Wet references unset)"
                        }
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )

                Button(
                    onClick = onOpenDeviceScreen,
                    modifier = Modifier.fillMaxWidth().testTag("open_esp32_setup_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Router, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Configure ESP32 Token, IP & Dry/Wet Calibration")
                }
            }
        }

        // Data & Logs Card
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onOpenHistoryScreen,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.History, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Soil Log")
            }
            OutlinedButton(
                onClick = onOpenComparisonScreen,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.CompareArrows, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Plots (${farm.activePlots})")
            }
        }

        OutlinedButton(
            onClick = onClearHistory,
            modifier = Modifier.fillMaxWidth().testTag("clear_recorded_history_button")
        ) {
            Icon(Icons.Default.DeleteOutline, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Clear Recorded Local Telemetry History")
        }
    }
}
