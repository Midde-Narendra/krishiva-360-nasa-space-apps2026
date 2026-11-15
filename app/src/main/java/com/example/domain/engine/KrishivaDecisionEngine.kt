package com.example.domain.engine

import com.example.domain.models.CropRecommendation
import com.example.domain.models.DemandInformation
import com.example.domain.models.DemandLevel
import com.example.domain.models.NutrientRating
import com.example.domain.models.SoilReading
import com.example.domain.models.TrendDirection
import kotlin.math.abs
import kotlin.math.roundToInt

object KrishivaDecisionEngine {

    private data class CropReferenceProfile(
        val id: String,
        val cropName: String,
        val hindiName: String,
        val teluguName: String,
        val minOptimalMoisturePct: Double,
        val maxOptimalMoisturePct: Double,
        val minOptimalPh: Double,
        val maxOptimalPh: Double,
        val waterRequirement: String,
        val season: String
    )

    private val cropProfiles = listOf(
        CropReferenceProfile(
            id = "crop_groundnut",
            cropName = "Groundnut",
            hindiName = "मूंगफली (Groundnut)",
            teluguName = "వేరుశనగ (Groundnut)",
            minOptimalMoisturePct = 35.0,
            maxOptimalMoisturePct = 55.0,
            minOptimalPh = 6.0,
            maxOptimalPh = 7.2,
            waterRequirement = "Moderate (Reference index: 35–55%)",
            season = "Kharif / Rabi"
        ),
        CropReferenceProfile(
            id = "crop_maize",
            cropName = "Maize",
            hindiName = "मक्का (Maize)",
            teluguName = "మొక్కజొన్న (Maize)",
            minOptimalMoisturePct = 45.0,
            maxOptimalMoisturePct = 65.0,
            minOptimalPh = 5.8,
            maxOptimalPh = 7.5,
            waterRequirement = "Moderate to High (Reference index: 45–65%)",
            season = "Kharif / Rabi"
        ),
        CropReferenceProfile(
            id = "crop_bengal_gram",
            cropName = "Bengal Gram (Chickpea)",
            hindiName = "चना (Chickpea)",
            teluguName = "శనగలు (Chickpea)",
            minOptimalMoisturePct = 25.0,
            maxOptimalMoisturePct = 45.0,
            minOptimalPh = 6.0,
            maxOptimalPh = 7.5,
            waterRequirement = "Low to Moderate (Reference index: 25–45%)",
            season = "Rabi"
        ),
        CropReferenceProfile(
            id = "crop_cotton",
            cropName = "Cotton",
            hindiName = "कपास (Cotton)",
            teluguName = "పత్తి (Cotton)",
            minOptimalMoisturePct = 40.0,
            maxOptimalMoisturePct = 60.0,
            minOptimalPh = 6.0,
            maxOptimalPh = 7.8,
            waterRequirement = "Moderate (Reference index: 40–60%)",
            season = "Kharif"
        ),
        CropReferenceProfile(
            id = "crop_paddy",
            cropName = "Paddy",
            hindiName = "धान (Paddy)",
            teluguName = "వరి (Paddy)",
            minOptimalMoisturePct = 75.0,
            maxOptimalMoisturePct = 100.0,
            minOptimalPh = 5.5,
            maxOptimalPh = 7.0,
            waterRequirement = "High / Saturated (Reference index: 75–100%)",
            season = "Kharif / Rabi"
        )
    )

    /**
     * Evaluates provisional crop compatibility using ONLY measured or manually entered soil factors.
     * - Never invents pH, temperature, or N/P/K/S values when those sensors are not connected.
     * - Never includes market demand in the soil suitability score.
     * - Returns null scores when no calibrated soil-moisture reading is available.
     */
    fun evaluateSuitability(soil: SoilReading?): List<CropRecommendation> {
        val moisturePct = soil?.moisturePercent
        if (soil == null || moisturePct == null || !soil.calibrated) {
            return cropProfiles.map { profile ->
                val rangeText = "${profile.minOptimalMoisturePct.toInt()}–${profile.maxOptimalMoisturePct.toInt()}%"
                CropRecommendation(
                    id = profile.id,
                    cropName = profile.cropName,
                    hindiName = profile.hindiName,
                    teluguName = profile.teluguName,
                    suitabilityScore = null,
                    isProvisionalMoistureOnly = true,
                    measuredFactorsCount = 0,
                    totalFactorsCount = 4,
                    optimalMoistureRangeText = rangeText,
                    waterRequirement = profile.waterRequirement,
                    moistureCompatibility = null,
                    phCompatibility = null,
                    nutrientCompatibility = null,
                    shortExplanation = if (soil == null) {
                        "Waiting for live ESP32 soil scan."
                    } else {
                        "Calibration required before computing relative moisture compatibility."
                    },
                    decisionReason = "Provisional crop guidance requires a calibrated relative moisture reading from the ESP32 sensor. pH, temperature, and N/P/K/S sensors are not connected.",
                    season = profile.season
                )
            }
        }

        val recommendations = cropProfiles.map { profile ->
            val rangeText = "${profile.minOptimalMoisturePct.toInt()}–${profile.maxOptimalMoisturePct.toInt()}%"
            val moistureMatch = computeRangeCompatibility(
                value = moisturePct,
                minOptimal = profile.minOptimalMoisturePct,
                maxOptimal = profile.maxOptimalMoisturePct,
                maxDeviation = 45.0
            )

            val phMatch: Int? = soil.ph?.let { measuredPh ->
                computeRangeCompatibility(
                    value = measuredPh,
                    minOptimal = profile.minOptimalPh,
                    maxOptimal = profile.maxOptimalPh,
                    maxDeviation = 2.5
                )
            }

            val nutrientRatings = listOfNotNull(
                soil.nitrogen,
                soil.phosphorus,
                soil.potassium,
                soil.sulfur
            )
            val nutrientMatch: Int? = if (nutrientRatings.isNotEmpty()) {
                nutrientRatings.map { rating ->
                    when (rating) {
                        NutrientRating.LOW -> 45
                        NutrientRating.MODERATE -> 75
                        NutrientRating.GOOD -> 92
                        NutrientRating.HIGH -> 88
                    }
                }.average().roundToInt()
            } else {
                null
            }

            var weightedSum = moistureMatch * 0.50
            var totalWeight = 0.50
            var factorsMeasured = 1 // Moisture

            if (phMatch != null) {
                weightedSum += phMatch * 0.25
                totalWeight += 0.25
                factorsMeasured += 1
            }
            if (nutrientMatch != null) {
                weightedSum += nutrientMatch * 0.25
                totalWeight += 0.25
                factorsMeasured += 1
            }

            val normalizedScore = (weightedSum / totalWeight).roundToInt().coerceIn(0, 100)
            val isMoistureOnly = factorsMeasured == 1

            val missingFactorsNote = buildList {
                if (phMatch == null) add("pH")
                if (soil.temperatureCelsius == null) add("soil temperature")
                if (nutrientMatch == null) add("N/P/K/S")
            }.joinToString(", ")

            val shortExp = buildString {
                append("Provisional (Based on $factorsMeasured of 4 factors): ")
                append("Relative moisture ${String.format("%.1f", moisturePct)}% vs reference $rangeText.")
                if (missingFactorsNote.isNotEmpty()) {
                    append(" Unmeasured: $missingFactorsNote.")
                }
            }

            val detailedReason = buildString {
                append("Measured relative moisture is ${String.format("%.1f", moisturePct)}% ")
                append("(Raw ADC: ${soil.moistureRaw ?: "N/A"}), ")
                append("compared against ${profile.cropName}'s reference moisture index window ($rangeText). ")
                if (isMoistureOnly) {
                    append("Because pH, soil temperature, EC, and N/P/K/S sensors are not connected, this score reflects moisture compatibility only and must be verified with a Soil Health Card before sowing.")
                } else {
                    append("Includes manually entered Soil Health Card values.")
                }
            }

            CropRecommendation(
                id = profile.id,
                cropName = profile.cropName,
                hindiName = profile.hindiName,
                teluguName = profile.teluguName,
                suitabilityScore = normalizedScore,
                isProvisionalMoistureOnly = isMoistureOnly,
                measuredFactorsCount = factorsMeasured,
                totalFactorsCount = 4,
                optimalMoistureRangeText = rangeText,
                waterRequirement = profile.waterRequirement,
                moistureCompatibility = moistureMatch,
                phCompatibility = phMatch,
                nutrientCompatibility = nutrientMatch,
                shortExplanation = shortExp,
                decisionReason = detailedReason,
                season = profile.season
            )
        }

        return recommendations.sortedByDescending { it.suitabilityScore ?: -1 }
    }

    private fun computeRangeCompatibility(
        value: Double,
        minOptimal: Double,
        maxOptimal: Double,
        maxDeviation: Double
    ): Int {
        return when {
            value in minOptimal..maxOptimal -> {
                val center = (minOptimal + maxOptimal) / 2.0
                val halfSpan = ((maxOptimal - minOptimal) / 2.0).coerceAtLeast(1.0)
                val centerOffset = abs(value - center) / halfSpan
                (96.0 - centerOffset * 8.0).roundToInt().coerceIn(85, 98)
            }
            value < minOptimal -> {
                val deficit = minOptimal - value
                val ratio = (1.0 - (deficit / maxDeviation)).coerceIn(0.10, 0.84)
                (ratio * 100.0).roundToInt()
            }
            else -> {
                val excess = value - maxOptimal
                val ratio = (1.0 - (excess / maxDeviation)).coerceIn(0.10, 0.84)
                (ratio * 100.0).roundToInt()
            }
        }
    }

    fun getRegionalDemandData(): List<DemandInformation> {
        return listOf(
            DemandInformation(
                cropName = "Groundnut",
                level = DemandLevel.HIGH,
                trend = TrendDirection.STABLE,
                referenceNote = "Live market data not configured — static seasonal planning reference",
                regionalNotes = "Oilseed crops typically see steady demand from regional extraction mills. Check your local APMC mandi board for current daily arrivals and rates.",
                mspStatus = "Verify current season MSP notification via official CACP / e-NAM portal",
                isLiveMarketData = false
            ),
            DemandInformation(
                cropName = "Maize",
                level = DemandLevel.MEDIUM,
                trend = TrendDirection.STABLE,
                referenceNote = "Live market data not configured — static seasonal planning reference",
                regionalNotes = "Used in poultry feed and starch processing. Verify local procurement prices at your nearest mandi before sowing.",
                mspStatus = "Verify current season MSP notification via official CACP / e-NAM portal",
                isLiveMarketData = false
            ),
            DemandInformation(
                cropName = "Bengal Gram (Chickpea)",
                level = DemandLevel.HIGH,
                trend = TrendDirection.STABLE,
                referenceNote = "Live market data not configured — static seasonal planning reference",
                regionalNotes = "Suitable for lower residual soil moisture in Rabi season. Check local pulse mandi rates for live price discovery.",
                mspStatus = "Verify current season MSP notification via official CACP / e-NAM portal",
                isLiveMarketData = false
            ),
            DemandInformation(
                cropName = "Paddy",
                level = DemandLevel.MEDIUM,
                trend = TrendDirection.STABLE,
                referenceNote = "Live market data not configured — static seasonal planning reference",
                regionalNotes = "Requires sustained high water availability. Check district procurement center schedules and water table advisories.",
                mspStatus = "Verify current season MSP notification via official CACP / e-NAM portal",
                isLiveMarketData = false
            )
        )
    }
}
