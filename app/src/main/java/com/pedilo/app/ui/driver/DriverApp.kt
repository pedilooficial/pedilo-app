package com.pedilo.app.ui.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pedilo.app.core.model.AdminLiveOrderActionRequest
import com.pedilo.app.core.model.DriverOrderDetail
import com.pedilo.app.core.model.DriverOrderSummary
import com.pedilo.app.core.model.LiveOrderAction
import com.pedilo.app.core.result.CoreError
import com.pedilo.app.core.result.CoreResult
import com.pedilo.app.core.runtime.driverOrdersUseCase
import com.pedilo.app.ui.components.PediloTextField
import com.pedilo.app.ui.publicuser.PediloBg
import com.pedilo.app.ui.publicuser.PediloGreen
import com.pedilo.app.ui.publicuser.PediloLine
import com.pedilo.app.ui.publicuser.PediloMuted
import com.pedilo.app.ui.publicuser.PediloOrange
import com.pedilo.app.ui.publicuser.PediloPanel
import com.pedilo.app.ui.publicuser.PediloPanelSoft
import com.pedilo.app.ui.publicuser.PediloText
import com.pedilo.app.ui.publicuser.PediloWarning
import kotlinx.coroutines.launch

private data class PendingDriverAction(
    val orderId: String,
    val action: LiveOrderAction,
    val expectedVersion: Int,
)

@Composable
fun DriverApp(onSignOutConfirmed: () -> Unit) {
    val driverOrders = remember { driverOrdersUseCase() }
    val scope = rememberCoroutineScope()
    var orders by remember { mutableStateOf<List<DriverOrderSummary>>(emptyList()) }
    var selectedOrderId by remember { mutableStateOf<String?>(null) }
    var detail by remember { mutableStateOf<DriverOrderDetail?>(null) }
    var message by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var pendingAction by remember { mutableStateOf<PendingDriverAction?>(null) }
    var pendingReason by remember { mutableStateOf("") }
    var runningAction by remember { mutableStateOf<PendingDriverAction?>(null) }
    var isClosingCashbox by remember { mutableStateOf(false) }
    var cashboxDialogOpen by remember { mutableStateOf(false) }
    var cashboxNote by remember { mutableStateOf("") }

    fun refreshDetail(orderId: String) {
        scope.launch {
            when (val result = driverOrders.getDetail(orderId)) {
                is CoreResult.Success -> detail = result.value
                is CoreResult.Failure -> {
                    detail = null
                    error = result.error.driverErrorMessage()
                }
            }
        }
    }

    fun executeAction(pending: PendingDriverAction, reason: String) {
        if (runningAction != null) return
        runningAction = pending
        scope.launch {
            message = ""
            error = ""
            when (val result = driverOrders.execute(
                AdminLiveOrderActionRequest(
                    orderId = pending.orderId,
                    action = pending.action,
                    expectedVersion = pending.expectedVersion,
                    reason = reason,
                ),
            )) {
                is CoreResult.Success -> {
                    message = result.value.humanMessage.ifBlank { result.value.eventSummary }
                    pendingAction = null
                    pendingReason = ""
                    runningAction = null
                    refreshDetail(pending.orderId)
                }
                is CoreResult.Failure -> {
                    error = result.error.driverErrorMessage()
                    pendingAction = null
                    pendingReason = ""
                    runningAction = null
                    refreshDetail(pending.orderId)
                }
            }
        }
    }

    fun closeCashbox(note: String) {
        if (isClosingCashbox) return
        isClosingCashbox = true
        scope.launch {
            message = ""
            error = ""
            when (val result = driverOrders.closeCashbox(note)) {
                is CoreResult.Success -> {
                    val value = result.value
                    message = "${value.humanMessage} Pedidos: ${value.ordersCount}. Total: ${value.declaredAmountCents.driverCentsLabel()}."
                    cashboxDialogOpen = false
                    cashboxNote = ""
                    isClosingCashbox = false
                    selectedOrderId?.let { refreshDetail(it) }
                }
                is CoreResult.Failure -> {
                    error = result.error.driverErrorMessage()
                    cashboxDialogOpen = false
                    cashboxNote = ""
                    isClosingCashbox = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        driverOrders.observe().collect { result ->
            when (result) {
                is CoreResult.Success -> orders = result.value
                is CoreResult.Failure -> {
                    orders = emptyList()
                    error = result.error.driverErrorMessage()
                }
            }
        }
    }

    selectedOrderId?.let { LaunchedEffect(it) { refreshDetail(it) } }

    val availableOrders = orders.filter { !it.isAssignedToCurrentDriver }
    val assignedOrders = orders.filter { it.isAssignedToCurrentDriver }
    val pickupReadyOrders = assignedOrders.count { it.nextAllowedActions.contains(LiveOrderAction.DriverMarkPickedUp) }
    val deliveryReadyOrders = assignedOrders.count { it.nextAllowedActions.contains(LiveOrderAction.DriverMarkDelivered) }
    val problemOrders = orders.count { it.activeIncident || it.requiresHumanReview }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PediloBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            DriverHeader(
                title = "Repartidor",
                subtitle = "Pedidos disponibles para tomar y pedidos asignados a tu cuenta",
                action = "Cerrar sesión",
                onAction = onSignOutConfirmed,
            )
        }
        if (message.isNotBlank()) item { DriverInfoCard("Resultado", message, PediloGreen) }
        if (error.isNotBlank()) item { DriverInfoCard("Error", error, PediloWarning) }
        runningAction?.let { action ->
            item {
                DriverInfoCard(
                    "Procesando acción",
                    "${action.action.driverLabel()} en curso. Esperá la confirmación antes de tocar otra acción del pedido.",
                    PediloOrange,
                )
            }
        }
        if (isClosingCashbox) {
            item { DriverInfoCard("Cerrando caja", "Estamos registrando el cierre operativo. Esperá el resultado antes de volver a cerrarla.", PediloOrange) }
        }
        if (selectedOrderId == null) {
            item {
                DriverWorkPlanCard(
                    title = "Qué hacer ahora",
                    steps = listOf(
                        "Tomá un pedido disponible sólo si podés cumplirlo.",
                        "Marcá retirado al salir del local.",
                        "Marcá entregado al dejar el pedido y cerrá caja al finalizar cobros.",
                    ),
                )
            }
            item {
                DriverMetricsRow(
                    available = availableOrders.size,
                    assigned = assignedOrders.size,
                    pickupReady = pickupReadyOrders,
                    deliveryReady = deliveryReadyOrders,
                    problems = problemOrders,
                )
            }
            item {
                DriverInfoCard(
                    "Capacidad y caja",
                    "La capacidad se valida al tomar pedidos. El cierre de caja registra cobros entregados sin proveedor externo.",
                    PediloOrange,
                )
            }
            item {
                DriverCashboxCard(onClose = {
                    cashboxDialogOpen = true
                    cashboxNote = ""
                })
            }
            if (orders.isEmpty()) {
                item {
                    DriverEmptyState(
                        title = "Sin pedidos para operar",
                        message = "Cuando un local solicite repartidor, el pedido va a aparecer en Disponibles. Los pedidos tomados por tu cuenta quedan separados en Asignados.",
                    )
                }
            } else {
                if (availableOrders.isNotEmpty()) {
                    item { DriverSectionHeader("Disponibles", "Listos para tomar por el repartidor activo.") }
                    items(availableOrders) { order ->
                        DriverOrderCard(order = order, onClick = {
                            selectedOrderId = order.id
                            message = ""
                            error = ""
                        })
                    }
                }
                if (assignedOrders.isNotEmpty()) {
                    item { DriverSectionHeader("Asignados", "Pedidos ya tomados por tu cuenta.") }
                    items(assignedOrders) { order ->
                        DriverOrderCard(order = order, onClick = {
                            selectedOrderId = order.id
                            message = ""
                            error = ""
                        })
                    }
                }
            }
        } else {
            item {
                TextButton(onClick = {
                    selectedOrderId = null
                    detail = null
                    message = ""
                    error = ""
                }) {
                    Text("Volver")
                }
            }
            val current = detail
            if (current == null) {
                item { DriverInfoCard("Pedido", "Cargando pedido.", PediloMuted) }
            } else {
                item { DriverOrderDetailCard(current) }
                item {
                    DriverWorkPlanCard(
                        title = "Siguiente paso",
                        steps = current.nextAllowedActions.map { it.driverLabel() }.ifEmpty {
                            listOf("No hay acciones disponibles ahora. Revisá si el pedido está cerrado o necesita intervención Admin.")
                        },
                    )
                }
                item {
                    DriverInfoCard(
                        "Estado operativo",
                        "Tipo: ${current.orderType.driverOrderTypeLabel()}\nReal: ${current.operationalStatus.ifBlank { "No informado" }}\nVersión: ${current.version}\nAcción necesaria: ${current.driverActionNeeded()}\nAcciones permitidas por backend: ${current.nextAllowedActions.joinToString { it.driverLabel() }.ifBlank { "Ninguna" }}",
                        PediloOrange,
                    )
                }
                item {
                    DriverInfoCard(
                        "Incidencia",
                        if (current.activeIncident) "Hay una incidencia activa. Operá solo las acciones que siga habilitando el backend." else "Sin incidencia activa visible para este pedido.",
                        if (current.activeIncident) PediloWarning else PediloMuted,
                    )
                }
                item {
                    DriverInfoCard(
                        "Comunicación",
                        current.communicationStatus.driverCommunicationLabel(),
                        if (current.communicationStatus == "disabled") PediloMuted else PediloOrange,
                    )
                }
                if (current.assistanceSummary.isNotBlank()) {
                    item {
                        DriverInfoCard(
                            "Ayuda operativa",
                            current.assistanceSummary,
                            if (current.requiresHumanReview) PediloWarning else PediloMuted,
                        )
                    }
                }
                item {
                    DriverInfoCard(
                        "Capacidad",
                        "La toma de pedidos respeta la capacidad activa registrada para tu cuenta.",
                        PediloOrange,
                    )
                }
                item {
                    DriverInfoCard(
                        "Cobro y caja",
                        current.driverFinancialSummary(),
                        if (current.collectionRequired) PediloOrange else PediloMuted,
                    )
                }
                if (current.nextAllowedActions.isEmpty()) {
                    item {
                        DriverInfoCard(
                            "Acciones",
                            "El sistema no habilita acciones para este pedido, versión o estado cerrado.",
                            PediloMuted,
                        )
                    }
                } else {
                    items(current.nextAllowedActions) { action ->
                        DriverActionCard(action = action, onClick = {
                            if (runningAction == null) {
                                pendingAction = PendingDriverAction(current.id, action, current.version)
                                pendingReason = ""
                            }
                        })
                    }
                }
            }
        }
    }

    pendingAction?.let { pending ->
        val requiresReason = pending.action.requiresDriverReason()
        val isRunning = runningAction != null
        AlertDialog(
            onDismissRequest = {
                if (!isRunning) {
                    pendingAction = null
                    pendingReason = ""
                }
            },
            title = { Text("Confirmar acción") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(pending.action.driverLabel())
                    Text(pending.action.driverImpact())
                    if (requiresReason) {
                        PediloTextField(
                            value = pendingReason,
                            onValueChange = { if (!isRunning) pendingReason = it },
                            label = "Motivo operativo",
                            singleLine = false,
                        )
                    }
                    if (isRunning) {
                        Text("Estamos esperando respuesta del backend para esta versión del pedido.", color = PediloMuted)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isRunning && (!requiresReason || pendingReason.trim().length >= 4),
                    onClick = { executeAction(pending, pendingReason.trim()) },
                ) {
                    Text(if (isRunning) "Procesando" else "Confirmar")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isRunning,
                    onClick = {
                        pendingAction = null
                        pendingReason = ""
                    },
                ) {
                    Text("Cancelar")
                }
            },
        )
    }

    if (cashboxDialogOpen) {
        AlertDialog(
            onDismissRequest = {
                if (!isClosingCashbox) {
                    cashboxDialogOpen = false
                    cashboxNote = ""
                }
            },
            title = { Text("Cerrar caja") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Registra cobros entregados como cierre operativo pendiente de revisión Admin.")
                    PediloTextField(
                        value = cashboxNote,
                        onValueChange = { if (!isClosingCashbox) cashboxNote = it },
                        label = "Nota de cierre",
                        singleLine = false,
                    )
                    if (isClosingCashbox) {
                        Text("Cierre en curso. Esperá respuesta antes de repetir la acción.", color = PediloMuted)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isClosingCashbox && cashboxNote.trim().length >= 4,
                    onClick = { closeCashbox(cashboxNote.trim()) },
                ) {
                    Text(if (isClosingCashbox) "Cerrando" else "Cerrar caja")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isClosingCashbox,
                    onClick = {
                        cashboxDialogOpen = false
                        cashboxNote = ""
                    },
                ) {
                    Text("Cancelar")
                }
            },
        )
    }
}

@Composable
private fun DriverHeader(title: String, subtitle: String, action: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PediloPanel, RoundedCornerShape(14.dp))
            .border(1.dp, PediloLine, RoundedCornerShape(14.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            Text(title, color = PediloText, fontSize = 26.sp, lineHeight = 30.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = PediloMuted, fontSize = 13.sp, lineHeight = 17.sp)
        }
        TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable
private fun DriverOrderCard(order: DriverOrderSummary, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PediloPanelSoft, RoundedCornerShape(14.dp))
            .border(1.dp, PediloLine, RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Pedido #${order.visibleNumber}", color = PediloText, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Text(order.orderType.driverOrderTypeLabel(), color = PediloMuted, fontSize = 12.sp)
        Text(order.publicStatus.ifBlank { order.operationalStatus }, color = PediloOrange, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(order.storeLabel.ifBlank { "Local no informado" }, color = PediloMuted, fontSize = 13.sp)
        Text("Siguiente: ${order.driverNextStepLabel()}", color = PediloText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(order.itemsSummary.joinToString(" · ").ifBlank { "Productos no informados" }, color = PediloMuted, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(if (order.isAssignedToCurrentDriver) "Asignado a tu cuenta" else "Disponible para tomar", color = PediloText, fontSize = 12.sp)
        Text("${order.nextAllowedActions.size} acciones disponibles", color = PediloMuted, fontSize = 12.sp)
    }
}

@Composable
private fun DriverMetricsRow(available: Int, assigned: Int, pickupReady: Int, deliveryReady: Int, problems: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DriverMetricTile("Disp.", available.toString(), PediloOrange, Modifier.weight(1f))
            DriverMetricTile("Asig.", assigned.toString(), PediloGreen, Modifier.weight(1f))
            DriverMetricTile("Retiro", pickupReady.toString(), PediloOrange, Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DriverMetricTile("Entrega", deliveryReady.toString(), PediloGreen, Modifier.weight(1f))
            DriverMetricTile("Rev.", problems.toString(), PediloWarning, Modifier.weight(1f))
        }
    }
}

@Composable
private fun DriverMetricTile(title: String, value: String, tone: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(PediloPanelSoft, RoundedCornerShape(12.dp))
            .border(1.dp, tone.copy(alpha = 0.30f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, color = PediloMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, color = tone, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun DriverSectionHeader(title: String, note: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, color = PediloText, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Text(note, color = PediloMuted, fontSize = 13.sp)
    }
}

@Composable
private fun DriverWorkPlanCard(title: String, steps: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PediloPanel, RoundedCornerShape(14.dp))
            .border(1.dp, PediloOrange.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, color = PediloText, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        steps.forEachIndexed { index, step ->
            Text("${index + 1}. $step", color = PediloMuted, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun DriverEmptyState(title: String, message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PediloPanelSoft, RoundedCornerShape(14.dp))
            .border(1.dp, PediloLine, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, color = PediloText, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Text(message, color = PediloMuted, fontSize = 13.sp, lineHeight = 18.sp)
        Text("La capacidad se controla al tomar el pedido; si no aparece el botón, el backend no habilitó la acción.", color = PediloOrange, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DriverOrderDetailCard(order: DriverOrderDetail) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PediloPanelSoft, RoundedCornerShape(14.dp))
            .border(1.dp, PediloLine, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Pedido #${order.visibleNumber}", color = PediloText, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Text("Tipo: ${order.orderType.driverOrderTypeLabel()}", color = PediloMuted, fontSize = 13.sp)
        Text(order.publicStatus.ifBlank { order.operationalStatus }, color = PediloOrange, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text("Local: ${order.storeLabel.ifBlank { "No informado" }}", color = PediloMuted, fontSize = 13.sp)
        Text("Persona: ${order.contactName.ifBlank { "No informado" }}", color = PediloMuted, fontSize = 13.sp)
        Text("Teléfono: ${order.contactPhone.ifBlank { "No informado" }}", color = PediloMuted, fontSize = 13.sp)
        Text("Entrega: ${order.deliveryAddress.ifBlank { "No informado" }}", color = PediloMuted, fontSize = 13.sp)
        order.itemsSummary.forEach {
            Text(it, color = PediloText, fontSize = 14.sp, lineHeight = 18.sp)
        }
        Text("Total: ${order.total.asMoneyLabel()}", color = PediloMuted, fontSize = 13.sp)
        Text("Pago: ${order.paymentMethod.paymentMethodLabel()} · ${order.financialStatus.financialStatusLabel()}", color = PediloMuted, fontSize = 13.sp)
        Text("Comunicación: ${order.communicationStatus.driverCommunicationLabel()}", color = PediloMuted, fontSize = 13.sp)
        if (order.assistanceSummary.isNotBlank()) {
            Text("Ayuda: ${order.assistanceSummary}", color = PediloMuted, fontSize = 13.sp)
        }
        if (order.collectionRequired) {
            Text("A cobrar: ${order.amountToCollect.asMoneyLabel()} · responsable ${order.cashResponsibleRole.ifBlank { "no asignado" }}", color = PediloOrange, fontSize = 13.sp)
        }
    }
}

@Composable
private fun DriverCashboxCard(onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PediloPanel, RoundedCornerShape(14.dp))
            .border(1.dp, PediloOrange.copy(alpha = 0.42f), RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClose)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Cerrar caja operativa", color = PediloText, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        Text("Declara cobros de pedidos entregados y los deja pendientes de revisión Admin.", color = PediloMuted, fontSize = 13.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun DriverActionCard(action: LiveOrderAction, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PediloPanel, RoundedCornerShape(14.dp))
            .border(1.dp, PediloOrange.copy(alpha = 0.42f), RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(action.driverLabel(), color = PediloText, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        Text(action.driverImpact(), color = PediloMuted, fontSize = 13.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun DriverInfoCard(title: String, message: String, tone: androidx.compose.ui.graphics.Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(tone.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
            .border(1.dp, tone.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, color = tone, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        Text(message, color = PediloText, fontSize = 14.sp, lineHeight = 18.sp)
    }
}

private fun LiveOrderAction.driverLabel(): String =
    when (this) {
        LiveOrderAction.DriverTake -> "Tomar pedido"
        LiveOrderAction.DriverMarkPickedUp -> "Marcar retirado"
        LiveOrderAction.DriverMarkDelivered -> "Marcar entregado"
        LiveOrderAction.OpenIncident -> "Reportar incidencia"
        LiveOrderAction.CancelOrder -> "Cancelar pedido"
        else -> "Acción no disponible"
    }

private fun LiveOrderAction.driverImpact(): String =
    when (this) {
        LiveOrderAction.DriverTake -> "Asigna el pedido a tu cuenta operativa."
        LiveOrderAction.DriverMarkPickedUp -> "Confirma que retiraste el pedido del local."
        LiveOrderAction.DriverMarkDelivered -> "Cierra la entrega como completada."
        LiveOrderAction.OpenIncident -> "Manda el pedido a revisión operativa."
        LiveOrderAction.CancelOrder -> "Cierra el pedido con motivo auditado."
        else -> "El sistema no habilitó esta acción para el repartidor."
    }

private fun LiveOrderAction.requiresDriverReason(): Boolean =
    this in setOf(LiveOrderAction.OpenIncident, LiveOrderAction.CancelOrder)

private fun String.driverOrderTypeLabel(): String =
    when (trim()) {
        "local_order" -> "Pedido local"
        "direct_purchase" -> "Compra directa"
        "pickup_shipping" -> "Retiro y envío"
        else -> "Tipo no informado"
    }

private fun DriverOrderDetail.driverActionNeeded(): String =
    when {
        activeIncident -> "Revisión por incidencia activa"
        nextAllowedActions.contains(LiveOrderAction.DriverTake) -> "Tomar pedido"
        nextAllowedActions.contains(LiveOrderAction.DriverMarkPickedUp) -> "Marcar retirado"
        nextAllowedActions.contains(LiveOrderAction.DriverMarkDelivered) -> "Marcar entregado"
        nextAllowedActions.contains(LiveOrderAction.OpenIncident) -> "Operar o reportar incidencia si corresponde"
        nextAllowedActions.contains(LiveOrderAction.CancelOrder) -> "Operar o cancelar con motivo si corresponde"
        else -> "Sin acción disponible para repartidor"
    }

private fun DriverOrderDetail.driverFinancialSummary(): String =
    if (collectionRequired) {
        "Cobro operativo requerido: ${amountToCollect.asMoneyLabel()} al recibir. El cierre de caja se registra desde la pantalla principal."
    } else {
        "${paymentMethod.paymentMethodLabel()} · ${financialStatus.financialStatusLabel()}. Sin cobro al recibir para caja del repartidor."
    }

private fun DriverOrderSummary.driverNextStepLabel(): String =
    when {
        nextAllowedActions.contains(LiveOrderAction.DriverTake) -> "tomar pedido"
        nextAllowedActions.contains(LiveOrderAction.DriverMarkPickedUp) -> "marcar retirado"
        nextAllowedActions.contains(LiveOrderAction.DriverMarkDelivered) -> "marcar entregado"
        nextAllowedActions.contains(LiveOrderAction.OpenIncident) -> "operar o reportar incidencia"
        activeIncident -> "revisar incidencia"
        requiresHumanReview -> "revisión humana"
        else -> "sin acción de reparto disponible"
    }

private fun String.paymentMethodLabel(): String =
    when (trim()) {
        "cash" -> "Efectivo"
        "transfer" -> "Transferencia declarada"
        "already_paid" -> "Pago declarado"
        else -> "Pago en revisión"
    }

private fun String.financialStatusLabel(): String =
    when (trim()) {
        "collect_on_delivery" -> "Cobro en entrega"
        "transfer_declared_pending" -> "Transferencia pendiente"
        "paid_declared" -> "Pago declarado"
        "pending_review" -> "Revisión financiera"
        else -> ifBlank { "Estado financiero no informado" }
    }

private fun String.driverCommunicationLabel(): String =
    when (trim()) {
        "received" -> "Recibida"
        "pending" -> "Pendiente"
        "prepared" -> "Preparada, sin envío externo real"
        "sent" -> "Registrada como enviada; verificar canal"
        "failed" -> "Fallida"
        "closed" -> "Cerrada"
        "disabled" -> "Canal externo deshabilitado"
        else -> ifBlank { "Sin estado de comunicación" }
    }

private fun String.asMoneyLabel(): String {
    val cents = toLongOrNull() ?: return ifBlank { "No informado" }
    return "\$${cents / 100}"
}

private fun Long.driverCentsLabel(): String =
    "\$${this / 100}"

private fun CoreError.driverErrorMessage(): String =
    when (this) {
        is CoreError.Operational -> humanMessage
        CoreError.NotAvailable -> "No pudimos cargar pedidos visibles para este repartidor activo. Revisá conexión o volvé a iniciar sesión si la cuenta venció."
        CoreError.IncompleteData -> "Faltan datos para operar el pedido."
        is CoreError.Validation -> "Revisá los datos antes de confirmar."
        CoreError.Unknown -> "No pudimos completar la operación."
    }
