package com.pedilo.app.core.firebase

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.pedilo.app.core.model.PublicOrderStatus
import com.pedilo.app.core.model.PublicOrderTicket
import com.pedilo.app.core.model.PublicPlusOrderDraft
import com.pedilo.app.core.model.PublicPlusOrderType
import com.pedilo.app.core.port.PublicPlusOrderPort
import com.pedilo.app.core.result.CoreError
import com.pedilo.app.core.result.CoreResult
import kotlinx.coroutines.tasks.await

class FirebasePublicPlusOrderAdapter(
    private val functions: FirebaseFunctions = Firebase.functions(REGION),
) : PublicPlusOrderPort {
    override suspend fun createPlusOrder(draft: PublicPlusOrderDraft): CoreResult<PublicOrderTicket> =
        runCatching {
            val result = functions
                .getHttpsCallable(CREATE_PLUS_ORDER)
                .call(draft.toCallablePayload())
                .await()

            @Suppress("UNCHECKED_CAST")
            val data = result.getData() as? Map<String, Any?>
                ?: error("Unexpected createPlusOrder response")

            PublicOrderTicket(
                orderId = data["orderId"].asText(),
                trackingNumber = data["trackingNumber"].asText(),
                status = PublicOrderStatus.RECEIVED,
                publicStatus = data["publicStatus"].asText().ifBlank { "Pedido recibido" },
                storeName = data["requestLabel"].asText(),
            )
        }.fold(
            onSuccess = { CoreResult.Success(it) },
            onFailure = { CoreResult.Failure(CoreError.NotAvailable) },
        )

    private fun PublicPlusOrderDraft.toCallablePayload(): Map<String, Any?> =
        mapOf(
            "source" to source,
            "requestType" to requestType.toWireValue(),
            "contact" to mapOf(
                "name" to contact.name,
                "phone" to contact.phone,
            ),
            "items" to items.map {
                mapOf(
                    "name" to it.name,
                    "detail" to it.detail,
                )
            },
            "sourceReference" to sourceReference,
            "destination" to destination,
            "note" to note,
            "paymentMethod" to paymentMethod,
            "amount" to amount,
            "schedule" to schedule,
        )

    private fun PublicPlusOrderType.toWireValue(): String = when (this) {
        PublicPlusOrderType.BUY -> "buy"
        PublicPlusOrderType.PICKUP_SHIPPING -> "pickup_shipping"
    }

    private fun Any?.asText(): String = this as? String ?: ""

    private companion object {
        const val REGION = "southamerica-east1"
        const val CREATE_PLUS_ORDER = "createPlusOrder"
    }
}
