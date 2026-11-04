package com.example.cropcare

import android.net.Uri
import org.json.JSONObject

object CropCareFarmerApi {
    fun onboarding(first: String, last: String, address: String, username: String, cb: (FarmerApiResult) -> Unit) =
        CropCareHttpClient.request("POST", "/api/onboarding/profile", JSONObject().put("firstName", first)
            .put("lastName", last).put("address", address).put("username", username), callback = cb)
    fun requestCode(email: String, cb: (FarmerApiResult) -> Unit) =
        CropCareHttpClient.request("POST", "/api/recovery/request", JSONObject().put("email", email), false, cb)
    fun verifyCode(email: String, code: String, cb: (FarmerApiResult) -> Unit) =
        CropCareHttpClient.request("POST", "/api/recovery/verify", JSONObject().put("email", email).put("code", code), false, cb)
    fun reset(email: String, ticket: String, password: String, cb: (FarmerApiResult) -> Unit) =
        CropCareHttpClient.request("POST", "/api/recovery/reset", JSONObject().put("email", email)
            .put("resetTicket", ticket).put("newPassword", password), false, cb)
    fun subscription(cb: (FarmerApiResult) -> Unit) = CropCareHttpClient.request("GET", "/api/subscriptions/me", callback = cb)
    fun submitReference(reference: String, cb: (FarmerApiResult) -> Unit) =
        CropCareHttpClient.request("POST", "/api/subscriptions/request", JSONObject().put("referenceNumber", reference), callback = cb)
    fun notifications(cb: (FarmerApiResult) -> Unit) = CropCareHttpClient.request("GET", "/api/notifications?limit=100", callback = cb)
    fun markRead(id: String, cb: (FarmerApiResult) -> Unit) = CropCareHttpClient.request("POST", "/api/notifications/${Uri.encode(id)}/read", callback = cb)
    fun deleteNotification(id: String?, cb: (FarmerApiResult) -> Unit) = CropCareHttpClient.request("DELETE",
        "/api/notifications" + (id?.let { "/${Uri.encode(it)}" } ?: ""), callback = cb)
}
