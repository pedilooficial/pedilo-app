package com.pedilo.app.core.model

data class PublicStoreSummary(
    val id: String,
    val name: String,
    val category: String,
    val description: String = "",
    val imageUrl: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val openingHours: String? = null,
    val visible: Boolean = true,
    val isOpen: Boolean,
)
