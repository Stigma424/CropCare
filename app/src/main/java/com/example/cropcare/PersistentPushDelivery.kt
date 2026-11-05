package com.example.cropcare

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object PersistentPushDelivery {
    @Synchronized fun accept(context: Context, uid: String, id: String): Boolean {
        val prefs = context.getSharedPreferences("CropCarePushDelivery", Context.MODE_PRIVATE)
        val key = "messages:$uid"
        val rows = runCatching { JSONArray(prefs.getString(key, "[]")) }.getOrElse { JSONArray() }
        val now = System.currentTimeMillis()
        val retained = (0 until rows.length()).mapNotNull { rows.optJSONObject(it) }
            .filter { now - it.optLong("at") < 72L * 60 * 60 * 1000 }.toMutableList()
        if (retained.any { it.optString("id") == id }) return false
        retained.add(JSONObject().put("id", id).put("at", now))
        val bounded = JSONArray(); retained.takeLast(200).forEach { bounded.put(it) }
        prefs.edit().putString(key, bounded.toString()).apply()
        return true
    }
}
