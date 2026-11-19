package com.fatflow.app.data.local

import java.time.LocalDate

data class Profile(
    val startWeightLbs: Float,
    val startBfPct: Float,
    val startFatMassLbs: Float,
    val startDate: LocalDate,
    val goalWeightLbs: Float,
    val releaseRate: Int,
    val dietBreakAnchorWeightLbs: Float,
    val dietBreakAnchorBfPct: Float,
    val dietBreakStart: String,
    val usdaApiKey: String,
    val notificationsEnabled: Boolean = true,
    val onboardingComplete: Boolean = false,
)