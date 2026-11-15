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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.example.domain.models.CalibrationConfig
import com.example.domain.models.CropRecommendation
import com.example.domain.models.SoilReading
import com.example.localization.AppLanguage
import com.example.ui.components.WhyThisCropDialog

@Composable
fun CropIntelligenceScreen(
    language: AppLanguage,
    soil: SoilReading?,
    calibration: CalibrationConfig,
    recommendations: List<CropRecommendation>,
    selectedCropForPlan: CropRecommendation?,
    onSelectCropForPlan: (CropRecommendation) -> Unit,
    onProceedToDecision: () -> Unit,
    onOpenWater: () -> Unit,
    onOpenNutrients: () -> Unit
) {
    var dialogCrop by remember { mutableStateOf<CropRecommendation?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("crop_intelligence_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Provisional Moisture Compatibility",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Ranked by calibrated relative soil moisture (%) from your ESP32 probe. Full agronomic suitability also depends on soil pH, EC, temperature, and N-P-K-S (not connected on current hardware).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (soil?.moisturePercent == null) {
            item {
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (!calibration.isConfigured) {
                            "Calibration required: Set Dry and Wet soil ADC references in ESP32 Setup before compatibility percentages can be calculated."
                        } else {
                            "No calibrated ESP32 moisture reading available yet. Capture a live reading on the Scan tab."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF92400E),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        items(recommendations, key = { it.id }) { rec ->
            val isSelected = selectedCropForPlan?.id == rec.id
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${rec.cropName} (${rec.teluguName})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Optimal Moisture Band: ${rec.optimalMoistureRangeText} • ${rec.season}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val score = rec.suitabilityScore
                        Surface(
                            color = if (score != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color(0xFF6B7280)
                            },
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = if (score != null) "$score% Provisional" else "Requires Scan",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Text(
                        text = rec.shortExplanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { dialogCrop = rec },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Why This Crop?")
                        }
                        Button(
                            onClick = {
                                onSelectCropForPlan(rec)
                                onProceedToDecision()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isSelected) "Selected" else "Choose Crop")
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenWater,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Water Guidance")
                }
                OutlinedButton(
                    onClick = onOpenNutrients,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Nutrient Guidance")
                }
            }
        }
    }

    if (dialogCrop != null) {
        WhyThisCropDialog(
            crop = dialogCrop!!,
            lang = language,
            onDismiss = { dialogCrop = null }
        )
    }
}
