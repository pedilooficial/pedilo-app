package com.pedilo.app.core.firebase

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.pedilo.app.core.model.PublicOrderStatus
import com.pedilo.app.core.model.PublicTrackingState
import com.pedilo.app.core.port.PublicTrackingPort
import com.pedilo.app.core.result.CoreError
import com.pedilo.app.core.result.CoreResult
import kotlinx.coroutines.tasks.await

class FirebasePublicTrackingAdapter(
    private val functions: FirebaseFunctions = Firebase.functions(REGION),
) : PublicTrackingPort {
    override suspend fun getPublicTracking(trackingNumber: String): CoreResult<PublicTrackingState> =
        runCatching {
            val result = functions
                .getHttpsCallable(GET_PUBLIC_ORDER_TRACKING)
                .call(mapOf("trackingNumber" to trackingNumber))
                .await()

            @Suppress("UNCHECKED_CAST")
            val data = result.getData() as? Map<String, Any?>
                ?: error("Unexpected getPublicOrderTracking response")

            PublicTrackingState(
                trackingNumber = data["trackingNumber"].asText().ifBlank { trackingNumber },
                status = data["status"].asStatus(),
                publicStatus = data["publicStatus"].asText().ifBlank { "Pedido recibido" },
                humanMessage = data["humanMessage"].asText().ifBlank { "Ya recibimos tu pedido." },
                found = data["found"] as? Boolean ?: false,
                orderType = data["orderType"].asText(),
                storeName = data["storeName"].asText(),
                summary = data["summary"].asText(),
                paymentLabel = data["paymentLabel"].asText(),
                publicTotal = data["publicTotal"].asText(),
                collectionMessage = data["collectionMessage"].asText(),
                isClosed = data["isClosed"] as? Boolean ?: false,
                canCancel = data["canCancel"] as? Boolean ?: false,
            )
        }.fold(
            onSuccess = { CoreResult.Success(it) },
            onFailure = { CoreResult.Failure(CoreError.NotAvailable) },
        )

    override suspend fun requestPublicCancellation(trackingNumber: String, contact: String, reason: String): CoreResult<String> =
        runCatching {
            val result = functions
                .getHttpsCallable(REQUEST_PUBLIC_CANCELLATION)
                .call(
                    mapOf(
                        "trackingNumber" to trackingNumber,
                        "contact" to contact,
                        "reason" to reason,
                    ),
                )
                .await()

            @Suppress("UNCHECKED_CAST")
            val data = result.getData() as? Map<String, Any?>
                ?: error("Unexpected requestPublicCancellation response")

            data["humanMessage"].asText().ifBlank { "Cancelación registrada." }
        }.fold(
            onSuccess = { CoreResult.Success(it) },
            onFailure = { CoreResult.Failure(CoreError.Operational((it as? com.google.firebase.functions.FirebaseFunctionsException)?.message ?: "No pudimos cancelar el pedido.")) },
        )

    private fun Any?.asText(): String = this as? String ?: ""

    private fun Any?.asStatus(): PublicOrderStatus = when (asText().uppercase()) {
        "PREPARING" -> PublicOrderStatus.PREPARING
        "ON_THE_WAY" -> PublicOrderStatus.ON_THE_WAY
        "DELIVERED" -> PublicOrderStatus.DELIVERED
        "CANCELLED" -> PublicOrderStatus.CANCELLED
        "UNDER_REVIEW" -> PublicOrderStatus.UNDER_REVIEW
        else -> PublicOrderStatus.RECEIVED
    }

    private companion object {
        const val REGION = "southamerica-east1"
        const val GET_PUBLIC_ORDER_TRACKING = "getPublicOrderTracking"
        const val REQUEST_PUBLIC_CANCELLATION = "requestPublicCancellation"
    }
}
