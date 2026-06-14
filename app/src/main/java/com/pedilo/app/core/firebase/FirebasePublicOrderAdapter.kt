package com.pedilo.app.core.firebase

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.pedilo.app.core.model.PaymentMethod
import com.pedilo.app.core.model.PublicOrderDraft
import com.pedilo.app.core.model.PublicOrderStatus
import com.pedilo.app.core.model.PublicOrderTicket
import com.pedilo.app.core.port.PublicOrderPort
import com.pedilo.app.core.result.CoreError
import com.pedilo.app.core.result.CoreResult
import kotlinx.coroutines.tasks.await

class FirebasePublicOrderAdapter(
    private val functions: FirebaseFunctions = Firebase.functions(REGION),
) : PublicOrderPort {
    override suspend fun createPublicOrder(draft: PublicOrderDraft): CoreResult<PublicOrderTicket> =
        runCatching {
            val result = functions
                .getHttpsCallable(CREATE_LOCAL_ORDER)
                .call(draft.toCallablePayload())
                .await()

            @Suppress("UNCHECKED_CAST")
            val data = result.getData() as? Map<String, Any?>
                ?: error("Unexpected createLocalOrder response")

            PublicOrderTicket(
                orderId = data["orderId"].asText(),
                trackingNumber = data["trackingNumber"].asText(),
                status = PublicOrderStatus.RECEIVED,
                publicStatus = data["publicStatus"].asText().ifBlank { "Pedido recibido" },
                storeName = data["storeName"].asText(),
            )
        }.fold(
            onSuccess = { CoreResult.Success(it) },
            onFailure = { CoreResult.Failure(CoreError.NotAvailable) },
        )

    private fun PublicOrderDraft.toCallablePayload(): Map<String, Any?> =
        mapOf(
            "source" to source,
            "storeId" to storeId,
            "storeName" to storeName,
            "customer" to mapOf(
                "name" to contact.name,
                "phone" to contact.phone,
                "address" to deliveryLocation?.addressLine.orEmpty(),
            ),
            "note" to notes,
            "paymentMethod" to paymentMethod.toWireValue(),
            "items" to items.map {
                mapOf(
                    "productId" to it.productId,
                    "storeId" to it.storeId,
                    "name" to it.name,
                    "quantity" to it.quantity,
                    "unitPrice" to it.unitPriceCents,
                    "note" to it.notes,
                )
            },
        )

    private fun PaymentMethod.toWireValue(): String = when (this) {
        PaymentMethod.Cash -> "cash"
        PaymentMethod.Card -> "card"
        PaymentMethod.Transfer -> "transfer"
        PaymentMethod.NotSpecified -> ""
    }

    private fun Any?.asText(): String = this as? String ?: ""

    private companion object {
        const val REGION = "southamerica-east1"
        const val CREATE_LOCAL_ORDER = "createLocalOrder"
    }
}
