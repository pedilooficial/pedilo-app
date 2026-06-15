package com.pedilo.app.core.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.pedilo.app.core.model.AdminConfigState
import com.pedilo.app.core.model.AdminConfigUpdateRequest
import com.pedilo.app.core.model.AdminMutationResult
import com.pedilo.app.core.model.AdminLiveOrderActionRequest
import com.pedilo.app.core.model.AdminLiveOrderActionResult
import com.pedilo.app.core.model.AdminAuditSummary
import com.pedilo.app.core.model.AdminCriticalEvent
import com.pedilo.app.core.model.AdminHealthAlert
import com.pedilo.app.core.model.AdminModuleHealth
import com.pedilo.app.core.model.AdminOrderActionsRepairResult
import com.pedilo.app.core.model.AdminOrderEvent
import com.pedilo.app.core.model.AdminOrderDetail
import com.pedilo.app.core.model.AdminOrderSummary
import com.pedilo.app.core.model.AdminOperationalHealthMetrics
import com.pedilo.app.core.model.AdminOperationalHealthReport
import com.pedilo.app.core.model.AdminRoleUpdateRequest
import com.pedilo.app.core.model.AdminTeamUser
import com.pedilo.app.core.model.LiveOrderAction
import com.pedilo.app.core.port.AdminOrdersPort
import com.pedilo.app.core.result.CoreError
import com.pedilo.app.core.result.CoreResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirebaseAdminOrdersAdapter(
    private val db: FirebaseFirestore = Firebase.firestore,
    private val functions: FirebaseFunctions = Firebase.functions(REGION),
) : AdminOrdersPort {
    override suspend fun getOrdersReadOnly(): CoreResult<List<AdminOrderSummary>> =
        runCatching {
            db.collection(ORDERS).get().await().documents.map { doc -> doc.toSummary() }
        }.fold(
            onSuccess = { CoreResult.Success(it) },
            onFailure = { CoreResult.Failure(CoreError.NotAvailable) },
        )

    override fun observeOrdersReadOnly(): Flow<CoreResult<List<AdminOrderSummary>>> =
        callbackFlow {
            val registration = db.collection(ORDERS).addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(CoreResult.Failure(CoreError.NotAvailable))
                    return@addSnapshotListener
                }
                trySend(CoreResult.Success(snapshot.documents.map { it.toSummary() }))
            }
            awaitClose { registration.remove() }
        }

    override suspend fun getOrderDetailReadOnly(orderId: String): CoreResult<AdminOrderDetail> =
        runCatching {
            val doc = db.collection(ORDERS).document(orderId).get().await()
            val itemsSummary = (doc.get(ITEMS) as? List<Map<String, Any?>>).orEmpty().map {
                "${it[ITEM_NAME].orEmptyText()} x${it[ITEM_QTY].orEmptyText()}"
            }
            val customer = doc.get(CUSTOMER) as? Map<String, Any?>
            val lastEvent = (doc.get(LAST_OPERATION_EVENT) as? Map<String, Any?>).orEmpty()
            val events = getOrderEventsReadOnly(orderId).let {
                when (it) {
                    is CoreResult.Success -> it.value
                    is CoreResult.Failure -> emptyList()
                }
            }
            AdminOrderDetail(
                id = doc.id,
                trackingNumber = doc.getString(TRACKING).orEmpty(),
                publicOrderNumber = doc.getString(PUBLIC_NUMBER).orEmpty(),
                status = doc.getString(STATUS).orEmpty(),
                publicStatus = doc.getString(PUBLIC_STATUS).orEmpty(),
                operationalStatus = doc.operationalStatus(),
                responsibleRole = doc.getString(RESPONSIBLE_ROLE).orEmpty(),
                priority = doc.priority(),
                needsAttention = doc.getBoolean(NEEDS_ATTENTION) ?: false,
                activeIncident = doc.getBoolean(ACTIVE_INCIDENT) ?: false,
                nextAllowedActions = doc.nextAllowedActions(),
                source = doc.getString(SOURCE).orEmpty(),
                requestType = doc.getString(REQUEST_TYPE).orEmpty(),
                storeName = doc.getString(STORE_NAME).orEmpty(),
                customerName = customer?.get(NAME).orEmptyText(),
                customerPhone = customer?.get(PHONE).orEmptyText(),
                customerAddress = customer?.get(ADDRESS).orEmptyText(),
                createdAtMillis = (doc.get(CREATED_AT) as? Timestamp)?.toDate()?.time,
                updatedAtMillis = (doc.get(UPDATED_AT) as? Timestamp)?.toDate()?.time,
                total = doc.get(TOTAL)?.toString().orEmpty(),
                paymentMethod = doc.getString(PAYMENT_METHOD).orEmpty(),
                amountToCollect = doc.get(AMOUNT_TO_COLLECT)?.toString().orEmpty(),
                collectedAmount = doc.get(COLLECTED_AMOUNT)?.toString().orEmpty(),
                collectionRequired = doc.getBoolean(COLLECTION_REQUIRED) ?: false,
                cashResponsibleRole = doc.getString(CASH_RESPONSIBLE_ROLE).orEmpty(),
                financialNotes = doc.getString(FINANCIAL_NOTES).orEmpty(),
                itemsSummary = itemsSummary,
                lastEventSummary = lastEvent[EVENT_SUMMARY].orEmptyText(),
                orderType = doc.getString(ORDER_TYPE).orEmpty(),
                financialStatus = doc.getString(FINANCIAL_STATUS).orEmpty(),
                communicationStatus = doc.getString(COMMUNICATION_STATUS).orEmpty(),
                aiRiskLevel = doc.getString(AI_RISK_LEVEL).orEmpty(),
                aiClassification = doc.getString(AI_CLASSIFICATION).orEmpty(),
                aiSuggestedAction = doc.getString(AI_SUGGESTED_ACTION).orEmpty(),
                aiRequiresHumanReview = doc.getBoolean(AI_REQUIRES_HUMAN_REVIEW) ?: false,
                incidentStatus = doc.getString(INCIDENT_STATUS).orEmpty(),
                archiveStatus = doc.getString(ARCHIVE_STATUS).orEmpty(),
                currentResponsibleRole = doc.getString(CURRENT_RESPONSIBLE_ROLE).orEmpty(),
                assignedActorId = doc.getString(ASSIGNED_ACTOR_ID).orEmpty(),
                assignedActorRole = doc.getString(ASSIGNED_ACTOR_ROLE).orEmpty(),
                version = doc.version(),
                idempotencyKey = doc.getString(IDEMPOTENCY_KEY).orEmpty(),
                events = events,
            )
        }.fold(
            onSuccess = { CoreResult.Success(it) },
            onFailure = { CoreResult.Failure(CoreError.NotAvailable) },
        )

    override suspend fun executeLiveOrderAction(request: AdminLiveOrderActionRequest): CoreResult<AdminLiveOrderActionResult> =
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

    override suspend fun recalculateOrderActions(orderId: String): CoreResult<AdminOrderActionsRepairResult> =
        runCatching {
            val result = functions
                .getHttpsCallable(ADMIN_RECALCULATE_ORDER_ACTIONS)
                .call(mapOf("orderId" to orderId))
                .await()

            @Suppress("UNCHECKED_CAST")
            val data = result.getData() as? Map<String, Any?>
                ?: error("Unexpected adminRecalculateOrderActions response")
            val actionWires = data["nextAllowedActions"] as? List<*> ?: emptyList<Any>()

            AdminOrderActionsRepairResult(
                orderId = data["orderId"].asText(),
                version = (data["version"] as? Number)?.toInt() ?: 0,
                nextAllowedActions = actionWires.mapNotNull { LiveOrderAction.fromWire(it.orEmptyText()) },
                eventSummary = data["eventSummary"].asText(),
                humanMessage = data["humanMessage"].asText(),
            )
        }.fold(
            onSuccess = { CoreResult.Success(it) },
            onFailure = { CoreResult.Failure(CoreError.Operational((it as? FirebaseFunctionsException)?.message ?: "No pudimos actualizar acciones permitidas.")) },
        )

    override suspend fun getOrderEventsReadOnly(orderId: String): CoreResult<List<AdminOrderEvent>> =
        runCatching {
            db.collection(ORDERS)
                .document(orderId)
                .collection(EVENTS)
                .orderBy(CREATED_AT, Query.Direction.DESCENDING)
                .limit(8)
                .get()
                .await()
                .documents
                .map { doc ->
                    AdminOrderEvent(
                        id = doc.id,
                        type = doc.getString(EVENT_TYPE).orEmpty(),
                        summary = doc.getString(EVENT_SUMMARY).orEmpty(),
                        actorRole = doc.getString(ACTOR_ROLE).orEmpty(),
                        reason = doc.getString(REASON).orEmpty(),
                        createdAtMillis = (doc.get(CREATED_AT) as? Timestamp)?.toDate()?.time,
                    )
                }
        }.fold(
            onSuccess = { CoreResult.Success(it) },
            onFailure = { CoreResult.Failure(CoreError.NotAvailable) },
        )

    override suspend fun getOperationalHealth(): CoreResult<AdminOperationalHealthReport> =
        runCatching {
            val result = functions.getHttpsCallable(GET_OPERATIONAL_HEALTH).call(emptyMap<String, Any>()).await()
            @Suppress("UNCHECKED_CAST")
            (result.getData() as? Map<String, Any?>).orEmpty().toOperationalHealthReport()
        }.fold(
            onSuccess = { CoreResult.Success(it) },
            onFailure = { CoreResult.Failure(CoreError.NotAvailable) },
        )



    override fun observeTeamUsers(): Flow<CoreResult<List<AdminTeamUser>>> =
        callbackFlow {
            val registration = db.collection(USERS).addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(CoreResult.Failure(CoreError.NotAvailable))
                    return@addSnapshotListener
                }
                trySend(CoreResult.Success(snapshot.documents.map { it.toTeamUser() }.sortedWith(compareBy({ !it.active }, { it.role }, { it.displayName.ifBlank { it.email } }))))
            }
            awaitClose { registration.remove() }
        }

    override fun observeAdminConfig(): Flow<CoreResult<AdminConfigState>> =
        callbackFlow {
            val registration = db.collection(ADMIN_CONFIG).document(ADMIN_CONFIG_REAL_USE)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        launch { trySend(readAdminConfigThroughCallable()) }
                        return@addSnapshotListener
                    }
                    if (snapshot == null) {
                        launch { trySend(readAdminConfigThroughCallable()) }
                        return@addSnapshotListener
                    }
                    if (snapshot.exists()) {
                        trySend(CoreResult.Success(snapshot.toAdminConfig()))
                    } else {
                        launch { trySend(readAdminConfigThroughCallable()) }
                    }
                }
            awaitClose { registration.remove() }
        }

    override suspend fun updateTeamUser(request: AdminRoleUpdateRequest): CoreResult<AdminMutationResult> =
        runCatching {
            val result = functions.getHttpsCallable(ADMIN_UPDATE_TEAM_USER).call(request.toCallablePayload()).await()
            @Suppress("UNCHECKED_CAST")
            val data = result.getData() as? Map<String, Any?> ?: emptyMap()
            AdminMutationResult(data["message"].asText().ifBlank { "Acceso actualizado y persistido." })
        }.fold(
            onSuccess = { CoreResult.Success(it) },
            onFailure = { CoreResult.Failure(CoreError.Operational((it as? FirebaseFunctionsException)?.message ?: "No pudimos actualizar el acceso.")) },
        )

    override suspend fun updateAdminConfig(request: AdminConfigUpdateRequest): CoreResult<AdminMutationResult> =
        runCatching {
            val result = functions.getHttpsCallable(ADMIN_UPDATE_CONFIG).call(request.toCallablePayload()).await()
            @Suppress("UNCHECKED_CAST")
            val data = result.getData() as? Map<String, Any?> ?: emptyMap()
            AdminMutationResult(data["message"].asText().ifBlank { "Configuración actualizada y persistida." })
        }.fold(
            onSuccess = { CoreResult.Success(it) },
            onFailure = { CoreResult.Failure(CoreError.Operational((it as? FirebaseFunctionsException)?.message ?: "No pudimos actualizar la configuración.")) },
        )

    private fun com.google.firebase.firestore.DocumentSnapshot.toSummary(): AdminOrderSummary =
        AdminOrderSummary(
            id = id,
            trackingNumber = getString(TRACKING).orEmpty(),
            publicOrderNumber = getString(PUBLIC_NUMBER).orEmpty(),
            status = getString(STATUS).orEmpty(),
            publicStatus = getString(PUBLIC_STATUS).orEmpty(),
            operationalStatus = operationalStatus(),
            responsibleRole = getString(RESPONSIBLE_ROLE).orEmpty(),
            priority = priority(),
            needsAttention = getBoolean(NEEDS_ATTENTION) ?: false,
            activeIncident = getBoolean(ACTIVE_INCIDENT) ?: false,
            nextAllowedActions = nextAllowedActions(),
            source = getString(SOURCE).orEmpty(),
            requestType = getString(REQUEST_TYPE).orEmpty(),
            storeName = getString(STORE_NAME).orEmpty(),
            createdAtMillis = (get(CREATED_AT) as? Timestamp)?.toDate()?.time,
            total = get(TOTAL)?.toString().orEmpty(),
            paymentMethod = getString(PAYMENT_METHOD).orEmpty(),
            amountToCollect = get(AMOUNT_TO_COLLECT)?.toString().orEmpty(),
            collectedAmount = get(COLLECTED_AMOUNT)?.toString().orEmpty(),
            collectionRequired = getBoolean(COLLECTION_REQUIRED) ?: false,
            cashResponsibleRole = getString(CASH_RESPONSIBLE_ROLE).orEmpty(),
            orderType = getString(ORDER_TYPE).orEmpty(),
            financialStatus = getString(FINANCIAL_STATUS).orEmpty(),
            communicationStatus = getString(COMMUNICATION_STATUS).orEmpty(),
            aiRiskLevel = getString(AI_RISK_LEVEL).orEmpty(),
            aiClassification = getString(AI_CLASSIFICATION).orEmpty(),
            aiSuggestedAction = getString(AI_SUGGESTED_ACTION).orEmpty(),
            aiRequiresHumanReview = getBoolean(AI_REQUIRES_HUMAN_REVIEW) ?: false,
            incidentStatus = getString(INCIDENT_STATUS).orEmpty(),
            archiveStatus = getString(ARCHIVE_STATUS).orEmpty(),
            currentResponsibleRole = getString(CURRENT_RESPONSIBLE_ROLE).orEmpty(),
            assignedActorId = getString(ASSIGNED_ACTOR_ID).orEmpty(),
            assignedActorRole = getString(ASSIGNED_ACTOR_ROLE).orEmpty(),
            version = version(),
            idempotencyKey = getString(IDEMPOTENCY_KEY).orEmpty(),
        )



    private fun com.google.firebase.firestore.DocumentSnapshot.toTeamUser(): AdminTeamUser =
        AdminTeamUser(
            uid = id,
            email = getString(EMAIL).orEmpty(),
            displayName = getString(DISPLAY_NAME).orEmpty(),
            role = getString(ROLE).orEmpty(),
            active = getBoolean(ACTIVE) ?: false,
            storeId = getString(STORE_ID).orEmpty(),
            driverId = getString(DRIVER_ID).orEmpty(),
            updatedAtMillis = (get(UPDATED_AT) as? Timestamp)?.toDate()?.time,
        )

    private fun com.google.firebase.firestore.DocumentSnapshot.toAdminConfig(): AdminConfigState =
        AdminConfigState(
            id = id,
            rainMode = getBoolean(RAIN_MODE) ?: false,
            rainDeliveryFee = (get(RAIN_DELIVERY_FEE) as? Number)?.toInt() ?: DEFAULT_RAIN_DELIVERY_FEE,
            baseDeliveryFee = (get(BASE_DELIVERY_FEE) as? Number)?.toInt() ?: DEFAULT_BASE_DELIVERY_FEE,
            distanceSurcharge = (get(DISTANCE_SURCHARGE) as? Number)?.toInt() ?: DEFAULT_DISTANCE_SURCHARGE,
            lastUpdatedBy = getString(LAST_UPDATED_BY).orEmpty().ifBlank { getString(UPDATED_BY).orEmpty() },
            updatedAtMillis = (get(UPDATED_AT) as? Timestamp)?.toDate()?.time,
        )

    private suspend fun readAdminConfigThroughCallable(): CoreResult<AdminConfigState> =
        runCatching {
            val result = functions.getHttpsCallable(GET_ADMIN_CONFIG).call(emptyMap<String, Any>()).await()
            @Suppress("UNCHECKED_CAST")
            val data = result.getData() as? Map<String, Any?> ?: emptyMap()
            data.toAdminConfigState()
        }.fold(
            onSuccess = { CoreResult.Success(it) },
            onFailure = { CoreResult.Failure(CoreError.NotAvailable) },
        )

    private fun Map<String, Any?>.toAdminConfigState(): AdminConfigState =
        AdminConfigState(
            id = this["id"].asText().ifBlank { ADMIN_CONFIG_REAL_USE },
            rainMode = this[RAIN_MODE].asBoolValue(),
            rainDeliveryFee = this[RAIN_DELIVERY_FEE].asIntValue(DEFAULT_RAIN_DELIVERY_FEE),
            baseDeliveryFee = this[BASE_DELIVERY_FEE].asIntValue(DEFAULT_BASE_DELIVERY_FEE),
            distanceSurcharge = this[DISTANCE_SURCHARGE].asIntValue(DEFAULT_DISTANCE_SURCHARGE),
            lastUpdatedBy = this[LAST_UPDATED_BY].asText().ifBlank { this[UPDATED_BY].asText() },
        )

    private fun com.google.firebase.firestore.DocumentSnapshot.operationalStatus(): String =
        getString(OPERATIONAL_STATUS).orEmpty().ifBlank { getString(STATUS).orEmpty() }

    private fun com.google.firebase.firestore.DocumentSnapshot.priority(): String =
        getString(PRIORITY).orEmpty().ifBlank {
            if (getBoolean(ACTIVE_INCIDENT) == true || getBoolean(NEEDS_ATTENTION) == true) "high" else "normal"
        }

    private fun com.google.firebase.firestore.DocumentSnapshot.version(): Int =
        (get(VERSION) as? Number)?.toInt() ?: 0

    private fun com.google.firebase.firestore.DocumentSnapshot.nextAllowedActions(): List<LiveOrderAction> =
        (get(NEXT_ALLOWED_ACTIONS) as? List<*>)
            .orEmpty()
            .mapNotNull { LiveOrderAction.fromWire(it.orEmptyText()) }

    private fun AdminLiveOrderActionRequest.toCallablePayload(): Map<String, Any?> =
        mapOf(
            "orderId" to orderId,
            "action" to action.wireName,
            "expectedVersion" to expectedVersion,
            "reason" to reason,
        )

    private fun AdminRoleUpdateRequest.toCallablePayload(): Map<String, Any?> =
        mapOf(
            "uid" to uid,
            "role" to role,
            "active" to active,
        )

    private fun AdminConfigUpdateRequest.toCallablePayload(): Map<String, Any?> =
        mapOf(
            "field" to field,
            "enabled" to enabled,
            "amount" to amount,
        )

    private fun Any?.asText(): String = this as? String ?: ""

    private fun Any?.asIntValue(default: Int = 0): Int = (this as? Number)?.toInt() ?: default

    private fun Any?.asBoolValue(default: Boolean = false): Boolean = this as? Boolean ?: default

    @Suppress("UNCHECKED_CAST")
    private fun Any?.asMap(): Map<String, Any?> = this as? Map<String, Any?> ?: emptyMap()

    @Suppress("UNCHECKED_CAST")
    private fun Any?.asMapList(): List<Map<String, Any?>> =
        (this as? List<*>).orEmpty().mapNotNull { it as? Map<String, Any?> }

    private fun Any?.orEmptyText(): String = this?.toString().orEmpty()

    private fun Map<String, Any?>.toOperationalHealthReport(): AdminOperationalHealthReport =
        AdminOperationalHealthReport(
            healthStatus = this["healthStatus"].asText(),
            severity = this["severity"].asText(),
            generatedAt = this["generatedAt"].asText(),
            metrics = this["metrics"].asMap().toHealthMetrics(),
            modules = this["modules"].asMapList().map { it.toModuleHealth() },
            alerts = this["alerts"].asMapList().map { it.toHealthAlert() },
            criticalEvents = this["criticalEvents"].asMapList().map { it.toCriticalEvent() },
            auditSummary = this["auditSummary"].asMap().toAuditSummary(),
            securitySignals = this["securitySignals"].asMapList().map { it.toHealthAlert() },
        )

    private fun Map<String, Any?>.toHealthMetrics(): AdminOperationalHealthMetrics =
        AdminOperationalHealthMetrics(
            liveOrders = this["liveOrders"].asIntValue(),
            pendingReviewOrders = this["pendingReviewOrders"].asIntValue(),
            openIncidentOrders = this["openIncidentOrders"].asIntValue(),
            cancelledOrders = this["cancelledOrders"].asIntValue(),
            closedOrders = this["closedOrders"].asIntValue(),
            failedCommunicationOrders = this["failedCommunicationOrders"].asIntValue(),
            preparedCommunicationOrders = this["preparedCommunicationOrders"].asIntValue(),
            disabledCommunicationOrders = this["disabledCommunicationOrders"].asIntValue(),
            financialReviewOrders = this["financialReviewOrders"].asIntValue(),
            pendingAiSuggestionOrders = this["pendingAiSuggestionOrders"].asIntValue(),
            publicClaimsReceived = this["publicClaimsReceived"].asIntValue(),
            linkedPublicClaims = this["linkedPublicClaims"].asIntValue(),
            unlinkedPublicClaims = this["unlinkedPublicClaims"].asIntValue(),
            requiresAttention = this["requiresAttention"].asIntValue(),
            collectOnDeliveryOrders = this["collectOnDeliveryOrders"].asIntValue(),
            transferDeclaredPending = this["transferDeclaredPending"].asIntValue(),
            paidDeclaredUnconfirmed = this["paidDeclaredUnconfirmed"].asIntValue(),
            collectionPendingOrders = this["collectionPendingOrders"].asIntValue(),
            openIncidents = this["openIncidents"].asIntValue(),
            resolvedIncidents = this["resolvedIncidents"].asIntValue(),
            unresolvedIncidents = this["unresolvedIncidents"].asIntValue(),
            aiSuggested = this["aiSuggested"].asIntValue(),
            aiAccepted = this["aiAccepted"].asIntValue(),
            aiRejected = this["aiRejected"].asIntValue(),
            aiNotApplicable = this["aiNotApplicable"].asIntValue(),
            highRiskAi = this["highRiskAi"].asIntValue(),
            whatsappDisabled = this["whatsappDisabled"].asBoolValue(true),
            pushDisabled = this["pushDisabled"].asBoolValue(true),
            externalAiDisabled = this["externalAiDisabled"].asBoolValue(true),
            engineVersion = this["engineVersion"].asText(),
            providerStatus = this["providerStatus"].asText(),
        )

    private fun Map<String, Any?>.toModuleHealth(): AdminModuleHealth =
        AdminModuleHealth(
            key = this["key"].asText(),
            label = this["label"].asText(),
            moduleStatus = this["moduleStatus"].asText(),
            healthStatus = this["healthStatus"].asText(),
            severity = this["severity"].asText(),
            warningCode = this["warningCode"].asText(),
            warningMessage = this["warningMessage"].asText(),
        )

    private fun Map<String, Any?>.toHealthAlert(): AdminHealthAlert =
        AdminHealthAlert(
            healthStatus = this["healthStatus"].asText(),
            severity = this["severity"].asText(),
            scope = this["scope"].asText(),
            source = this["source"].asText(),
            metricKey = this["metricKey"].asText(),
            metricValue = this["metricValue"].orEmptyText(),
            warningCode = this["warningCode"].asText(),
            warningMessage = this["warningMessage"].asText(),
            requiresAdminReview = this["requiresAdminReview"].asBoolValue(),
            relatedOrderId = this["relatedOrderId"].asText(),
        )

    private fun Map<String, Any?>.toCriticalEvent(): AdminCriticalEvent =
        AdminCriticalEvent(
            relatedOrderId = this["relatedOrderId"].asText(),
            source = this["source"].asText(),
            type = this["type"].asText(),
            summary = this["summary"].asText(),
            actorRole = this["actorRole"].asText(),
            previousStatus = this["previousStatus"].asText(),
            nextStatus = this["nextStatus"].asText(),
            severity = this["severity"].asText(),
        )

    private fun Map<String, Any?>.toAuditSummary(): AdminAuditSummary =
        AdminAuditSummary(
            ordersWithEvents = this["ordersWithEvents"].asIntValue(),
            orderEventRecords = this["orderEventRecords"].asIntValue(),
            incidentRecords = this["incidentRecords"].asIntValue(),
            claimRecords = this["claimRecords"].asIntValue(),
            communicationRecords = this["communicationRecords"].asIntValue(),
            aiDecisionRecords = this["aiDecisionRecords"].asIntValue(),
            publicClaimRecords = this["publicClaimRecords"].asIntValue(),
            exposesPublicAudit = this["exposesPublicAudit"].asBoolValue(),
            correctiveActionsExecuted = this["correctiveActionsExecuted"].asBoolValue(),
        )

    private companion object {
        const val REGION = "southamerica-east1"
        const val ORDERS = "orders"

        const val USERS = "users"
        const val ADMIN_CONFIG = "admin_config"
        const val ADMIN_CONFIG_REAL_USE = "real_use"
        const val EMAIL = "email"
        const val DISPLAY_NAME = "displayName"
        const val ROLE = "role"
        const val ACTIVE = "active"
        const val STORE_ID = "storeId"
        const val DRIVER_ID = "driverId"
        const val RAIN_MODE = "rainMode"
        const val RAIN_DELIVERY_FEE = "rainDeliveryFee"
        const val BASE_DELIVERY_FEE = "baseDeliveryFee"
        const val DISTANCE_SURCHARGE = "distanceSurcharge"
        const val DEFAULT_RAIN_DELIVERY_FEE = 4000
        const val DEFAULT_BASE_DELIVERY_FEE = 3500
        const val DEFAULT_DISTANCE_SURCHARGE = 1500
        const val LAST_UPDATED_BY = "lastUpdatedBy"
        const val UPDATED_BY = "updatedBy"
        const val OPERATE_LIVE_ORDER = "operateLiveOrder"
        const val ADMIN_RECALCULATE_ORDER_ACTIONS = "adminRecalculateOrderActions"
        const val GET_OPERATIONAL_HEALTH = "getOperationalHealth"
        const val GET_ADMIN_CONFIG = "getAdminConfig"
        const val ADMIN_UPDATE_TEAM_USER = "adminUpdateTeamUser"
        const val ADMIN_UPDATE_CONFIG = "adminUpdateConfig"
        const val TRACKING = "trackingNumber"
        const val PUBLIC_NUMBER = "publicOrderNumber"
        const val STATUS = "status"
        const val PUBLIC_STATUS = "publicStatus"
        const val OPERATIONAL_STATUS = "operationalStatus"
        const val RESPONSIBLE_ROLE = "responsibleRole"
        const val PRIORITY = "priority"
        const val NEEDS_ATTENTION = "needsAttention"
        const val ACTIVE_INCIDENT = "activeIncident"
        const val ADMIN_REVIEWED = "adminReviewed"
        const val NEXT_ALLOWED_ACTIONS = "nextAllowedActions"
        const val LAST_OPERATION_EVENT = "lastOperationEvent"
        const val EVENTS = "events"
        const val EVENT_TYPE = "type"
        const val EVENT_SUMMARY = "summary"
        const val ACTOR_ROLE = "actorRole"
        const val REASON = "reason"
        const val SOURCE = "source"
        const val REQUEST_TYPE = "requestType"
        const val STORE_NAME = "storeName"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
        const val TOTAL = "total"
        const val PAYMENT_METHOD = "paymentMethod"
        const val AMOUNT_TO_COLLECT = "amountToCollect"
        const val COLLECTED_AMOUNT = "collectedAmount"
        const val COLLECTION_REQUIRED = "collectionRequired"
        const val CASH_RESPONSIBLE_ROLE = "cashResponsibleRole"
        const val FINANCIAL_NOTES = "financialNotes"
        const val ITEMS = "items"
        const val ITEM_NAME = "name"
        const val ITEM_QTY = "quantity"
        const val CUSTOMER = "customer"
        const val NAME = "name"
        const val PHONE = "phone"
        const val ADDRESS = "address"
        const val ORDER_TYPE = "orderType"
        const val FINANCIAL_STATUS = "financialStatus"
        const val COMMUNICATION_STATUS = "communicationStatus"
        const val AI_RISK_LEVEL = "aiRiskLevel"
        const val AI_CLASSIFICATION = "aiClassification"
        const val AI_SUGGESTED_ACTION = "aiSuggestedAction"
        const val AI_REQUIRES_HUMAN_REVIEW = "aiRequiresHumanReview"
        const val INCIDENT_STATUS = "incidentStatus"
        const val ARCHIVE_STATUS = "archiveStatus"
        const val CURRENT_RESPONSIBLE_ROLE = "currentResponsibleRole"
        const val ASSIGNED_ACTOR_ID = "assignedActorId"
        const val ASSIGNED_ACTOR_ROLE = "assignedActorRole"
        const val VERSION = "version"
        const val IDEMPOTENCY_KEY = "idempotencyKey"
    }
}
