package com.pedilo.app.core.port

import com.pedilo.app.core.model.PublicClaimDraft
import com.pedilo.app.core.model.PublicClaimReceipt
import com.pedilo.app.core.result.CoreResult

interface PublicClaimPort {
    suspend fun submitPublicClaim(draft: PublicClaimDraft): CoreResult<PublicClaimReceipt>
}
