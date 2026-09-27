package com.example.cropcare

import java.util.Locale

data class RecommendationResult(
    val stageName: String,
    val summaryText: String,
    val detailedAdvice: String,
    val isPremiumLocked: Boolean,
    val actionItems: List<String>
)

object CornRecommendationEngine {

    fun generateRecommendation(
        n: Double,
        p: Double,
        k: Double,
        ph: Double,
        moisture: Double,
        cornAgeDays: Long,
        isHarvested: Boolean,
        zoneAreaSqm: Double,
        isSubscribed: Boolean
    ): RecommendationResult {

        if (isHarvested) {
            return RecommendationResult(
                stageName = "Post-Harvest / Fallow",
                summaryText = "Zone is marked as harvested. Prepare soil for next planting.",
                detailedAdvice = "Post-Harvest Soil Management:\n• Test soil pH and organic matter.\n• Incorporate crop residues or organic compost into soil.\n• Soil area: ${zoneAreaSqm.toInt()} sqm.",
                isPremiumLocked = false,
                actionItems = listOf("Clear crop residue", "Apply organic compost", "Prepare field for next crop cycle")
            )
        }

        if (n <= 0.0 && p <= 0.0 && k <= 0.0) {
            return RecommendationResult(
                stageName = "Awaiting Sensor Data",
                summaryText = "Connect soil sensors to receive custom corn fertilization advice.",
                detailedAdvice = "No active soil readings detected. Please ensure your soil sensor is powered on and assigned to this zone.",
                isPremiumLocked = false,
                actionItems = listOf("Add sensor to zone", "Check device online status")
            )
        }

        // Determine Corn Growth Stage based on DAP (Days After Planting)
        val stageName: String
        val targetN: Double
        val targetP: Double
        val targetK: Double
        val stageGuide: String

        when {
            cornAgeDays <= 14 -> {
                stageName = "Stage 1: Basal / Emergence (0–14 DAP)"
                targetN = 35.0
                targetP = 25.0
                targetK = 30.0
                stageGuide = "Focus on root establishment and uniform seedling emergence."
            }
            cornAgeDays in 15..30 -> {
                stageName = "Stage 2: Active Vegetative V6–V8 (15–30 DAP)"
                targetN = 65.0
                targetP = 20.0
                targetK = 40.0
                stageGuide = "Peak leaf and stalk expansion. Nitrogen demand is extremely high."
            }
            cornAgeDays in 31..55 -> {
                stageName = "Stage 3: Tasseling & Silking VT–R1 (31–55 DAP)"
                targetN = 60.0
                targetP = 25.0
                targetK = 50.0
                stageGuide = "Ear initialization and flowering. High Nitrogen and Potassium needed for grain size."
            }
            else -> {
                stageName = "Stage 4: Grain Filling & Maturity (56+ DAP)"
                targetN = 30.0
                targetP = 15.0
                targetK = 35.0
                stageGuide = "Kernel starch accumulation. Maintain proper irrigation; avoid excess late Nitrogen."
            }
        }

        // Deficit calculation
        val defN = (targetN - n).coerceAtLeast(0.0)
        val defP = (targetP - p).coerceAtLeast(0.0)
        val defK = (targetK - k).coerceAtLeast(0.0)

        // Convert field size sqm to hectares (1 ha = 10,000 sqm)
        val hectares = if (zoneAreaSqm > 0) zoneAreaSqm / 10000.0 else 0.1 // Default 1000 sqm (0.1 ha) if 0

        // Standard fertilizer conversion estimates (per hectare base):
        // Urea (46-0-0) -> ~2.17 kg Urea per 1 kg N deficit per ha
        // Complete (14-14-14) -> ~7.14 kg per 1 kg N/P/K deficit per ha
        // Muriate of Potash (0-0-60) -> ~1.67 kg per 1 kg K deficit per ha
        val kgUrea = (defN * 2.17 * hectares).coerceAtLeast(0.0)
        val kgComplete = (defP * 7.14 * hectares).coerceAtLeast(0.0)
        val kgPotash = (defK * 1.67 * hectares).coerceAtLeast(0.0)

        // pH Conditioning
        val phAdvice: String = when {
            ph in 5.8..7.2 -> "Soil pH is optimal (%.1f) for corn nutrient absorption.".format(Locale.US, ph)
            ph < 5.8 -> "Soil is acidic (%.1f). Apply Agricultural Lime (Calcite/Dolomite) at ~100 kg/ha to optimize fertilizer uptake.".format(Locale.US, ph)
            else -> "Soil is alkaline (%.1f). Use Ammonium Sulfate (21-0-0) instead of Urea to help lower soil pH.".format(Locale.US, ph)
        }

        // Moisture advice
        val moistureAdvice: String = when {
            moisture < 25.0 -> "Soil Moisture is LOW (%.0f%%). Irrigate field immediately BEFORE fertilizer application to prevent root burn.".format(Locale.US, moisture)
            moisture > 75.0 -> "Soil Moisture is HIGH (%.0f%%). Delay granular fertilizer application until soil drains to prevent nutrient leaching.".format(Locale.US, moisture)
            else -> "Soil Moisture is OPTIMAL (%.0f%%) for side-dressing fertilizer.".format(Locale.US, moisture)
        }

        val actions = mutableListOf<String>()

        val summary: String
        val detailed: String

        if (!isSubscribed) {
            // Free Version Output
            summary = when {
                defN > 15 -> "WARNING: Nitrogen deficit detected for $stageName. Upgrade to Premium for exact kg formula."
                defP > 10 -> "WARNING: Phosphorus deficit detected for $stageName. Upgrade to Premium for exact kg formula."
                defK > 15 -> "WARNING: Potassium deficit detected for $stageName. Upgrade to Premium for exact kg formula."
                else -> "Soil levels are currently fair for $stageName."
            }

            detailed = """
                |$stageName
                |$stageGuide
                |
                |Basic Status Overview:
                |• Nitrogen (N): ${if (defN > 10) "LOW" else "OK"}
                |• Phosphorus (P): ${if (defP > 5) "LOW" else "OK"}
                |• Potassium (K): ${if (defK > 10) "LOW" else "OK"}
                |• Moisture: ${if (moisture < 25) "LOW" else if (moisture > 75) "HIGH" else "OK"}
                |• pH Status: ${if (ph < 5.8) "ACIDIC" else if (ph > 7.2) "ALKALINE" else "OK"}
                |
                |🔒 PREMIUM LOCKED CONTENT:
                |Subscribe to Premium to unlock:
                |  ✓ Exact fertilizer chemical products (Urea 46-0-0, Complete 14-14-14, Potash 0-0-60)
                |  ✓ Exact dosage in kg calculated for your zone size (${zoneAreaSqm.toInt()} sqm)
                |  ✓ Step-by-step application method (Basal vs Side-dressing schedule)
                |  ✓ Specific Soil pH amendment instructions
            """.trimMargin()

            actions.add("Subscribe to Premium to unlock full fertilizer calculations")
            actions.add(moistureAdvice)

            return RecommendationResult(
                stageName = stageName,
                summaryText = summary,
                detailedAdvice = detailed,
                isPremiumLocked = true,
                actionItems = actions
            )
        } else {
            // Premium Subscribed Output
            val sbDetailed = StringBuilder()
            sbDetailed.append("PRO Corn Fertilization Plan for Zone (${zoneAreaSqm.toInt()} sqm)\n")
            sbDetailed.append("Stage: $stageName\n")
            sbDetailed.append("$stageGuide\n\n")

            sbDetailed.append("Calculated Fertilizer Dosage:\n")

            if (kgComplete > 0.1) {
                val line = "• Complete 14-14-14: Apply %.1f kg (Basal / root zone placement)".format(Locale.US, kgComplete)
                sbDetailed.append(line).append("\n")
                actions.add("Apply %.1f kg of Complete (14-14-14)".format(Locale.US, kgComplete))
            }

            if (kgUrea > 0.1) {
                val line = "• Urea (46-0-0): Side-dress %.1f kg (Band 5cm away from plant base)".format(Locale.US, kgUrea)
                sbDetailed.append(line).append("\n")
                actions.add("Side-dress %.1f kg of Urea (46-0-0)".format(Locale.US, kgUrea))
            }

            if (kgPotash > 0.1) {
                val line = "• Muriate of Potash (0-0-60): Apply %.1f kg for ear development".format(Locale.US, kgPotash)
                sbDetailed.append(line).append("\n")
                actions.add("Apply %.1f kg of Muriate of Potash (0-0-60)".format(Locale.US, kgPotash))
            }

            if (kgUrea <= 0.1 && kgComplete <= 0.1 && kgPotash <= 0.1) {
                sbDetailed.append("• NPK levels are in target range! No immediate chemical fertilizer needed.\n")
                actions.add("Soil nutrients optimal for current stage")
            }

            sbDetailed.append("\nWatering & Soil Management:\n")
            sbDetailed.append("• ").append(moistureAdvice).append("\n")
            sbDetailed.append("• ").append(phAdvice)

            actions.add(moistureAdvice)

            summary = if (kgUrea > 0.1 || kgComplete > 0.1) {
                "Rec: Apply %.1f kg Urea & %.1f kg Complete 14-14-14 for $stageName.".format(Locale.US, kgUrea, kgComplete)
            } else {
                "Soil nutrients optimal for $stageName."
            }

            return RecommendationResult(
                stageName = stageName,
                summaryText = summary,
                detailedAdvice = sbDetailed.toString(),
                isPremiumLocked = false,
                actionItems = actions
            )
        }
    }
}
