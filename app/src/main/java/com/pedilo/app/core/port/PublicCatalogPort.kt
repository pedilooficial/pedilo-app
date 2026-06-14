package com.pedilo.app.core.port

import com.pedilo.app.core.model.PublicProductSummary
import com.pedilo.app.core.model.PublicStoreSummary
import com.pedilo.app.core.result.CoreResult

interface PublicCatalogPort {
    suspend fun getVisibleStores(): CoreResult<List<PublicStoreSummary>>
    suspend fun getProductsForStore(storeId: String): CoreResult<List<PublicProductSummary>>
}
