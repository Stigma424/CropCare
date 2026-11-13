package com.example.cropcare

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject

object CropCareFcmRegistration {
    @Volatile var status = "Registration not checked"
        private set
    fun register(done: ((String) -> Unit)? = null) {
        if (FirebaseAuth.getInstance().currentUser == null) { status = "Sign in to register phone alerts"; done?.invoke(status); return }
        status = "Registering push notifications…"
        FirebaseMessaging.getInstance().token.addOnSuccessListener { save(it, done) }
            .addOnFailureListener { status = "Firebase token unavailable; check Google Play services and connection"; done?.invoke(status) }
    }
    fun save(token: String, done: ((String) -> Unit)? = null) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (token.isBlank()) return
        CropCareHttpClient.request("PUT", "/api/account/fcm-token", JSONObject().put("token", token)) { result ->
            if (FirebaseAuth.getInstance().currentUser?.uid == uid) status = if (result.successful) "Push token registered" else "Push registration failed (${result.statusCode}): ${result.message}"
            done?.invoke(status)
        }
    }
    fun detach(callback: (Boolean) -> Unit) {
        if (FirebaseAuth.getInstance().currentUser == null) { callback(true); return }
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            CropCareHttpClient.request("DELETE", "/api/account/fcm-token", JSONObject().put("token", token)) { callback(it.successful) }
        }.addOnFailureListener { callback(false) }
    }
}

