package com.pedilo.app.core.usecase

import com.pedilo.app.core.model.PublicOrderTicket
import com.pedilo.app.core.model.PublicPlusOrderDraft
import com.pedilo.app.core.port.PublicPlusOrderPort
import com.pedilo.app.core.result.CoreResult

class CreatePublicPlusOrderUseCase(
    private val orderPort: PublicPlusOrderPort,
    private val validateDraft: ValidatePublicPlusOrderDraftUseCase = ValidatePublicPlusOrderDraftUseCase(),
) {
    suspend operator fun invoke(draft: PublicPlusOrderDraft): CoreResult<PublicOrderTicket> =
        when (val validation = validateDraft(draft)) {
            is CoreResult.Failure -> validation
            is CoreResult.Success -> orderPort.createPlusOrder(draft)
        }
}
