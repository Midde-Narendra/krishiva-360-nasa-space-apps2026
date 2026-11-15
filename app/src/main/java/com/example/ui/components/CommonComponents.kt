package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.domain.models.CropRecommendation
import com.example.domain.models.Esp32TelemetryStatus
import com.example.domain.models.NutrientRating
import com.example.domain.models.ReceiverStatus
import com.example.localization.AppLanguage
import com.example.localization.Strings
import com.example.ui.theme.AgriBackground
import com.example.ui.theme.AgriEarthAmber
import com.example.ui.theme.AgriGreenContainer
import com.example.ui.theme.AgriGreenPrimary
import com.example.ui.theme.AgriGreenSecondary
import com.example.ui.theme.AgriOnGreenContainer
import com.example.ui.theme.AgriSkyBlue
import com.example.ui.theme.AgriTextPrimary
import com.example.ui.theme.AgriTextSecondary
import com.example.ui.theme.AgriTextTertiary

@Composable
fun StatusPill(
    receiverStatus: ReceiverStatus,
    esp32Status: Esp32TelemetryStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when {
        receiverStatus == ReceiverStatus.STOPPED -> Triple(
            Color(0xFFE2E8F0),
            Color(0xFF334155),
            "● RECEIVER STOPPED"
        )
        receiverStatus == ReceiverStatus.ERROR -> Triple(
            Color(0xFFFEE2E2),
            Color(0xFF991B1B),
            "● RECEIVER ERROR"
        )
        esp32Status == Esp32TelemetryStatus.CONNECTED -> Triple(
            Color(0xFFDCFCE7),
            Color(0xFF14532D),
            "● ESP32 CONNECTED"
        )
        esp32Status == Esp32TelemetryStatus.DISCONNECTED -> Triple(
            Color(0xFFFEE2E2),
            Color(0xFF991B1B),
            "● ESP32 DISCONNECTED"
        )
        else -> Triple(
            Color(0xFFFEF3C7),
            Color(0xFF92400E),
            "● WAITING FOR ESP32"
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .testTag("status_pill"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp
        )
    }
}

@Composable
fun CircularMetricRing(
    valueText: String,
    labelText: String,
    percent: Float, // 0f to 1f
    color: Color = AgriGreenSecondary,
    size: Dp = 88.dp,
    strokeWidth: Dp = 7.dp,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = percent.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500),
        label = "MetricProgress"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(size)) {
                drawArc(
                    color = color.copy(alpha = 0.15f),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = color,
                    startAngle = 135f,
                    sweepAngle = 270f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 6.dp)
            ) {
                Text(
                    text = valueText,
                    fontSize = if (valueText.length > 5) 11.sp else 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AgriTextPrimary,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = labelText,
            fontSize = 11.sp,
            color = AgriTextSecondary,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun NutrientHorizontalMeter(
    symbol: String,
    name: String,
    rating: NutrientRating?,
    modifier: Modifier = Modifier
) {
    val (progress, ratingColor, label) = when (rating) {
        NutrientRating.LOW -> Triple(0.25f, Color(0xFFEF4444), "Low")
        NutrientRating.MODERATE -> Triple(0.55f, Color(0xFFF59E0B), "Moderate")
        NutrientRating.GOOD -> Triple(0.80f, Color(0xFF10B981), "Good")
        NutrientRating.HIGH -> Triple(1.0f, Color(0xFF059669), "High")
        null -> Triple(0.0f, Color(0xFF64748B), "Not connected")
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (rating != null) AgriGreenContainer else Color(0xFFE2E8F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = symbol,
                        color = if (rating != null) AgriOnGreenContainer else Color(0xFF475569),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AgriTextPrimary
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(ratingColor.copy(alpha = 0.14f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = label,
                    color = ratingColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = ratingColor,
            trackColor = Color(0xFFE2E8F0),
        )
    }
}

@Composable
fun WhyThisCropDialog(
    crop: CropRecommendation,
    lang: AppLanguage,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("why_this_crop_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = AgriBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = Strings.whyThisCrop(lang),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AgriGreenPrimary
                        )
                        Text(
                            text = crop.cropName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = AgriTextPrimary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (crop.suitabilityScore != null) AgriGreenContainer else Color(0xFFE2E8F0))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = crop.suitabilityScore?.let { "$it% Provisional" } ?: "No Reading",
                            color = if (crop.suitabilityScore != null) AgriOnGreenContainer else Color(0xFF475569),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Measured vs Unconnected Factors (${crop.measuredFactorsCount} of ${crop.totalFactorsCount} available):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AgriTextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                FactorScoreRow(
                    label = "Relative Moisture Match (Ref: ${crop.optimalMoistureRangeText})",
                    percent = crop.moistureCompatibility,
                    color = AgriSkyBlue
                )
                Spacer(modifier = Modifier.height(6.dp))
                FactorScoreRow(
                    label = "Soil pH Compatibility",
                    percent = crop.phCompatibility,
                    color = AgriGreenSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                FactorScoreRow(
                    label = "Soil Temperature Sensor",
                    percent = null,
                    color = AgriEarthAmber
                )
                Spacer(modifier = Modifier.height(6.dp))
                FactorScoreRow(
                    label = "N / P / K / S Nutrient Assay",
                    percent = crop.nutrientCompatibility,
                    color = AgriEarthAmber
                )

                Spacer(modifier = Modifier.height(14.dp))

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = Strings.decisionLogic(lang),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AgriTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = crop.decisionReason,
                            fontSize = 11.sp,
                            color = AgriTextPrimary,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Provisional guidance only: confirm pH, EC, and N/P/K/S with an official Soil Health Card.",
                    fontSize = 10.sp,
                    color = AgriTextTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AgriGreenPrimary)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FactorScoreRow(label: String, percent: Int?, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 11.sp, color = AgriTextSecondary, fontWeight = FontWeight.Medium)
            Text(
                text = percent?.let { "$it%" } ?: "Not connected",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (percent != null) color else Color(0xFF64748B)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { (percent ?: 0) / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(2.5.dp)),
            color = color,
            trackColor = Color(0xFFE2E8F0)
        )
    }
}
