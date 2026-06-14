package com.pedilo.app.core.usecase

import com.pedilo.app.core.model.PublicClaimDraft
import com.pedilo.app.core.model.PublicClaimReceipt
import com.pedilo.app.core.port.PublicClaimPort
import com.pedilo.app.core.result.CoreError
import com.pedilo.app.core.result.CoreResult
import com.pedilo.app.core.result.ValidationError

class SubmitPublicClaimUseCase(
    private val publicClaimPort: PublicClaimPort,
) {
    suspend operator fun invoke(draft: PublicClaimDraft): CoreResult<PublicClaimReceipt> {
        val clean = draft.copy(
            trackingNumber = draft.trackingNumber.trim().uppercase(),
            customerName = draft.customerName.trim(),
            contact = draft.contact.trim(),
            reason = draft.reason.trim(),
            description = draft.description.trim(),
            type = draft.type.trim().ifBlank { "other" },
        )
        if (clean.trackingNumber.isNotBlank() && !clean.trackingNumber.matches(Regex("^PDL-[A-Z0-9]{4,10}$"))) {
            return CoreResult.Failure(
                CoreError.Validation(
                    listOf(ValidationError(ValidationError.Field.TRACKING_NUMBER, ValidationError.Reason.INVALID)),
                ),
            )
        }
        if (clean.customerName.isBlank() || clean.contact.length < 8 || clean.reason.length < 4 || clean.description.length < 8) {
            return CoreResult.Failure(CoreError.IncompleteData)
        }
        return publicClaimPort.submitPublicClaim(clean)
    }
}
