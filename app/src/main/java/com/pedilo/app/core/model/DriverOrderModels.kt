package com.pedilo.app.core.model

data class DriverOrderSummary(
    val id: String,
    val visibleNumber: String,
    val orderType: String,
    val publicStatus: String,
    val operationalStatus: String,
    val contactName: String,
    val storeLabel: String,
    val itemsSummary: List<String>,
    val nextAllowedActions: List<LiveOrderAction>,
    val version: Int,
    val activeIncident: Boolean,
    val isAssignedToCurrentDriver: Boolean,
    val communicationStatus: String = "",
    val assistanceSummary: String = "",
    val requiresHumanReview: Boolean = false,
)

data class DriverOrderDetail(
    val id: String,
    val visibleNumber: String,
    val orderType: String,
    val publicStatus: String,
    val operationalStatus: String,
    val contactName: String,
    val contactPhone: String,
    val deliveryAddress: String,
    val storeLabel: String,
    val itemsSummary: List<String>,
    val total: String,
    val paymentMethod: String,
    val financialStatus: String,
    val amountToCollect: String,
    val collectionRequired: Boolean,
    val cashResponsibleRole: String,
    val nextAllowedActions: List<LiveOrderAction>,
    val version: Int,
    val activeIncident: Boolean,
    val isAssignedToCurrentDriver: Boolean,
    val communicationStatus: String = "",
    val assistanceSummary: String = "",
    val requiresHumanReview: Boolean = false,
)

data class DriverCashboxCloseResult(
    val cashboxId: String,
    val ordersCount: Int,
    val declaredAmountCents: Long,
    val settlementStatus: String,
    val humanMessage: String,
)
