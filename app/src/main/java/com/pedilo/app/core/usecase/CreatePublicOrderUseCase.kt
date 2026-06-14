package com.pedilo.app.core.usecase

import com.pedilo.app.core.model.PublicOrderDraft
import com.pedilo.app.core.model.PublicOrderTicket
import com.pedilo.app.core.port.PublicOrderPort
import com.pedilo.app.core.result.CoreResult

class CreatePublicOrderUseCase(
    private val orderPort: PublicOrderPort,
    private val validateDraft: ValidatePublicOrderDraftUseCase = ValidatePublicOrderDraftUseCase(),
) {
    suspend operator fun invoke(draft: PublicOrderDraft): CoreResult<PublicOrderTicket> =
        when (val validation = validateDraft(draft)) {
            is CoreResult.Failure -> validation
            is CoreResult.Success -> orderPort.createPublicOrder(draft)
        }
}
