package com.pedilo.app.core.model

data class PublicOrderItem(
    val productId: String = "",
    val storeId: String = "",
    val name: String,
    val quantity: Int = 1,
    val unitPriceCents: Long? = null,
    val notes: String = "",
)
