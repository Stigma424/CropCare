package com.example.cropcare

enum class OnboardingDecision { COMPLETE, CREATE_PROFILE, RETRY, REJECT }

fun onboardingDecision(result: FarmerApiResult, uid: String?): OnboardingDecision = when {
    uid.isNullOrBlank() -> OnboardingDecision.REJECT
    result.successful && result.data?.optString("userId") == uid -> OnboardingDecision.COMPLETE
    result.successful -> OnboardingDecision.REJECT
    result.statusCode == 404 -> OnboardingDecision.CREATE_PROFILE
    else -> OnboardingDecision.RETRY
}
