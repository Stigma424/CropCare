package com.example.cropcare

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Farmer-facing summary with optional supporting detail. The server owns every decision. */
class RecommendationView(context: Context, private val onProgramChanged: (String) -> Unit) : LinearLayout(context) {
    private val green = Color.rgb(29, 78, 43)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    init { orientation = VERTICAL; setPadding(dp(20), dp(8), dp(20), dp(16)) }
    fun showMessage(message: String) { removeAllViews(); addText(this, message, 16) }
    private fun panel(color: Int = Color.WHITE) = LinearLayout(context).apply {
        orientation = VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(16))
        background = GradientDrawable().apply { setColor(color); cornerRadius = dp(14).toFloat() }
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(12) }
    }
    private fun addText(parent: LinearLayout, value: String, size: Int = 14, bold: Boolean = false) = TextView(context).also {
        it.text = value; it.textSize = size.toFloat(); it.setTextColor(green)
        it.setLineSpacing(dp(3).toFloat(), 1f); it.setPadding(0, 0, 0, dp(10))
        if (bold) it.setTypeface(it.typeface, Typeface.BOLD)
        it.setTextIsSelectable(true); parent.addView(it)
    }
    private fun section(title: String, build: (LinearLayout) -> Unit) {
        val card = panel()
        addText(card, title, 17, true)
        val body = LinearLayout(context).apply {
            orientation = VERTICAL
        }
        build(body); card.addView(body); addView(card)
    }
    private fun rows(array: JSONArray?, each: (JSONObject, Int) -> Unit) {
        for (i in 0 until (array?.length() ?: 0)) array?.optJSONObject(i)?.let { each(it, i) }
    }
    fun renderSummary(assessment: FarmerAssessment) {
        removeAllViews()
        setPadding(0, dp(4), 0, 0)
        listOf("What to apply" to assessment.whatToApply, "How to apply" to assessment.howToApply,
            "When to apply" to assessment.whenToApply, "How much to apply" to assessment.howMuchToApply).forEach { (title, text) ->
            val card = panel(Color.rgb(237, 246, 232))
            addText(card, title, 16, true)
            addText(card, "• $text", 14)
            addView(card)
        }
        val status = panel(Color.rgb(255, 247, 223))
        addText(status, "Application status", 14, true)
        addText(status, "• " + assessment.applicationNote, 13)
        addView(status)
    }
    fun render(assessment: FarmerAssessment) {
        removeAllViews(); val json = assessment.json; val primary = json.optJSONObject("primaryAction")
        val hero = panel(Color.rgb(237, 246, 232)); addText(hero, "YOUR NEXT STEP", 11, true)
        addText(hero, if (assessment.hasPlan) "Your corn application plan" else "Your crop-cycle check", 22, true)
        addText(hero, "Products, amounts and timing for your zone", 14)
        addView(hero)
        listOf("What to apply" to assessment.whatToApply, "How to apply" to assessment.howToApply,
            "When to apply" to assessment.whenToApply, "How much to apply" to assessment.howMuchToApply).forEach { (title, text) ->
            val card = panel(if (assessment.canReviewApplication) Color.rgb(237, 246, 232) else Color.rgb(255, 247, 223))
            addText(card, title, 17, true)
            addText(card, "• $text", 15).setTextColor(if (assessment.canReviewApplication) green else Color.rgb(116, 83, 25))
            addView(card)
        }
        section("Before application") { body -> addText(body, "• " + assessment.applicationNote) }
        section("Priority field action") { body ->
            addText(body, primary?.optString("title").orEmpty(), 16, true)
            addText(body, "• " + primary?.optString("whatToDo").orEmpty())
            addText(body, "• Why: " + primary?.optString("why").orEmpty(), 12)
        }
        val scoreCard = panel()
        val valid = json.optJSONObject("soilHealth")?.optInt("validSensors") ?: 0
        val installed = json.optJSONObject("sensorAllocation")?.optInt("installed") ?: 0
        addText(scoreCard, "Soil health score: ${assessment.score?.let { String.format(Locale.US, "%.1f/100", it) } ?: "Not ready"}", 18, true)
        addText(scoreCard, "$valid of $installed sensors usable · ${assessment.label}")
        addText(scoreCard, "Sensor-based condition score; laboratory testing provides a fuller soil assessment.", 12)
        if (json.optBoolean("partialCoverage")) addText(scoreCard, "• Some readings cannot be used. This does not describe every part of your zone.", 13)
        val time = SimpleDateFormat("MMM d, h:mm a", Locale.US).apply { timeZone = TimeZone.getTimeZone("Asia/Manila") }.format(Date(assessment.evaluatedAtMs))
        addText(scoreCard, "Updated $time · Based on your zone assessment.", 11); addView(scoreCard)
        val other = json.optJSONArray("recommendations")
        val otherActions = (0 until (other?.length() ?: 0)).mapNotNull { other?.optJSONObject(it) }.filter { it.optString("code") != primary?.optString("code") }
        if (otherActions.isNotEmpty()) section("Other checks (${otherActions.size})") { body ->
            otherActions.forEach { row -> addText(body, row.optString("title"), 16, true); addText(body, "• " + row.optString("whatToDo")); addText(body, "• " + row.optString("why"), 12) }
        }
        section("Sensor readings and quality") { body ->
            rows(json.optJSONArray("sensorQuality")) { row, i ->
                val usable = row.optBoolean("canUseForScore")
                addText(body, "Sensor ${i + 1} · ${if (usable) "USABLE" else "CHECK REQUIRED"}", 15, true)
                    .setTextColor(if (usable) green else Color.rgb(164, 48, 48))
                addText(body, "• " + row.optString("message")); addText(body, "Device record: ${row.optString("sensorId")}", 11)
            }
        }
        section("Full application plan") { body ->
            val plan = assessment.displayPlan; val entries = plan?.optJSONArray("entries")
            addText(body, "Corn program ${assessment.program} · Published amounts scaled to your zone area, not a measured soil-deficit dose. Complete field and crop-stage checks before application.")
            if (entries == null || entries.length() == 0) addText(body, "Complete your zone area and planting details and resolve the priority issue above to calculate an application plan.", 14, true)
            rows(entries) { row, _ -> addText(body, "${row.optString("product")} (${row.optString("fertilizerGrade")})", 15, true); addText(body, "• Amount: ${String.format(Locale.US, "%.2f", row.optDouble("kgForPlot"))} kg for this plot\n• When: ${row.optString("applicationTiming")}") }
            addText(body, "How to apply: Follow the product label and the application timing shown for each product. Check soil moisture before application and account for fertilizer already applied this cycle.", 13)
            addText(body, "DAE means days after emergence, not planting. Prior applications are unknown.", 12)
        }
        section("Corn program options") { body ->
            addText(body, "Published hybrid-corn yield target. This is a target, not a predicted yield.", 13)
            listOf("HYB-1" to "5–8 t/ha", "HYB-2" to "8–12 t/ha").forEach { (id, target) ->
                body.addView(MaterialButton(context).apply {
                    text = "${if (id == assessment.program) "✓ " else ""}$id · $target"; isAllCaps = false
                    minHeight = dp(52); cornerRadius = dp(12)
                    isCheckable = true; isChecked = id == assessment.program
                    setOnClickListener { if (id != assessment.program) onProgramChanged(id) }
                }, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
            }
        }
        section("Sources and limitations") { body ->
            rows(json.optJSONArray("evidenceReferences")) { row, _ -> addText(body, row.optString("title"), 14, true); addText(body, "• " + row.optString("basis"), 12); addText(body, row.optString("url"), 11) }
            val limits = json.optJSONArray("limitations"); for(i in 0 until (limits?.length() ?: 0)) addText(body, "• " + limits?.optString(i).orEmpty(), 12)
            addText(body, "${json.optString("rulesetVersion")} · ${json.optString("datasetVersion")}", 11)
        }
    }
}


