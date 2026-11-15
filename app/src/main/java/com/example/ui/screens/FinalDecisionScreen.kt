package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.models.CropRecommendation
import com.example.domain.models.Farm
import com.example.domain.models.FarmDecision
import com.example.domain.models.NutrientGuidance
import com.example.domain.models.SoilReading
import com.example.domain.models.WaterGuidance
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinalDecisionScreen(
    farm: Farm,
    soil: SoilReading?,
    selectedCrop: CropRecommendation?,
    waterGuidance: WaterGuidance,
    nutrientGuidance: NutrientGuidance,
    savedDecisions: List<FarmDecision>,
    onSaveDecision: (CropRecommendation) -> Unit,
    onBackToDashboard: () -> Unit
) {
    var savedBanner by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Final Cultivation Decision", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBackToDashboard) {
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
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("final_decision_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = selectedCrop?.let { "${it.cropName} (${it.teluguName})" } ?: "No Crop Selected",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Farm: ${farm.name.ifBlank { "Local Field" }}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    val score = selectedCrop?.suitabilityScore
                    Text(
                        text = "Moisture Compatibility: ${
                            if (score != null) "$score% Provisional (Calibrated ESP32)" else "Uncalibrated / Not scanned"
                        }",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    val pctText = soil?.moisturePercent?.let { "${it.roundToInt()}%" } ?: "Calibration required"
                    Text(
                        text = "Measured Soil Moisture: Raw ADC ${soil?.moistureRaw ?: "--"} • Relative $pctText",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Water Guidance: ${waterGuidance.statusText} — ${waterGuidance.recommendedAction}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Nutrient Guidance: ${nutrientGuidance.guidanceText}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (selectedCrop != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = {
                                onSaveDecision(selectedCrop)
                                savedBanner = true
                            },
                            modifier = Modifier.fillMaxWidth().testTag("save_final_decision_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Cultivation Decision Locally")
                        }
                    }

                    if (savedBanner) {
                        Text(
                            text = "Decision saved to local Room database.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (savedDecisions.isNotEmpty()) {
                Text(
                    text = "Saved Cultivation Decisions (${savedDecisions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                savedDecisions.forEach { rec ->
                    val recScore = rec.suitabilityPercent
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rec.recommendedCrop, fontWeight = FontWeight.Bold)
                                Text(rec.waterSummary, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                text = if (recScore != null) "$recScore% Match" else "Unscored",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
