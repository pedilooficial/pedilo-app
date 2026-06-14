package com.pedilo.app.core.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.pedilo.app.core.model.AdminLiveOrderActionRequest
import com.pedilo.app.core.model.AdminLiveOrderActionResult
import com.pedilo.app.core.model.LiveOrderAction
import com.pedilo.app.core.model.PublicProductSummary
import com.pedilo.app.core.model.StoreOrderDetail
import com.pedilo.app.core.model.StoreOrderSummary
import com.pedilo.app.core.port.StoreOrdersPort
import com.pedilo.app.core.result.CoreError
import com.pedilo.app.core.result.CoreResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseStoreOrdersAdapter(
    private val auth: FirebaseAuth = Firebase.auth,
    private val db: FirebaseFirestore = Firebase.firestore,
    private val functions: FirebaseFunctions = Firebase.functions(REGION),
) : StoreOrdersPort {
    override fun observeOwnOrders(): Flow<CoreResult<List<StoreOrderSummary>>> =
        callbackFlow {
            val uid = auth.currentUser?.uid
            if (uid.isNullOrBlank()) {
                trySend(CoreResult.Failure(CoreError.NotAvailable))
                close()
                return@callbackFlow
            }
            val registration = db.collection(ORDERS)
                .whereEqualTo(STORE_ID, uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        trySend(CoreResult.Failure(CoreError.NotAvailable))
                        return@addSnapshotListener
                    }
                    trySend(CoreResult.Success(snapshot.documents.map { it.toStoreSummary() }))
                }
            awaitClose { registration.remove() }
        }

    override suspend fun getOwnOrderDetail(orderId: String): CoreResult<StoreOrderDetail> =
        runCatching {
            val uid = auth.currentUser?.uid ?: error("Missing store session")
            val doc = db.collection(ORDERS).document(orderId).get().await()
            if (!doc.exists() || doc.getString(STORE_ID).orEmpty() != uid) {
                error("Order does not belong to store")
            }
            doc.toStoreDetail()
        }.fold(
            onSuccess = { CoreResult.Success(it) },
            onFailure = { CoreResult.Failure(CoreError.NotAvailable) },
        )

    override suspend fun getOwnProducts(): CoreResult<List<PublicProductSummary>> =
        runCatching {
            val uid = auth.currentUser?.uid ?: error("Missing store session")
            db.collection(STORES)
                .document(uid)
                .collection(PRODUCTS)
                .get()
                .await()
                .documents
                .map { doc ->
                    PublicProductSummary(
                        id = doc.id,
                        storeId = uid,
                        name = doc.getString(NAME).orEmpty(),
                        description = doc.getString(DESCRIPTION).orEmpty(),
                        imageUrl = doc.getString(IMAGE_URL),
                        priceCents = (doc.get(PRICE_CENTS) as? Number)?.toLong(),
                        visible = doc.getBoolean(VISIBLE) ?: false,
                        available = doc.getBoolean(AVAILABLE) ?: false,
                    )
                }
                .filter { it.name.isNotBlank() }
                .sortedWith(compareByDescending<PublicProductSummary> { it.visible && it.available }.thenBy { it.name.lowercase() })
        }.fold(
            onSuccess = { CoreResult.Success(it) },
            onFailure = { CoreResult.Failure(CoreError.NotAvailable) },
        )

    override suspend fun executeStoreOrderAction(request: AdminLiveOrderActionRequest): CoreResult<AdminLiveOrderActionResult> =
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

    private fun com.google.firebase.firestore.DocumentSnapshot.toStoreSummary(): StoreOrderSummary =
        StoreOrderSummary(
            id = id,
            visibleNumber = visibleNumber(),
            publicStatus = getString(PUBLIC_STATUS).orEmpty(),
            operationalStatus = operationalStatus(),
            contactName = customerName(),
            itemsSummary = itemsSummary(),
            nextAllowedActions = nextAllowedActions().storeAllowedOnly(),
            version = version(),
            activeIncident = getBoolean(ACTIVE_INCIDENT) ?: false,
            communicationStatus = getString(COMMUNICATION_STATUS).orEmpty(),
            assistanceSummary = safeAssistanceSummary(),
            requiresHumanReview = getBoolean(AI_REQUIRES_HUMAN_REVIEW) ?: false,
        )

    private fun com.google.firebase.firestore.DocumentSnapshot.toStoreDetail(): StoreOrderDetail =
        StoreOrderDetail(
            id = id,
            visibleNumber = visibleNumber(),
            publicStatus = getString(PUBLIC_STATUS).orEmpty(),
            operationalStatus = operationalStatus(),
            contactName = customerName(),
            itemsSummary = itemsSummary(),
            total = get(TOTAL)?.toString().orEmpty(),
            paymentMethod = getString(PAYMENT_METHOD).orEmpty(),
            financialStatus = getString(FINANCIAL_STATUS).orEmpty(),
            amountToCollect = get(AMOUNT_TO_COLLECT)?.toString().orEmpty(),
            collectionRequired = getBoolean(COLLECTION_REQUIRED) ?: false,
            nextAllowedActions = nextAllowedActions().storeAllowedOnly(),
            version = version(),
            activeIncident = getBoolean(ACTIVE_INCIDENT) ?: false,
            communicationStatus = getString(COMMUNICATION_STATUS).orEmpty(),
            assistanceSummary = safeAssistanceSummary(),
            requiresHumanReview = getBoolean(AI_REQUIRES_HUMAN_REVIEW) ?: false,
        )

    private fun com.google.firebase.firestore.DocumentSnapshot.visibleNumber(): String =
        getString(TRACKING).orEmpty().ifBlank { getString(PUBLIC_NUMBER).orEmpty() }.ifBlank { id }

    private fun com.google.firebase.firestore.DocumentSnapshot.operationalStatus(): String =
        getString(OPERATIONAL_STATUS).orEmpty().ifBlank { getString(STATUS).orEmpty() }

    private fun com.google.firebase.firestore.DocumentSnapshot.customerName(): String =
        ((get(CUSTOMER) as? Map<*, *>)?.get(NAME) as? String).orEmpty()

    private fun com.google.firebase.firestore.DocumentSnapshot.itemsSummary(): List<String> =
        (get(ITEMS) as? List<*>)
            .orEmpty()
            .mapNotNull { it as? Map<*, *> }
            .map {
                "${it[ITEM_NAME].orEmptyText()} x${it[ITEM_QTY].orEmptyText()}"
            }

    private fun com.google.firebase.firestore.DocumentSnapshot.nextAllowedActions(): List<LiveOrderAction> =
        (get(NEXT_ALLOWED_ACTIONS) as? List<*>)
            .orEmpty()
            .mapNotNull { LiveOrderAction.fromWire(it.orEmptyText()) }

    private fun List<LiveOrderAction>.storeAllowedOnly(): List<LiveOrderAction> =
        filter {
            it in setOf(
                LiveOrderAction.LocalAccept,
                LiveOrderAction.LocalReject,
                LiveOrderAction.LocalMarkPreparing,
                LiveOrderAction.LocalMarkReady,
                LiveOrderAction.StoreDriverRequest,
                LiveOrderAction.CancelOrder,
                LiveOrderAction.OpenIncident,
            )
        }

    private fun com.google.firebase.firestore.DocumentSnapshot.version(): Int =
        (get(VERSION) as? Number)?.toInt() ?: 0

    private fun com.google.firebase.firestore.DocumentSnapshot.safeAssistanceSummary(): String =
        when (getString(AI_SUGGESTED_ACTION).orEmpty()) {
            "revisar incidencia" -> "Revisá la incidencia operativa del pedido."
            "revisar comunicación" -> "Revisá la comunicación preparada del pedido."
            "revisar datos del pedido" -> "Revisá los datos operativos propios del pedido."
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
        const val STORES = "stores"
        const val PRODUCTS = "products"
        const val OPERATE_LIVE_ORDER = "operateLiveOrder"
        const val STORE_ID = "storeId"
        const val TRACKING = "trackingNumber"
        const val PUBLIC_NUMBER = "publicOrderNumber"
        const val STATUS = "status"
        const val PUBLIC_STATUS = "publicStatus"
        const val OPERATIONAL_STATUS = "operationalStatus"
        const val NEXT_ALLOWED_ACTIONS = "nextAllowedActions"
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
        const val ITEMS = "items"
        const val ITEM_NAME = "name"
        const val ITEM_QTY = "quantity"
        const val CUSTOMER = "customer"
        const val NAME = "name"
        const val DESCRIPTION = "description"
        const val IMAGE_URL = "imageUrl"
        const val PRICE_CENTS = "priceCents"
        const val VISIBLE = "visible"
        const val AVAILABLE = "available"
    }
}
