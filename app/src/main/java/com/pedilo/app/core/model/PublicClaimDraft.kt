package com.pedilo.app.core.model

data class PublicClaimDraft(
    val trackingNumber: String,
    val customerName: String,
    val contact: String,
    val reason: String,
    val description: String,
    val type: String = "other",
)
