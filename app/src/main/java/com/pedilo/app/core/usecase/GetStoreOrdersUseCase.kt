package com.pedilo.app.core.usecase

import com.pedilo.app.core.model.AdminLiveOrderActionRequest
import com.pedilo.app.core.port.StoreOrdersPort

class GetStoreOrdersUseCase(
    private val port: StoreOrdersPort,
) {
    fun observe() = port.observeOwnOrders()
    suspend fun getDetail(orderId: String) = port.getOwnOrderDetail(orderId)
    suspend fun getProducts() = port.getOwnProducts()
    suspend fun execute(request: AdminLiveOrderActionRequest) = port.executeStoreOrderAction(request)
}
