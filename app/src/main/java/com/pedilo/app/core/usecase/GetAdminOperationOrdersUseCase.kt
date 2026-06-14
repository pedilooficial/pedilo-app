package com.pedilo.app.core.usecase

import com.pedilo.app.core.model.AdminRoleUpdateRequest
import com.pedilo.app.core.model.AdminConfigUpdateRequest
import com.pedilo.app.core.model.AdminLiveOrderActionRequest
import com.pedilo.app.core.port.AdminOrdersPort

class GetAdminOperationOrdersUseCase(
    private val port: AdminOrdersPort,
) {
    suspend operator fun invoke() = port.getOrdersReadOnly()
    fun observe() = port.observeOrdersReadOnly()
    suspend fun getDetail(orderId: String) = port.getOrderDetailReadOnly(orderId)
    suspend fun executeLive(request: AdminLiveOrderActionRequest) = port.executeLiveOrderAction(request)
    suspend fun recalculateActions(orderId: String) = port.recalculateOrderActions(orderId)
    suspend fun getEvents(orderId: String) = port.getOrderEventsReadOnly(orderId)
    suspend fun getHealth() = port.getOperationalHealth()
    fun observeTeamUsers() = port.observeTeamUsers()
    fun observeAdminConfig() = port.observeAdminConfig()
    suspend fun updateTeamUser(request: AdminRoleUpdateRequest) = port.updateTeamUser(request)
    suspend fun updateAdminConfig(request: AdminConfigUpdateRequest) = port.updateAdminConfig(request)
}
