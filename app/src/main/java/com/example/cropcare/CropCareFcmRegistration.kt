
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
}
