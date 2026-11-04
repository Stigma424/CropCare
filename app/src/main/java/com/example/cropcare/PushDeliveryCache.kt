package com.example.cropcare

/** Message identity only; identical text from distinct events is not deduplicated. */
object PushDeliveryCache {
    private val ids = LinkedHashSet<String>()
    @Synchronized fun accept(id: String?): Boolean {
        if (id.isNullOrBlank()) return true
        if (!ids.add(id)) return false
        while (ids.size > 200) ids.remove(ids.first())
        return true
    }
}
