package com.example.cropcare

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
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
        val card = panel(); val body = LinearLayout(context).apply { orientation = VERTICAL; visibility = View.GONE }
        val button = MaterialButton(context).apply {
            text = "$title  ▸"; isAllCaps = false; textSize = 14f; gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
            setTextColor(green); backgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(243, 248, 239))
            minHeight = dp(48); contentDescription = "Expand $title"
            setOnClickListener {
                val open = body.visibility != View.VISIBLE; body.visibility = if (open) View.VISIBLE else View.GONE
                text = "$title  ${if (open) "▾" else "▸"}"; contentDescription = "${if (open) "Collapse" else "Expand"} $title"
            }
        }
        card.addView(button, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)); build(body); card.addView(body); addView(card)
    }
    private fun rows(array: JSONArray?, each: (JSONObject, Int) -> Unit) {
        for (i in 0 until (array?.length() ?: 0)) array?.optJSONObject(i)?.let { each(it, i) }
    }
    fun render(assessment: FarmerAssessment) {
        removeAllViews(); val json = assessment.json; val primary = json.optJSONObject("primaryAction")
        val hero = panel(Color.rgb(237, 246, 232)); addText(hero, "YOUR NEXT STEP", 11, true)
        addText(hero, primary?.optString("title").orEmpty(), 22, true)
        addText(hero, primary?.optString("whatToDo").orEmpty(), 16)
        val valid = json.optJSONObject("soilHealth")?.optInt("validSensors") ?: 0
        val installed = json.optJSONObject("sensorAllocation")?.optInt("installed") ?: 0
        addText(hero, "Soil score: ${assessment.score?.let { String.format(Locale.US, "%.1f/100", it) } ?: "Not ready"}", 18, true)
        addText(hero, "$valid of $installed sensors usable · ${assessment.label}")
        if (json.optBoolean("partialCoverage")) addText(hero, "Some readings cannot be used. This does not describe every part of your zone.", 13)
        val time = SimpleDateFormat("MMM d, h:mm a", Locale.US).apply { timeZone = TimeZone.getTimeZone("Asia/Manila") }.format(Date(assessment.evaluatedAtMs))
        addText(hero, "Updated $time · Rules-based guidance. Confirm field conditions before acting.", 11); addView(hero)
        val other = json.optJSONArray("recommendations")
        val otherActions = (0 until (other?.length() ?: 0)).mapNotNull { other?.optJSONObject(it) }.filter { it.optString("code") != primary?.optString("code") }
        if (otherActions.isNotEmpty()) section("Other checks (${otherActions.size})") { body ->
            otherActions.forEach { row -> addText(body, row.optString("title"), 16, true); addText(body, row.optString("whatToDo")); addText(body, row.optString("why"), 12) }
        }
        section("Sensor readings and quality") { body ->
            rows(json.optJSONArray("sensorQuality")) { row, i ->
                addText(body, "Sensor ${i + 1} · ${row.optString("status").replace('_', ' ')}", 15, true)
                addText(body, row.optString("message")); addText(body, "Device record: ${row.optString("sensorId")}", 11)
            }
        }
        section("Fertilizer reference · reference only") { body ->
            val plan = json.optJSONObject("referenceFertilizerPlan"); val entries = plan?.optJSONArray("entries")
            addText(body, "Published program ${assessment.program}. Amounts are not calculated from your sensor readings.")
            if (entries == null || entries.length() == 0) addText(body, "Quantities withheld. Do not substitute an earlier result.", 14, true)
            rows(entries) { row, _ -> addText(body, "${row.optString("product")} (${row.optString("fertilizerGrade")})", 15, true); addText(body, "${row.optDouble("kgForPlot")} kg for this plot · ${row.optString("applicationTiming")}") }
            addText(body, plan?.optString("warning").orEmpty(), 12)
            addText(body, "DAE means days after emergence, not planting. Prior applications are unknown.", 12)
        }
        section("Sources and limitations") { body ->
            rows(json.optJSONArray("evidenceReferences")) { row, _ -> addText(body, row.optString("title"), 14, true); addText(body, row.optString("basis"), 12); addText(body, row.optString("url"), 11) }
            val limits = json.optJSONArray("limitations"); for(i in 0 until (limits?.length() ?: 0)) addText(body, limits?.optString(i).orEmpty(), 12)
            addText(body, "${json.optString("rulesetVersion")} · ${json.optString("datasetVersion")}", 11)
        }
        section("Reference program options") { body ->
            addText(body, "Published hybrid-corn reference", 13)
            val spinner = Spinner(context); spinner.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, listOf("HYB-1 · 5–8 t/ha", "HYB-2 · 8–12 t/ha"))
            spinner.setSelection(if (assessment.program == "HYB-2") 1 else 0)
            spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val program = if (position == 1) "HYB-2" else "HYB-1"; if (program != assessment.program) onProgramChanged(program)
                }
            }; body.addView(spinner, LayoutParams(LayoutParams.MATCH_PARENT, dp(52)))
        }
    }
}
