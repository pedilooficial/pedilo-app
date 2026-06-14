package com.pedilo.app.core.model

/**
 * Clasificación operativa read-only para Admin → Operación.
 * No escribe pedidos ni inventa estados sin señal en los datos.
 */
enum class AdminTodayOrdersBucket {
    ACTIVE,
    FINISHED,
    CANCELLED,
    WITH_PROBLEMS,
    REVIEW,
}

enum class AdminOrderPrimaryPlacement {
    PROBLEM,
    ACTIVE,
    FINISHED,
    CANCELLED,
    UNCLASSIFIED,
}

enum class AdminActiveOrdersBucket {
    WAITING_STORE,
    PREPARING,
    WAITING_DRIVER,
    IN_DELIVERY,
    REVIEW_STATE,
}

enum class AdminProblemOrdersBucket {
    STORE_NOT_RESPONDING,
    CUSTOMER_CLAIM,
    DELAYED,
    WITHOUT_RESPONSIBLE,
    OPERATIONAL_REVIEW,
}

data class AdminOperationOrderSignals(
    val status: String,
    val publicStatus: String,
    val operationalStatus: String,
    val responsibleRole: String,
    val needsAttention: Boolean,
    val activeIncident: Boolean,
    val source: String,
    val requestType: String,
) {
    companion object {
        fun from(summary: AdminOrderSummary): AdminOperationOrderSignals =
            AdminOperationOrderSignals(
                status = summary.status,
                publicStatus = summary.publicStatus,
                operationalStatus = summary.operationalStatus,
                responsibleRole = summary.responsibleRole,
                needsAttention = summary.needsAttention,
                activeIncident = summary.activeIncident,
                source = summary.source,
                requestType = summary.requestType,
            )
    }
}

object AdminOperationOrderClassification {
    const val STATUS_CREATED = "created"
    const val STATUS_CANCELLED = "cancelled"
    const val STATUS_CANCELED = "canceled"
    const val PUBLIC_STATUS_RECEIVED = "Pedido recibido"

    const val SOURCE_PUBLIC_LOCAL = "public_local"
    const val SOURCE_PUBLIC_PLUS_BUY = "public_plus_buy"
    const val SOURCE_PUBLIC_PLUS_PICKUP_SHIPPING = "public_plus_pickup_shipping"
    const val SOURCE_PUBLIC_APP = "public_app"

    const val IDENTITY_PLUS_BUY = "Compra solicitada"
    const val IDENTITY_PLUS_PICKUP = "Retiro solicitado"
    const val IDENTITY_LOCAL_PICKUP = "Retiro de local"
    const val IDENTITY_OPERATIONAL_ORDER = "Pedido"

    const val FUNCTION_BUY_AND_DELIVER = "Comprar y entregar"
    const val FUNCTION_PICKUP_AND_DELIVER = "Retirar y entregar"
    const val FUNCTION_REVIEW_ORDER = "Revisar pedido"

    fun primaryPlacement(signals: AdminOperationOrderSignals): AdminOrderPrimaryPlacement =
        when {
            hasRealProblemSignal(signals) -> AdminOrderPrimaryPlacement.PROBLEM
            hasRealCancellationSignal(signals) -> AdminOrderPrimaryPlacement.CANCELLED
            hasRealFinishedSignal(signals) -> AdminOrderPrimaryPlacement.FINISHED
            hasNormalActiveSignal(signals) -> AdminOrderPrimaryPlacement.ACTIVE
            else -> AdminOrderPrimaryPlacement.UNCLASSIFIED
        }

    fun todayBucket(signals: AdminOperationOrderSignals): AdminTodayOrdersBucket? {
        return when (primaryPlacement(signals)) {
            AdminOrderPrimaryPlacement.PROBLEM -> AdminTodayOrdersBucket.WITH_PROBLEMS
            AdminOrderPrimaryPlacement.CANCELLED -> AdminTodayOrdersBucket.CANCELLED
            AdminOrderPrimaryPlacement.FINISHED -> AdminTodayOrdersBucket.FINISHED
            AdminOrderPrimaryPlacement.ACTIVE -> AdminTodayOrdersBucket.ACTIVE
            AdminOrderPrimaryPlacement.UNCLASSIFIED -> AdminTodayOrdersBucket.REVIEW
        }
    }

    fun activeBucket(signals: AdminOperationOrderSignals): AdminActiveOrdersBucket? {
        if (!hasNormalActiveSignal(signals)) return null
        if (hasRealPreparingSignal(signals)) return AdminActiveOrdersBucket.PREPARING
        if (hasRealWaitingDriverSignal(signals)) return AdminActiveOrdersBucket.WAITING_DRIVER
        if (hasRealInDeliverySignal(signals)) return AdminActiveOrdersBucket.IN_DELIVERY
        if (hasRealWaitingStoreSignal(signals)) return AdminActiveOrdersBucket.WAITING_STORE
        return AdminActiveOrdersBucket.REVIEW_STATE
    }

    fun problemBucket(signals: AdminOperationOrderSignals): AdminProblemOrdersBucket? {
        if (!hasRealProblemSignal(signals)) return null
        if (hasRealStoreNotRespondingSignal(signals)) return AdminProblemOrdersBucket.STORE_NOT_RESPONDING
        if (hasRealCustomerClaimSignal(signals)) return AdminProblemOrdersBucket.CUSTOMER_CLAIM
        if (hasRealDelaySignal(signals)) return AdminProblemOrdersBucket.DELAYED
        if (hasRealWithoutResponsibleSignal(signals)) return AdminProblemOrdersBucket.WITHOUT_RESPONSIBLE
        return AdminProblemOrdersBucket.OPERATIONAL_REVIEW
    }

    fun operationalIdentity(source: String, requestType: String = ""): String =
        when {
            source.trim() == SOURCE_PUBLIC_LOCAL -> IDENTITY_LOCAL_PICKUP
            source.trim() == SOURCE_PUBLIC_PLUS_BUY || requestType.trim() == "buy" -> IDENTITY_PLUS_BUY
            source.trim() == SOURCE_PUBLIC_PLUS_PICKUP_SHIPPING ||
                requestType.trim() == "pickup_shipping" -> IDENTITY_PLUS_PICKUP
            else -> IDENTITY_OPERATIONAL_ORDER
        }

    fun operationalFunction(source: String, requestType: String = ""): String =
        when (operationalIdentity(source, requestType)) {
            IDENTITY_PLUS_BUY -> FUNCTION_BUY_AND_DELIVER
            IDENTITY_PLUS_PICKUP,
            IDENTITY_LOCAL_PICKUP -> FUNCTION_PICKUP_AND_DELIVER
            else -> FUNCTION_REVIEW_ORDER
        }

    fun hasRealActiveSignal(signals: AdminOperationOrderSignals): Boolean =
        normalizedStatus(signals.status) == STATUS_CREATED &&
            signals.publicStatus.trim() == PUBLIC_STATUS_RECEIVED

    fun hasRealCancellationSignal(signals: AdminOperationOrderSignals): Boolean =
        normalizedStatus(signals.status) in setOf(STATUS_CANCELLED, STATUS_CANCELED)

    fun hasRealFinishedSignal(signals: AdminOperationOrderSignals): Boolean =
        normalizedStatus(signals.status) in setOf("delivered", "closed", "archived")

    fun hasRealDelaySignal(signals: AdminOperationOrderSignals): Boolean =
        signals.publicStatus.contains("demora", ignoreCase = true) ||
            signals.publicStatus.contains("retras", ignoreCase = true) ||
            signals.operationalStatus.contains("demora", ignoreCase = true) ||
            signals.operationalStatus.contains("retras", ignoreCase = true)

    fun hasRealProblemSignal(signals: AdminOperationOrderSignals): Boolean =
        signals.activeIncident ||
            signals.needsAttention ||
            hasRealStoreNotRespondingSignal(signals) ||
            hasRealCustomerClaimSignal(signals) ||
            hasRealDelaySignal(signals) ||
            hasRealWithoutResponsibleSignal(signals)

    fun hasNormalActiveSignal(signals: AdminOperationOrderSignals): Boolean =
        hasRealActiveSignal(signals) && !hasRealProblemSignal(signals)

    fun hasRealWaitingStoreSignal(signals: AdminOperationOrderSignals): Boolean =
        signals.operationalStatus.contains("esperando local", ignoreCase = true) ||
            signals.operationalStatus.contains("waiting_store", ignoreCase = true)

    fun hasRealPreparingSignal(signals: AdminOperationOrderSignals): Boolean =
        signals.publicStatus.contains("preparando", ignoreCase = true) ||
            signals.operationalStatus.contains("preparando", ignoreCase = true) ||
            signals.operationalStatus.contains("preparing", ignoreCase = true)

    fun hasRealWaitingDriverSignal(signals: AdminOperationOrderSignals): Boolean =
        signals.publicStatus.contains("esperando repartidor", ignoreCase = true) ||
            signals.operationalStatus.contains("esperando repartidor", ignoreCase = true) ||
            signals.operationalStatus.contains("waiting_driver", ignoreCase = true)

    fun hasRealInDeliverySignal(signals: AdminOperationOrderSignals): Boolean =
        signals.publicStatus.contains("en entrega", ignoreCase = true) ||
            signals.operationalStatus.contains("en entrega", ignoreCase = true) ||
            signals.operationalStatus.contains("in_delivery", ignoreCase = true)

    fun hasRealStoreNotRespondingSignal(signals: AdminOperationOrderSignals): Boolean =
        signals.publicStatus.contains("local no responde", ignoreCase = true) ||
            signals.operationalStatus.contains("local no responde", ignoreCase = true) ||
            signals.operationalStatus.contains("sin respuesta", ignoreCase = true)

    fun hasRealCustomerClaimSignal(signals: AdminOperationOrderSignals): Boolean =
        signals.publicStatus.contains("reclamo", ignoreCase = true) ||
            signals.publicStatus.contains("problema", ignoreCase = true)

    fun hasRealWithoutResponsibleSignal(signals: AdminOperationOrderSignals): Boolean =
        hasRealActiveSignal(signals) && signals.responsibleRole.isBlank()

    private fun normalizedStatus(status: String): String = status.trim().lowercase()
}
