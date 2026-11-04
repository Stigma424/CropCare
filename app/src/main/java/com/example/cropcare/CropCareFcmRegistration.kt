
package com.example.cropcare

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

object CropCareFcmRegistration {

    fun register() {
        FirebaseMessaging.getInstance()
            .token
            .addOnSuccessListener { token ->
                save(token)
            }
            .addOnFailureListener { error ->
                Log.w(
                    "CropCareFCM",
                    "Token request failed",
                    error
                )
            }
    }

    fun save(token: String) {
        val uid =
            FirebaseAuth.getInstance().currentUser?.uid
                ?: return

        if (token.isBlank()) {
            return
        }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .update("fcmToken", token)
            .addOnFailureListener { error ->
                Log.w(
                    "CropCareFCM",
                    "Could not save FCM token",
                    error
                )
            }
    }
    fun detach(callback: (Boolean) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: return callback(true)
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            val ref = FirebaseFirestore.getInstance().collection("users").document(user.uid)
            FirebaseFirestore.getInstance().runTransaction { tx ->
                val doc = tx.get(ref)
                if (doc.exists() && doc.getString("fcmToken") == token) {
                    tx.update(ref, "fcmToken", com.google.firebase.firestore.FieldValue.delete())
                }
                true
            }.addOnSuccessListener { callback(true) }.addOnFailureListener { callback(false) }
        }.addOnFailureListener { callback(false) }
    }}
