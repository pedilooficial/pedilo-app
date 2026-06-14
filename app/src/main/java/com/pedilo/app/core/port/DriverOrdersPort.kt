package com.pedilo.app.core.port

import com.pedilo.app.core.model.AdminLiveOrderActionRequest
import com.pedilo.app.core.model.AdminLiveOrderActionResult
import com.pedilo.app.core.model.DriverCashboxCloseResult
import com.pedilo.app.core.model.DriverOrderDetail
import com.pedilo.app.core.model.DriverOrderSummary
import com.pedilo.app.core.result.CoreResult
import kotlinx.coroutines.flow.Flow

interface DriverOrdersPort {
    fun observeAvailableAndAssignedOrders(): Flow<CoreResult<List<DriverOrderSummary>>>
    suspend fun getVisibleOrderDetail(orderId: String): CoreResult<DriverOrderDetail>
    suspend fun executeDriverOrderAction(request: AdminLiveOrderActionRequest): CoreResult<AdminLiveOrderActionResult>
    suspend fun closeCashbox(note: String): CoreResult<DriverCashboxCloseResult>
}
