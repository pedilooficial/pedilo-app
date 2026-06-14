package com.pedilo.app.core.model

data class PublicPlusOrderDraft(
    val source: String,
    val requestType: PublicPlusOrderType,
    val contact: CustomerContact,
    val items: List<PublicPlusOrderItem> = emptyList(),
    val sourceReference: String = "",
    val destination: String = "",
    val note: String = "",
    val paymentMethod: String = "",
    val amount: String = "",
    val schedule: String = "",
)

enum class PublicPlusOrderType {
    BUY,
    PICKUP_SHIPPING,
}

data class PublicPlusOrderItem(
    val name: String,
    val detail: String = "",
)
