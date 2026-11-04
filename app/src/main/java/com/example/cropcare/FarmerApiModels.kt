package com.example.cropcare

object FarmerValidation {
    fun password(value: String) = value.length in 8..128
    fun username(value: String) = Regex("^[A-Za-z0-9._-]{3,30}$").matches(value)
    fun code(value: String) = Regex("^[0-9]{6}$").matches(value)
    fun reference(value: String) = Regex("^[0-9]{8,20}$").matches(value)
}

enum class SubscriptionState {
    FREE, PENDING, REJECTED, PRO, UNAVAILABLE;
    companion object {
        fun parse(value: String?) = entries.firstOrNull { it.name.equals(value, true) } ?: UNAVAILABLE
    }
    val canSubmit: Boolean get() = this == FREE || this == REJECTED
}
