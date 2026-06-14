package com.pedilo.app.core.model

enum class LiveOrderType(val wireName: String) {
    LocalOrder("local_order"),
    DirectPurchase("direct_purchase"),
    PickupShipping("pickup_shipping"),
}

enum class LiveOrderOperationalStatus(val wireName: String) {
    WaitingAdminReview("waiting_admin_review"),
    LocalAccepted("local_accepted"),
    Preparing("preparing"),
    ReadyForPickup("ready_for_pickup"),
    DriverAssigned("driver_assigned"),
    PickedUp("picked_up"),
    Delivered("delivered"),
    AdminReviewed("admin_reviewed"),
    AdminIntervention("admin_intervention"),
    IncidentOpen("incident_open"),
    IncidentResolved("incident_resolved"),
}

enum class LiveOrderFinancialStatus(val wireName: String) {
    PendingReview("pending_review"),
    PendingPayment("pending_payment"),
    CollectOnDelivery("collect_on_delivery"),
    TransferDeclaredPending("transfer_declared_pending"),
    PaidDeclared("paid_declared"),
    ConfirmedInternal("confirmed_internal"),
    Rejected("rejected"),
    Disputed("disputed"),
    SettlementPending("settlement_pending"),
    Settled("settled"),
}

enum class LiveOrderCommunicationStatus(val wireName: String) {
    Received("received"),
    Closed("closed"),
}

enum class LiveOrderIncidentStatus(val wireName: String) {
    None("none"),
    Open("open"),
    Resolved("resolved"),
}

enum class LiveOrderArchiveStatus(val wireName: String) {
    Live("live"),
    Archived("archived"),
}

enum class LiveOrderAction(val wireName: String) {
    LocalAccept("local_accept"),
    LocalReject("local_reject"),
    LocalMarkPreparing("local_mark_preparing"),
    LocalMarkReady("local_mark_ready"),
    StoreDriverRequest("store_driver_request"),
    DriverTake("driver_take"),
    DriverMarkPickedUp("driver_mark_picked_up"),
    DriverMarkDelivered("driver_mark_delivered"),
    CancelOrder("cancel_order"),
    OpenIncident("open_incident"),
    ResolveIncident("resolve_incident"),
    AdminIntervene("admin_intervene"),
    ;

    companion object {
        fun fromWire(value: String): LiveOrderAction? =
            entries.firstOrNull { it.wireName == value.trim() }
    }
}

data class LiveOrderBirthState(
    val orderType: String,
    val operationalStatus: String,
    val financialStatus: String,
    val communicationStatus: String,
    val incidentStatus: String,
    val archiveStatus: String,
    val currentResponsibleRole: String,
    val assignedActorId: String,
    val version: Int,
    val idempotencyKey: String,
)

data class AdminLiveOrderActionRequest(
    val orderId: String,
    val action: LiveOrderAction,
    val expectedVersion: Int,
    val reason: String = "",
)

data class AdminLiveOrderActionResult(
    val orderId: String,
    val action: String,
    val publicStatus: String,
    val operationalStatus: String,
    val version: Int,
    val eventSummary: String,
    val humanMessage: String,
)

data class AdminOrderActionsRepairResult(
    val orderId: String,
    val version: Int,
    val nextAllowedActions: List<LiveOrderAction>,
    val eventSummary: String,
    val humanMessage: String,
)

data class AdminOrderEvent(
    val id: String,
    val type: String,
    val summary: String,
    val actorRole: String,
    val reason: String,
    val createdAtMillis: Long?,
)
