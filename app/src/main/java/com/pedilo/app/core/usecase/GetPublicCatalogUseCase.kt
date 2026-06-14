package com.pedilo.app.core.usecase

import com.pedilo.app.core.model.PublicProductSummary
import com.pedilo.app.core.model.PublicStoreSummary
import com.pedilo.app.core.port.PublicCatalogPort
import com.pedilo.app.core.result.CoreError
import com.pedilo.app.core.result.CoreResult

class GetPublicCatalogUseCase(
    private val catalogPort: PublicCatalogPort,
) {
    suspend fun stores(): CoreResult<List<PublicStoreSummary>> =
        catalogPort.getVisibleStores()

    suspend fun productsForStore(storeId: String): CoreResult<List<PublicProductSummary>> =
        if (storeId.isBlank()) {
            CoreResult.Failure(CoreError.IncompleteData)
        } else {
            catalogPort.getProductsForStore(storeId.trim())
        }
}
