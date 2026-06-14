package com.pedilo.app.core.port

import com.pedilo.app.core.model.PublicOrderTicket
import com.pedilo.app.core.model.PublicPlusOrderDraft
import com.pedilo.app.core.result.CoreResult

interface PublicPlusOrderPort {
    suspend fun createPlusOrder(draft: PublicPlusOrderDraft): CoreResult<PublicOrderTicket>
}
