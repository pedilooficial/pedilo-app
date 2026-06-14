package com.pedilo.app.core.port

import com.pedilo.app.core.model.AdminLiveOrderActionRequest
import com.pedilo.app.core.model.AdminLiveOrderActionResult
import com.pedilo.app.core.model.PublicProductSummary
import com.pedilo.app.core.model.StoreOrderDetail
import com.pedilo.app.core.model.StoreOrderSummary
import com.pedilo.app.core.result.CoreResult
import kotlinx.coroutines.flow.Flow

interface StoreOrdersPort {
    fun observeOwnOrders(): Flow<CoreResult<List<StoreOrderSummary>>>
    suspend fun getOwnOrderDetail(orderId: String): CoreResult<StoreOrderDetail>
    suspend fun getOwnProducts(): CoreResult<List<PublicProductSummary>>
    suspend fun executeStoreOrderAction(request: AdminLiveOrderActionRequest): CoreResult<AdminLiveOrderActionResult>
}
