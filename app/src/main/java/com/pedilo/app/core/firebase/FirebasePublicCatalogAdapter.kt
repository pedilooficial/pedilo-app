package com.pedilo.app.core.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.pedilo.app.core.model.PublicProductSummary
import com.pedilo.app.core.model.PublicStoreSummary
import com.pedilo.app.core.port.PublicCatalogPort
import com.pedilo.app.core.result.CoreError
import com.pedilo.app.core.result.CoreResult
import kotlinx.coroutines.tasks.await

class FirebasePublicCatalogAdapter(
    private val db: FirebaseFirestore = Firebase.firestore,
) : PublicCatalogPort {
    override suspend fun getVisibleStores(): CoreResult<List<PublicStoreSummary>> =
        runCatching {
            db.collection(STORES)
                .whereEqualTo(VISIBLE, true)
                .get()
                .await()
                .documents
                .mapNotNull { it.toPublicStoreSummaryOrNull() }
        }.fold(
            onSuccess = { CoreResult.Success(it) },
            onFailure = { CoreResult.Failure(CoreError.NotAvailable) },
        )

    override suspend fun getProductsForStore(storeId: String): CoreResult<List<PublicProductSummary>> {
        val cleanStoreId = storeId.trim()
        if (cleanStoreId.isBlank()) return CoreResult.Failure(CoreError.IncompleteData)

        return runCatching {
            db.collection(STORES)
                .document(cleanStoreId)
                .collection(PRODUCTS)
                .whereEqualTo(VISIBLE, true)
                .whereEqualTo(AVAILABLE, true)
                .get()
                .await()
                .documents
                .mapNotNull { it.toPublicProductSummaryOrNull(cleanStoreId) }
        }.fold(
            onSuccess = { CoreResult.Success(it) },
            onFailure = { CoreResult.Failure(CoreError.NotAvailable) },
        )
    }

    private companion object {
        const val STORES = "stores"
        const val PRODUCTS = "products"
        const val VISIBLE = "visible"
        const val AVAILABLE = "available"
    }
}
