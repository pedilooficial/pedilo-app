package com.pedilo.app.core.port

import com.pedilo.app.core.model.AdminConfigState
import com.pedilo.app.core.model.AdminConfigUpdateRequest
import com.pedilo.app.core.model.AdminMutationResult
import com.pedilo.app.core.model.AdminOrderDetail
import com.pedilo.app.core.model.AdminRoleUpdateRequest
import com.pedilo.app.core.model.AdminTeamUser
import com.pedilo.app.core.model.AdminLiveOrderActionRequest
import com.pedilo.app.core.model.AdminLiveOrderActionResult
import com.pedilo.app.core.model.AdminOrderActionsRepairResult
import com.pedilo.app.core.model.AdminOrderEvent
import com.pedilo.app.core.model.AdminOperationalHealthReport
import com.pedilo.app.core.model.AdminOrderSummary
import com.pedilo.app.core.result.CoreResult
import kotlinx.coroutines.flow.Flow

interface AdminOrdersPort {
    suspend fun getOrdersReadOnly(): CoreResult<List<AdminOrderSummary>>
    fun observeOrdersReadOnly(): Flow<CoreResult<List<AdminOrderSummary>>>
    suspend fun getOrderDetailReadOnly(orderId: String): CoreResult<AdminOrderDetail>
    suspend fun executeLiveOrderAction(request: AdminLiveOrderActionRequest): CoreResult<AdminLiveOrderActionResult>
    suspend fun recalculateOrderActions(orderId: String): CoreResult<AdminOrderActionsRepairResult>
    suspend fun getOrderEventsReadOnly(orderId: String): CoreResult<List<AdminOrderEvent>>
    suspend fun getOperationalHealth(): CoreResult<AdminOperationalHealthReport>
    fun observeTeamUsers(): Flow<CoreResult<List<AdminTeamUser>>>
    fun observeAdminConfig(): Flow<CoreResult<AdminConfigState>>
    suspend fun updateTeamUser(request: AdminRoleUpdateRequest): CoreResult<AdminMutationResult>
    suspend fun updateAdminConfig(request: AdminConfigUpdateRequest): CoreResult<AdminMutationResult>
}
