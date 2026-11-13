package com.example.cropcare

/** Display bands aligned with the server recommendation_rules.v2 configuration. */
object ReadingStatus {
    fun style(view: android.widget.TextView) {
        val status = view.text.toString().uppercase()
        val good = status == "GOOD"
        val alert = status.contains("LOW") || status.contains("HIGH") || status.contains("CHECK SENSOR")
        view.setTextColor(android.graphics.Color.parseColor(if (alert) "#B42318" else if (good) "#176B35" else "#59636B"))
        view.background = android.graphics.drawable.GradientDrawable().apply {
            setColor(android.graphics.Color.parseColor(if (alert) "#FEE4E2" else if (good) "#DCF3E2" else "#E9ECEF"))
            cornerRadius = 8 * view.resources.displayMetrics.density
        }
    }
    fun watch(root: android.view.View) {
        if (root is android.widget.TextView && root.id != android.view.View.NO_ID) {
            val name = runCatching { root.resources.getResourceEntryName(root.id) }.getOrDefault("")
            if (name.startsWith("tvStatus") || name == "tvHistoryStatus") {
                root.addTextChangedListener(object : android.text.TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { style(root) }
                    override fun afterTextChanged(s: android.text.Editable?) {}
                })
                style(root)
            }
        }
        if (root is android.view.ViewGroup) for (i in 0 until root.childCount) watch(root.getChildAt(i))
    }
    fun label(key: String, value: Double?): String {
        if (value == null || !value.isFinite()) return "NO DATA"
        val band = when (key.lowercase()) {
            "n", "nitrogen" -> doubleArrayOf(0.0, 1999.0, 20.0, 1999.0)
            "p", "phosphorus" -> doubleArrayOf(0.0, 1999.0, 10.0, 1999.0)
            "k", "potassium" -> doubleArrayOf(0.0, 1999.0, 100.0, 1999.0)
            "ph" -> doubleArrayOf(3.0, 9.0, 6.0, 7.2)
            "moisture" -> doubleArrayOf(0.0, 100.0, 40.0, 65.0)
            "temp", "temperature" -> doubleArrayOf(-40.0, 80.0, 18.0, 35.0)
            "ec" -> doubleArrayOf(0.0, 10000.0, 0.0, 1000.0)
            else -> return "UNKNOWN"
        }
        return when {
            value < band[0] || value > band[1] -> "CHECK SENSOR"
            value < band[2] -> "LOW"
            value > band[3] -> "HIGH"
            else -> "GOOD"
        }
    }
}

