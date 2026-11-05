package com.example.cropcare

data class AccountPushEnvelope(val id: String, val recipient: String, val title: String, val body: String) {
    companion object {
        fun parse(data: Map<String, String>, currentUid: String?): AccountPushEnvelope? {
            val id = data["notificationId"]?.takeIf { it.isNotBlank() } ?: return null
            val recipient = data["recipientUid"]?.takeIf { it == currentUid && it.isNotBlank() } ?: return null
            return AccountPushEnvelope(id, recipient, data["title"] ?: "CropCare update", data["body"] ?: "Open notification history for details.")
        }
    }
}
