package com.pedilo.app.core.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.pedilo.app.core.model.AdminLiveOrderActionRequest
import com.pedilo.app.core.model.AdminLiveOrderActionResult
import com.pedilo.app.core.model.DriverCashboxCloseResult
import com.pedilo.app.core.model.DriverOrderDetail
import com.pedilo.app.core.model.DriverOrderSummary
import com.pedilo.app.core.model.LiveOrderAction
import com.pedilo.app.core.port.DriverOrdersPort
import com.pedilo.app.core.result.CoreError
import com.pedilo.app.core.result.CoreResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseDriverOrdersAdapter(
    private val auth: FirebaseAuth = Firebase.auth,
    private val db: FirebaseFirestore = Firebase.firestore,
    private val functions: FirebaseFunctions = Firebase.functions(REGION),
) : DriverOrdersPort {
    override fun observeAvailableAndAssignedOrders(): Flow<CoreResult<List<DriverOrderSummary>>> =
        callbackFlow {
            val uid = auth.currentUser?.uid
            if (uid.isNullOrBlank()) {
                trySend(CoreResult.Failure(CoreError.NotAvailable))
                close()
                return@callbackFlow
            }

            var availableOrders: List<DriverOrderSummary> = emptyList()
            var assignedOrders: List<DriverOrderSummary> = emptyList()

            fun pushMerged() {
                val merged = (availableOrders + assignedOrders)
                    .associateBy { it.id }
                    .values
                    .sortedWith(
                        compareByDescending<DriverOrderSummary> { it.isAssignedToCurrentDriver }
                            .thenBy { it.visibleNumber },
                    )
                trySend(CoreResult.Success(merged))
            }

            fun onFailure() {
                trySend(CoreResult.Failure(CoreError.NotAvailable))
            }

            val registrations = mutableListOf<ListenerRegistration>()
            registrations += db.collection(ORDERS)
                .whereEqualTo(RESPONSIBLE_ROLE, DRIVER_ROLE)
                .whereEqualTo(ASSIGNED_ACTOR_ID, "")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        onFailure()
                        return@addSnapshotListener
                    }
                    availableOrders = snapshot.documents
                        .map { it.toDriverSummary(uid) }
                        .filter { it.isAvailableToCurrentDriver }
                    pushMerged()
                }
            registrations += db.collection(ORDERS)
                .whereEqualTo(DRIVER_ID, uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        onFailure()
                        return@addSnapshotListener
                    }
                    assignedOrders = snapshot.documents
                        .map { it.toDriverSummary(uid) }
                        .filter { it.isAssignedToCurrentDriver }
                    pushMerged()
                }

            awaitClose { registrations.forEach { it.remove() } }
        }

    override suspend fun getVisibleOrderDetail(orderId: String): CoreResult<DriverOrderDetail> =
        runCatching {
            val uid = auth.currentUser?.uid ?: error("Missing driver session")
            val doc = db.collection(ORDERS).document(orderId).get().await()
            if (!doc.exists() || !doc.isVisibleToDriver(uid)) {
                error("Order does not belong to driver visibility")
            }
            doc.toDriverDetail(uid)
        }.fold(
            onSuccess = { CoreResult.Success(it) },
            onFailure = { CoreResult.Failure(CoreError.NotAvailable) },
        )

    override suspend fun executeDriverOrderAction(request: AdminLiveOrderActionRequest): CoreResult<AdminLiveOrderActionResult> =
        runCatching {
            val result = functions
                .getHttpsCallable(OPERATE_LIVE_ORDER)
                .call(request.toCallablePayload())
                .await()

            @Suppress("UNCHECKED_CAST")
            val data = result.getData() as? Map<String, Any?>
                ?: error("Unexpected operateLiveOrder response")

            AdminLiveOrderActionResult(
                orderId = data["orderId"].asText(),
                action = data["action"].asText(),
                publicStatus = data["publicStatus"].asText(),
                operationalStatus = data["operationalStatus"].asText(),
                version = (data["version"] as? Number)?.toInt() ?: 0,
                eventSummary = data["eventSummary"].asText(),
                humanMessage = data["humanMessage"].asText(),
            )
        }.fold(
            onSuccess = { CoreResult.Success(it) },
            onFailure = { CoreResult.Failure(CoreError.Operational((it as? FirebaseFunctionsException)?.message ?: "No pudimos ejecutar la acción.")) },
        )

    override suspend fun closeCashbox(note: String): CoreResult<DriverCashboxCloseResult> =
        runCatching {
            val result = functions
                .getHttpsCallable(CLOSE_DRIVER_CASHBOX)
                .call(mapOf("note" to note))
                .await()

            @Suppress("UNCHECKED_CAST")
            val data = result.getData() as? Map<String, Any?>
                ?: error("Unexpected closeDriverCashbox response")

            DriverCashboxCloseResult(
                cashboxId = data["cashboxId"].asText(),
                ordersCount = (data["ordersCount"] as? Number)?.toInt() ?: 0,
                declaredAmountCents = (data["declaredAmountCents"] as? Number)?.toLong() ?: 0L,
                settlementStatus = data["settlementStatus"].asText(),
                humanMessage = data["humanMessage"].asText(),
            )
        }.fold(
            onSuccess = { CoreResult.Success(it) },
            onFailure = { CoreResult.Failure(CoreError.Operational((it as? FirebaseFunctionsException)?.message ?: "No pudimos cerrar la caja operativa.")) },
        )

    private fun com.google.firebase.firestore.DocumentSnapshot.toDriverSummary(uid: String): DriverOrderSummary =
        DriverOrderSummary(
            id = id,
            visibleNumber = visibleNumber(),
            orderType = getString(ORDER_TYPE).orEmpty(),
            publicStatus = getString(PUBLIC_STATUS).orEmpty(),
            operationalStatus = operationalStatus(),
            contactName = customerName(),
            storeLabel = storeLabel(),
            itemsSummary = itemsSummary(),
            nextAllowedActions = driverAllowedActions(uid),
            version = version(),
            activeIncident = getBoolean(ACTIVE_INCIDENT) ?: false,
            isAssignedToCurrentDriver = isAssignedToDriver(uid),
            communicationStatus = getString(COMMUNICATION_STATUS).orEmpty(),
            assistanceSummary = safeDriverAssistanceSummary(),
            requiresHumanReview = getBoolean(AI_REQUIRES_HUMAN_REVIEW) ?: false,
        )

    private fun com.google.firebase.firestore.DocumentSnapshot.toDriverDetail(uid: String): DriverOrderDetail =
        DriverOrderDetail(
            id = id,
            visibleNumber = visibleNumber(),
            orderType = getString(ORDER_TYPE).orEmpty(),
            publicStatus = getString(PUBLIC_STATUS).orEmpty(),
            operationalStatus = operationalStatus(),
            contactName = customerName(),
            contactPhone = customerPhone(),
            deliveryAddress = deliveryAddress(),
            storeLabel = storeLabel(),
            itemsSummary = itemsSummary(),
            total = get(TOTAL)?.toString().orEmpty(),
            paymentMethod = getString(PAYMENT_METHOD).orEmpty(),
            financialStatus = getString(FINANCIAL_STATUS).orEmpty(),
            amountToCollect = get(AMOUNT_TO_COLLECT)?.toString().orEmpty(),
            collectionRequired = getBoolean(COLLECTION_REQUIRED) ?: false,
            cashResponsibleRole = getString(CASH_RESPONSIBLE_ROLE).orEmpty(),
            nextAllowedActions = driverAllowedActions(uid),
            version = version(),
            activeIncident = getBoolean(ACTIVE_INCIDENT) ?: false,
            isAssignedToCurrentDriver = isAssignedToDriver(uid),
            communicationStatus = getString(COMMUNICATION_STATUS).orEmpty(),
            assistanceSummary = safeDriverAssistanceSummary(),
            requiresHumanReview = getBoolean(AI_REQUIRES_HUMAN_REVIEW) ?: false,
        )

    private fun com.google.firebase.firestore.DocumentSnapshot.isVisibleToDriver(uid: String): Boolean =
        isAssignedToDriver(uid) || isAvailableToDriver()

    private val DriverOrderSummary.isAvailableToCurrentDriver: Boolean
        get() = !isAssignedToCurrentDriver && nextAllowedActions.contains(LiveOrderAction.DriverTake)

    private fun com.google.firebase.firestore.DocumentSnapshot.isAvailableToDriver(): Boolean =
            getString(RESPONSIBLE_ROLE).orEmpty() == DRIVER_ROLE &&
                getString(CURRENT_RESPONSIBLE_ROLE).orEmpty().ifBlank { getString(RESPONSIBLE_ROLE).orEmpty() } == DRIVER_ROLE &&
                getString(ASSIGNED_ACTOR_ID).orEmpty().isBlank() &&
                getString(DRIVER_ID).orEmpty().isBlank() &&
                nextAllowedActions().contains(LiveOrderAction.DriverTake)

    private fun com.google.firebase.firestore.DocumentSnapshot.isAssignedToDriver(uid: String): Boolean =
        getString(DRIVER_ID).orEmpty() == uid || getString(ASSIGNED_ACTOR_ID).orEmpty() == uid

    private fun com.google.firebase.firestore.DocumentSnapshot.visibleNumber(): String =
        getString(TRACKING).orEmpty().ifBlank { getString(PUBLIC_NUMBER).orEmpty() }.ifBlank { id }

    private fun com.google.firebase.firestore.DocumentSnapshot.operationalStatus(): String =
        getString(OPERATIONAL_STATUS).orEmpty().ifBlank { getString(STATUS).orEmpty() }

    private fun com.google.firebase.firestore.DocumentSnapshot.customerName(): String =
        ((get(CUSTOMER) as? Map<*, *>)?.get(NAME) as? String).orEmpty()

    private fun com.google.firebase.firestore.DocumentSnapshot.customerPhone(): String =
        ((get(CUSTOMER) as? Map<*, *>)?.get(PHONE) as? String).orEmpty()

    private fun com.google.firebase.firestore.DocumentSnapshot.deliveryAddress(): String =
        buildList {
            val delivery = get(DELIVERY) as? Map<*, *>
            val addressLine = delivery?.get(ADDRESS_LINE)?.toString().orEmpty()
            val locality = delivery?.get(LOCALITY)?.toString().orEmpty()
            val customer = get(CUSTOMER) as? Map<*, *>
            val fallbackAddress = customer?.get(ADDRESS)?.toString().orEmpty()
            if (addressLine.isNotBlank()) {
                add(addressLine)
            } else if (fallbackAddress.isNotBlank()) {
                add(fallbackAddress)
            }
            if (locality.isNotBlank()) add(locality)
        }.joinToString(" · ")

    private fun com.google.firebase.firestore.DocumentSnapshot.storeLabel(): String =
        getString(STORE_NAME).orEmpty().ifBlank { getString(STORE_ID).orEmpty() }

    private fun com.google.firebase.firestore.DocumentSnapshot.itemsSummary(): List<String> =
        (get(ITEMS) as? List<*>)
            .orEmpty()
            .mapNotNull { it as? Map<*, *> }
            .map { "${it[ITEM_NAME].orEmptyText()} x${it[ITEM_QTY].orEmptyText()}" }

    private fun com.google.firebase.firestore.DocumentSnapshot.nextAllowedActions(): List<LiveOrderAction> =
        (get(NEXT_ALLOWED_ACTIONS) as? List<*>)
            .orEmpty()
            .mapNotNull { LiveOrderAction.fromWire(it.orEmptyText()) }

    private fun com.google.firebase.firestore.DocumentSnapshot.driverAllowedActions(uid: String): List<LiveOrderAction> {
        val actions = nextAllowedActions().driverAllowedOnly()
        return if (isAssignedToDriver(uid)) {
            actions
        } else {
            actions.filter { it == LiveOrderAction.DriverTake }
        }
    }

    private fun List<LiveOrderAction>.driverAllowedOnly(): List<LiveOrderAction> =
        filter {
            it in setOf(
                LiveOrderAction.DriverTake,
                LiveOrderAction.DriverMarkPickedUp,
                LiveOrderAction.DriverMarkDelivered,
                LiveOrderAction.OpenIncident,
                LiveOrderAction.CancelOrder,
            )
        }

    private fun com.google.firebase.firestore.DocumentSnapshot.version(): Int =
        (get(VERSION) as? Number)?.toInt() ?: 0

    private fun com.google.firebase.firestore.DocumentSnapshot.safeDriverAssistanceSummary(): String =
        when (getString(AI_SUGGESTED_ACTION).orEmpty()) {
            "revisar incidencia" -> "El pedido tiene revisión operativa activa."
            "revisar comunicación" -> "Revisá la comunicación operativa preparada."
            "revisar pago declarado" -> "Verificá instrucciones de cobro antes de cerrar."
            "intervención Admin sugerida" -> "El pedido requiere revisión operativa."
            else -> if (getBoolean(AI_REQUIRES_HUMAN_REVIEW) == true) "El pedido requiere revisión operativa." else ""
        }

    private fun AdminLiveOrderActionRequest.toCallablePayload(): Map<String, Any?> =
        mapOf(
            "orderId" to orderId,
            "action" to action.wireName,
            "expectedVersion" to expectedVersion,
            "reason" to reason,
        )

    private fun Any?.asText(): String = this as? String ?: ""
    private fun Any?.orEmptyText(): String = this?.toString().orEmpty()

    private companion object {
        const val REGION = "southamerica-east1"
        const val ORDERS = "orders"
        const val OPERATE_LIVE_ORDER = "operateLiveOrder"
        const val CLOSE_DRIVER_CASHBOX = "closeDriverCashbox"
        const val DRIVER_ROLE = "driver"
        const val DRIVER_ID = "driverId"
        const val ASSIGNED_ACTOR_ID = "assignedActorId"
        const val RESPONSIBLE_ROLE = "responsibleRole"
        const val TRACKING = "trackingNumber"
        const val PUBLIC_NUMBER = "publicOrderNumber"
        const val STATUS = "status"
        const val ORDER_TYPE = "orderType"
        const val PUBLIC_STATUS = "publicStatus"
        const val OPERATIONAL_STATUS = "operationalStatus"
        const val NEXT_ALLOWED_ACTIONS = "nextAllowedActions"
        const val CURRENT_RESPONSIBLE_ROLE = "currentResponsibleRole"
        const val ACTIVE_INCIDENT = "activeIncident"
        const val VERSION = "version"
        const val TOTAL = "total"
        const val PAYMENT_METHOD = "paymentMethod"
        const val FINANCIAL_STATUS = "financialStatus"
        const val COMMUNICATION_STATUS = "communicationStatus"
        const val AI_SUGGESTED_ACTION = "aiSuggestedAction"
        const val AI_REQUIRES_HUMAN_REVIEW = "aiRequiresHumanReview"
        const val AMOUNT_TO_COLLECT = "amountToCollect"
        const val COLLECTION_REQUIRED = "collectionRequired"
        const val CASH_RESPONSIBLE_ROLE = "cashResponsibleRole"
        const val ITEMS = "items"
        const val ITEM_NAME = "name"
        const val ITEM_QTY = "quantity"
        const val CUSTOMER = "customer"
        const val NAME = "name"
        const val PHONE = "phone"
        const val ADDRESS = "address"
        const val DELIVERY = "delivery"
        const val ADDRESS_LINE = "addressLine"
        const val LOCALITY = "locality"
        const val STORE_ID = "storeId"
        const val STORE_NAME = "storeName"
    }
}
