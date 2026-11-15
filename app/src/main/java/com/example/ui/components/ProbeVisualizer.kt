package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AgriEarthAmber
import com.example.ui.theme.AgriGreenLight
import com.example.ui.theme.AgriGreenPrimary
import com.example.ui.theme.AgriGreenSecondary
import com.example.ui.theme.AgriSkyBlue

@Composable
fun SmartProbeGraphic(
    isScanning: Boolean,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ProbeAnim")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WaveOffset"
    )

    Box(
        modifier = modifier
            .width(220.dp)
            .height(230.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val topY = 20f
            val groundY = size.height * 0.72f

            // 1. Soil Horizon Ground Layer
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF8D6E63), Color(0xFF5D4037))
                ),
                topLeft = Offset(10f, groundY),
                size = Size(size.width - 20f, size.height - groundY),
                cornerRadius = CornerRadius(14f, 14f)
            )

            // Soil texture specks
            for (i in 0..12) {
                val speckX = 25f + (i * 15f) % (size.width - 50f)
                val speckY = groundY + 12f + (i * 9f) % (size.height - groundY - 20f)
                drawCircle(
                    color = Color(0x33000000),
                    radius = 3f,
                    center = Offset(speckX, speckY)
                )
            }

            // 2. Pulse Waves when Scanning
            if (isScanning) {
                // Multi-spectral sensor pulses into the subsoil
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(AgriGreenLight.copy(alpha = 0.45f), Color.Transparent),
                        center = Offset(cx, groundY + 35f),
                        radius = 45f * pulseScale
                    ),
                    center = Offset(cx, groundY + 35f),
                    radius = 45f * pulseScale
                )

                // Subsoil Telemetry Waves
                drawArc(
                    color = AgriSkyBlue.copy(alpha = 0.6f),
                    startAngle = 30f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(cx - 30f - waveOffset, groundY + 15f),
                    size = Size(60f + waveOffset * 2, 40f + waveOffset),
                    style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                )
            }

            // 3. Central Detachable Smart Soil Probe Body
            val handleWidth = 42f
            val handleHeight = 55f
            // Handle / Top Telemetry Enclosure (Waterproof PETG)
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF1E293B), Color(0xFF334155), Color(0xFF1E293B))
                ),
                topLeft = Offset(cx - handleWidth / 2f, topY),
                size = Size(handleWidth, handleHeight),
                cornerRadius = CornerRadius(8f, 8f)
            )

            // Probe Status LED
            val ledColor = if (isScanning) AgriGreenLight else Color(0xFF10B981)
            drawCircle(
                color = ledColor,
                radius = 5f,
                center = Offset(cx, topY + 18f)
            )
            if (isScanning) {
                drawCircle(
                    color = ledColor.copy(alpha = 0.35f),
                    radius = 9f * pulseScale,
                    center = Offset(cx, topY + 18f)
                )
            }

            // Solar mini-panel stripe
            drawRoundRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(cx - 14f, topY + 30f),
                size = Size(28f, 16f),
                cornerRadius = CornerRadius(3f, 3f)
            )

            // 4. Stainless Steel / Titanium Probe Rods (penetrating soil)
            val rodWidth = 8f
            val rodLen = (groundY + 45f) - (topY + handleHeight)

            // Center Rod
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF94A3B8), Color(0xFFE2E8F0), Color(0xFF64748B))
                ),
                topLeft = Offset(cx - rodWidth / 2f, topY + handleHeight),
                size = Size(rodWidth, rodLen),
                cornerRadius = CornerRadius(3f, 3f)
            )

            // Left Electrode Prong
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF64748B), Color(0xFFCBD5E1))
                ),
                topLeft = Offset(cx - 18f, topY + handleHeight + 15f),
                size = Size(5f, rodLen - 15f),
                cornerRadius = CornerRadius(2f, 2f)
            )

            // Right Electrode Prong
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFFCBD5E1), Color(0xFF64748B))
                ),
                topLeft = Offset(cx + 13f, topY + handleHeight + 15f),
                size = Size(5f, rodLen - 15f),
                cornerRadius = CornerRadius(2f, 2f)
            )

            // Gold sensing tips embedded in soil
            val goldTipY = groundY + 38f
            drawCircle(color = AgriEarthAmber, radius = 4f, center = Offset(cx, goldTipY))
            drawCircle(color = AgriEarthAmber, radius = 3f, center = Offset(cx - 15.5f, goldTipY - 2f))
            drawCircle(color = AgriEarthAmber, radius = 3f, center = Offset(cx + 15.5f, goldTipY - 2f))

            // 5. Sprout shoot emerging beside probe to symbolize Crop Intelligence
            val sproutPath = Path().apply {
                moveTo(cx + 32f, groundY)
                cubicTo(cx + 36f, groundY - 24f, cx + 55f, groundY - 30f, cx + 58f, groundY - 45f)
                cubicTo(cx + 42f, groundY - 35f, cx + 38f, groundY - 18f, cx + 32f, groundY)
                close()
            }
            drawPath(
                path = sproutPath,
                color = AgriGreenSecondary
            )

            // Ground level line marker
            drawLine(
                color = Color(0x66FFFFFF),
                start = Offset(10f, groundY),
                end = Offset(size.width - 10f, groundY),
                strokeWidth = 2f
            )
        }
    }
}
