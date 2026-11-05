package com.example.cropcare

import org.json.JSONObject

data class FarmerAssessment(val json: JSONObject) {
    val score: Double? get() = (json.optJSONObject("soilHealth")?.opt("score") as? Number)?.toDouble()?.takeIf { it.isFinite() }
    val label: String get() = json.optJSONObject("soilHealth")?.optString("label") ?: "Unavailable"
    val summary: String get() = json.optJSONObject("primaryAction")?.let { it.optString("title") + "\n" + it.optString("whatToDo") } ?: "Assessment unavailable"
    val program: String get() = json.optString("programId", "HYB-1")
    val evaluatedAtMs: Long get() = json.optLong("evaluatedAtMs")
    fun details(): String = buildString {
        append("Rules-based advisory\n").append(summary).append("\n\nProvisional score: ").append(score?.let { "$it/100" } ?: "Unavailable")
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
