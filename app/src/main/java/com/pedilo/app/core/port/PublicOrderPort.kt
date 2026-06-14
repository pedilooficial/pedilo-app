package com.pedilo.app.core.port

import com.pedilo.app.core.model.PublicOrderDraft
import com.pedilo.app.core.model.PublicOrderTicket
import com.pedilo.app.core.result.CoreResult

interface PublicOrderPort {
    suspend fun createPublicOrder(draft: PublicOrderDraft): CoreResult<PublicOrderTicket>
}
