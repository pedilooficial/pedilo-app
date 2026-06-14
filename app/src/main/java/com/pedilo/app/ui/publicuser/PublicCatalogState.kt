package com.pedilo.app.ui.publicuser

import com.pedilo.app.core.model.PublicProductSummary
import com.pedilo.app.core.model.PublicStoreSummary
import com.pedilo.app.core.port.PublicCatalogPort
import com.pedilo.app.core.result.CoreResult
import com.pedilo.app.core.runtime.publicCatalogPort

data class PublicCatalogState(
    val stores: List<PublicStoreSummary> = emptyList(),
    val productsByStore: Map<String, List<PublicProductSummary>> = emptyMap(),
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val hasRealCatalog: Boolean = false,
)

suspend fun loadPublicCatalogState(
    adapter: PublicCatalogPort = publicCatalogPort(),
): PublicCatalogState {
    val storesResult = adapter.getVisibleStores()
    if (storesResult !is CoreResult.Success) {
        return PublicCatalogState(isLoading = false, loadFailed = true)
    }

    val stores = storesResult.value
    val productsByStore = stores.associate { store ->
        val productsResult = adapter.getProductsForStore(store.id)
        val products = when (productsResult) {
            is CoreResult.Success -> productsResult.value
            is CoreResult.Failure -> emptyList()
        }
        store.id to products
    }

    return PublicCatalogState(
        stores = stores,
        productsByStore = productsByStore,
        isLoading = false,
        hasRealCatalog = stores.isNotEmpty(),
    )
}
