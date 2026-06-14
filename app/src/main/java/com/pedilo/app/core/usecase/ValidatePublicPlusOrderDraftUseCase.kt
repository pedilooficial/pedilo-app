package com.pedilo.app.core.usecase

import com.pedilo.app.core.model.PublicPlusOrderDraft
import com.pedilo.app.core.model.PublicPlusOrderType
import com.pedilo.app.core.result.CoreError
import com.pedilo.app.core.result.CoreResult
import com.pedilo.app.core.result.ValidationError

class ValidatePublicPlusOrderDraftUseCase {
    operator fun invoke(draft: PublicPlusOrderDraft): CoreResult<Unit> {
        val errors = buildList {
            if (draft.source !in setOf("public_plus_buy", "public_plus_pickup_shipping")) {
                add(ValidationError(ValidationError.Field.SOURCE, ValidationError.Reason.INVALID))
            }
            if (draft.contact.name.isBlankOrPlaceholder()) {
                add(ValidationError(ValidationError.Field.NAME, ValidationError.Reason.REQUIRED))
            }
            if (draft.contact.phone.isBlankOrPlaceholder()) {
                add(ValidationError(ValidationError.Field.PHONE, ValidationError.Reason.REQUIRED))
            } else if (draft.contact.phone.count(Char::isDigit) !in 8..15) {
                add(ValidationError(ValidationError.Field.PHONE, ValidationError.Reason.INVALID))
            }
            if (draft.paymentMethod.isBlankOrPlaceholder()) {
                add(ValidationError(ValidationError.Field.PAYMENT, ValidationError.Reason.REQUIRED))
            }
            when (draft.requestType) {
                PublicPlusOrderType.BUY -> validateBuy(draft)
                PublicPlusOrderType.PICKUP_SHIPPING -> validatePickupShipping(draft)
            }.forEach(::add)
        }

        return if (errors.isEmpty()) {
            CoreResult.Success(Unit)
        } else {
            CoreResult.Failure(CoreError.Validation(errors))
        }
    }

    private fun validateBuy(draft: PublicPlusOrderDraft): List<ValidationError> = buildList {
        if (draft.source != "public_plus_buy") {
            add(ValidationError(ValidationError.Field.SOURCE, ValidationError.Reason.INVALID))
        }
        if (draft.items.isEmpty() || draft.items.any { it.name.isBlankOrPlaceholder() || it.detail.isBlankOrPlaceholder() }) {
            add(ValidationError(ValidationError.Field.ITEMS, ValidationError.Reason.REQUIRED))
        }
        if (draft.sourceReference.isBlankOrPlaceholder()) {
            add(ValidationError(ValidationError.Field.STORE, ValidationError.Reason.REQUIRED))
        }
        if (draft.destination.isBlankOrPlaceholder()) {
            add(ValidationError(ValidationError.Field.ADDRESS, ValidationError.Reason.REQUIRED))
        }
    }

    private fun validatePickupShipping(draft: PublicPlusOrderDraft): List<ValidationError> = buildList {
        if (draft.source != "public_plus_pickup_shipping") {
            add(ValidationError(ValidationError.Field.SOURCE, ValidationError.Reason.INVALID))
        }
        if (draft.sourceReference.isBlankOrPlaceholder()) {
            add(ValidationError(ValidationError.Field.PICKUP_ADDRESS, ValidationError.Reason.REQUIRED))
        }
        if (draft.destination.isBlankOrPlaceholder()) {
            add(ValidationError(ValidationError.Field.ADDRESS, ValidationError.Reason.REQUIRED))
        }
        if (draft.items.isEmpty() || draft.items.first().name.isBlankOrPlaceholder()) {
            add(ValidationError(ValidationError.Field.ITEMS, ValidationError.Reason.REQUIRED))
        }
    }
}

private val placeholderValues = setOf(
    "nombre",
    "tu nombre",
    "telefono",
    "teléfono",
    "direccion",
    "dirección",
    "pedido",
    "producto",
    "paquete",
)

private fun String.isBlankOrPlaceholder(): Boolean =
    isBlank() || trim().lowercase() in placeholderValues
