package com.example.cropcare

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject

object CropCareFcmRegistration {
    @Volatile var status = "Registration not checked"
        private set
    fun register() {
        if (FirebaseAuth.getInstance().currentUser == null) return
        status = "Registering push notifications…"
        FirebaseMessaging.getInstance().token.addOnSuccessListener { save(it) }
            .addOnFailureListener { status = "Token unavailable; tap Refresh to retry" }
    }
    fun save(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (token.isBlank()) return
        CropCareHttpClient.request("PUT", "/api/account/fcm-token", JSONObject().put("token", token)) { result ->
            if (FirebaseAuth.getInstance().currentUser?.uid == uid) status = if (result.successful) "Push token registered" else "Push registration failed; tap Refresh to retry"
        }
    }
    fun detach(callback: (Boolean) -> Unit) {
        if (FirebaseAuth.getInstance().currentUser == null) { callback(true); return }
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            CropCareHttpClient.request("DELETE", "/api/account/fcm-token", JSONObject().put("token", token)) { callback(it.successful) }
        }.addOnFailureListener { callback(false) }
    }
}
