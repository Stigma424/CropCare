package com.example.cropcare

import org.json.JSONObject

data class FarmerAssessment(val json: JSONObject) {
    val score: Double? get() = (json.optJSONObject("soilHealth")?.opt("score") as? Number)?.toDouble()?.takeIf { it.isFinite() }
    val label: String get() = json.optJSONObject("soilHealth")?.optString("label") ?: "Unavailable"
    val actions: List<JSONObject> get() = json.optJSONArray("recommendations").let { a ->
        (0 until (a?.length() ?: 0)).mapNotNull { a?.optJSONObject(it) }
    }
    val displayPlan: JSONObject? get() = json.optJSONObject("programFertilizerPlan") ?: json.optJSONObject("referenceFertilizerPlan")
    val planEntries: List<JSONObject> get() = displayPlan?.optJSONArray("entries").let { a ->
        (0 until (a?.length() ?: 0)).mapNotNull { a?.optJSONObject(it) }
    }
    private val blockingCodes = setOf("CHECK_SENSOR", "CHECK_REGISTRATION", "AWAIT_READINGS", "LOW_MOISTURE", "HIGH_MOISTURE", "LOW_PH", "HIGH_PH", "EC_REVIEW", "TEMPERATURE_REVIEW", "PLANTING_DATE_REQUIRED", "POST_HARVEST")
    val hasPlan: Boolean get() = planEntries.isNotEmpty()
    val canReviewApplication: Boolean get() = hasPlan && actions.any { it.optString("code") == "NUTRIENT_REVIEW" } && actions.none { it.optString("code") in blockingCodes }
    val applicationNote: String get() = if (!hasPlan) json.optJSONObject("primaryAction")?.optString("whatToDo").orEmpty()
        else if (actions.any { it.optString("code") in blockingCodes }) "Before applying: " + actions.filter { it.optString("code") in blockingCodes }.joinToString(" ") { it.optString("whatToDo") }
        else if (canReviewApplication) "Nutrient readings are low. Use this crop schedule after confirming the crop stage and earlier applications."
        else "Keep this crop schedule for the appropriate stage. Current readings do not establish a need for an extra fertilizer dose."
    val whatToApply: String get() = if (hasPlan) planEntries.joinToString("; ") { "${it.optString("product")} (${it.optString("fertilizerGrade")})" }
        else "A fertilizer schedule is not available for this crop cycle. " + json.optJSONObject("primaryAction")?.optString("whatToDo").orEmpty()
    val howToApply: String get() = if (hasPlan) "Use 14-14-14 as the basal application and urea as the side-dress at the listed stage. Follow each product label and check root-zone moisture."
        else "Complete the priority action, then refresh this assessment."
    val whenToApply: String get() = if (hasPlan) planEntries.joinToString("; ") { "${it.optString("product")}: ${it.optString("applicationTiming")}" } + ". DAE means days after emergence; confirm actual crop stage."
        else "Refresh after the crop-cycle details or priority issue are resolved."
    val howMuchToApply: String get() = if (hasPlan) planEntries.joinToString("; ") { "${it.optString("product")}: ${String.format(java.util.Locale.US, "%.2f", it.optDouble("kgForPlot"))} kg for this plot" } + ". Planned program totals; account for fertilizer already applied."
        else "Complete the current crop-cycle setup to display its calculated program quantities."
    val summary: String get() = listOf("What to apply" to whatToApply, "How to apply" to howToApply, "When to apply" to whenToApply, "How much to apply" to howMuchToApply)
        .joinToString("\n\n") { (title, text) -> "• $title\n$text" } + "\n\n• Application status\n" + applicationNote
    val program: String get() = json.optString("programId", "HYB-1")
    val evaluatedAtMs: Long get() = json.optLong("evaluatedAtMs")
    fun details(): String = buildString {
        append("Rules-based advisory\n").append(summary).append("\n\nSoil health score: ").append(score?.let { "$it/100" } ?: "Unavailable")
        val soil = json.optJSONObject("soilHealth")
        val allocation = json.optJSONObject("sensorAllocation")
        append("\n${soil?.optInt("validSensors") ?: 0} usable / ${allocation?.optInt("installed") ?: 0} installed; ${json.optInt("excludedSensors")} excluded.")
        if (json.optBoolean("partialCoverage")) append(" Partial coverage.")
        val quality = json.optJSONArray("sensorQuality")
        for (i in 0 until (quality?.length() ?: 0)) quality?.optJSONObject(i)?.let { row ->
            append("\n\n${row.optString("sensorId")}: ${row.optString("status")}\n${row.optString("message")}\n${row.optJSONArray("reasonCodes") ?: ""}")
        }
        val actions = json.optJSONArray("recommendations")
        for (i in 0 until (actions?.length() ?: 0)) actions?.optJSONObject(i)?.let { row ->
            append("\n\n${row.optString("title")}\n${row.optString("whatToDo")}\nWhy: ${row.optString("why")}\nRule: ${row.optString("sourceId")}")
        }
        val plan = json.optJSONObject("referenceFertilizerPlan")
        append("\n\nPUBLISHED REFERENCE ONLY\n").append(plan?.optString("warning") ?: "Reference unavailable")
        val entries = plan?.optJSONArray("entries")
        if (entries == null || entries.length() == 0) append("\nQuantities withheld. Do not substitute an earlier result.")
        for (i in 0 until (entries?.length() ?: 0)) entries?.optJSONObject(i)?.let { e ->
            append("\n${e.optString("product")} (${e.optString("fertilizerGrade")}): ${e.optDouble("kgForPlot")} kg for plot\n${e.optString("applicationTiming")}")
        }
        append("\nDAE means days after emergence, not planting. Prior applications are unknown.")
        val evidence = json.optJSONArray("evidenceReferences")
        for (i in 0 until (evidence?.length() ?: 0)) evidence?.optJSONObject(i)?.let { e ->
            append("\n\n${e.optString("title")} · ${e.optString("evidenceClass")}\n${e.optString("basis")}\n${e.optString("url", "")}")
        }
        append("\n\n${json.optString("rulesetVersion")} · ${json.optString("datasetVersion")}")
    }
    companion object {
        fun parse(json: JSONObject): FarmerAssessment {
            require(json.optJSONArray("sensorQuality") != null && json.optJSONObject("primaryAction") != null && json.optString("assessmentStatus").isNotBlank()) { "API assessment update required" }
            return FarmerAssessment(json)
        }
    }
}
