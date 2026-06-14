package com.pedilo.app.core.firebase

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.pedilo.app.core.model.PublicClaimDraft
import com.pedilo.app.core.model.PublicClaimReceipt
import com.pedilo.app.core.port.PublicClaimPort
import com.pedilo.app.core.result.CoreError
import com.pedilo.app.core.result.CoreResult
import kotlinx.coroutines.tasks.await

class FirebasePublicClaimAdapter(
    private val functions: FirebaseFunctions = Firebase.functions(REGION),
) : PublicClaimPort {
    override suspend fun submitPublicClaim(draft: PublicClaimDraft): CoreResult<PublicClaimReceipt> =
        runCatching {
            val result = functions
                .getHttpsCallable(SUBMIT_PUBLIC_CLAIM)
                .call(
                    mapOf(
                        "trackingNumber" to draft.trackingNumber,
                        "customerName" to draft.customerName,
                        "contact" to draft.contact,
                        "reason" to draft.reason,
                        "description" to draft.description,
                        "type" to draft.type,
                    ),
                )
                .await()

            @Suppress("UNCHECKED_CAST")
            val data = result.getData() as? Map<String, Any?>
                ?: error("Unexpected submitPublicClaim response")

            PublicClaimReceipt(
                claimId = data["claimId"].asText(),
                status = data["status"].asText(),
                publicMessage = data["publicMessage"].asText(),
            )
        }.fold(
            onSuccess = { CoreResult.Success(it) },
            onFailure = { CoreResult.Failure(CoreError.Operational((it as? FirebaseFunctionsException)?.message ?: "No pudimos registrar el reclamo.")) },
        )

    private fun Any?.asText(): String = this as? String ?: ""

    private companion object {
        const val REGION = "southamerica-east1"
        const val SUBMIT_PUBLIC_CLAIM = "submitPublicClaim"
    }
}
