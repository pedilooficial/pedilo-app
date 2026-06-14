package com.pedilo.app.core.usecase

import com.pedilo.app.core.model.AdminLiveOrderActionRequest
import com.pedilo.app.core.port.DriverOrdersPort

class GetDriverOrdersUseCase(
    private val port: DriverOrdersPort,
) {
    fun observe() = port.observeAvailableAndAssignedOrders()
    suspend fun getDetail(orderId: String) = port.getVisibleOrderDetail(orderId)
    suspend fun execute(request: AdminLiveOrderActionRequest) = port.executeDriverOrderAction(request)
    suspend fun closeCashbox(note: String) = port.closeCashbox(note.trim())
}
