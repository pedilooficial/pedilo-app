package com.pedilo.app.core.model

data class PublicOrderTicket(
    val orderId: String = "",
    val trackingNumber: String,
    val status: PublicOrderStatus,
    val publicStatus: String = "Pedido recibido",
    val storeName: String = "",
)
