package com.pedilo.app.core.usecase

import com.pedilo.app.core.model.PublicOrderDraft
import com.pedilo.app.core.result.CoreError
import com.pedilo.app.core.result.CoreResult
import com.pedilo.app.core.result.ValidationError

class ValidatePublicOrderDraftUseCase {
    operator fun invoke(draft: PublicOrderDraft): CoreResult<Unit> {
        val errors = buildList {
            if (draft.source != "public_local") {
                add(ValidationError(ValidationError.Field.STORE, ValidationError.Reason.INVALID))
            }
            if (draft.storeId.isBlankOrPlaceholder() || draft.storeName.isBlankOrPlaceholder()) {
                add(ValidationError(ValidationError.Field.STORE, ValidationError.Reason.REQUIRED))
            }
            if (draft.contact.name.isBlankOrPlaceholder()) {
                add(ValidationError(ValidationError.Field.NAME, ValidationError.Reason.REQUIRED))
            }
            if (draft.contact.phone.isBlankOrPlaceholder()) {
                add(ValidationError(ValidationError.Field.PHONE, ValidationError.Reason.REQUIRED))
            } else if (draft.contact.phone.count { it.isDigit() } !in 8..15) {
                add(ValidationError(ValidationError.Field.PHONE, ValidationError.Reason.INVALID))
            }
            if (draft.deliveryLocation?.addressLine.isNullOrBlankOrPlaceholder()) {
                add(ValidationError(ValidationError.Field.ADDRESS, ValidationError.Reason.REQUIRED))
            }
            if (draft.paymentMethod == com.pedilo.app.core.model.PaymentMethod.NotSpecified) {
                add(ValidationError(ValidationError.Field.PAYMENT, ValidationError.Reason.REQUIRED))
            }
            if (draft.items.isEmpty() || draft.items.any {
                    it.productId.isBlankOrPlaceholder() ||
                        it.storeId != draft.storeId ||
                        it.name.isBlankOrPlaceholder() ||
                        it.quantity <= 0
                }
            ) {
                add(ValidationError(ValidationError.Field.ITEMS, ValidationError.Reason.REQUIRED))
            }
        }

        return if (errors.isEmpty()) {
            CoreResult.Success(Unit)
        } else {
            CoreResult.Failure(CoreError.Validation(errors))
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
)

internal fun String.isPlaceholder(): Boolean =
    trim().lowercase() in placeholderValues

private fun String.isBlankOrPlaceholder(): Boolean =
    isBlank() || isPlaceholder()

private fun String?.isNullOrBlankOrPlaceholder(): Boolean =
    this == null || isBlankOrPlaceholder()
