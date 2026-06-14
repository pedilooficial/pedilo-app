package com.pedilo.app.ui.store

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.pedilo.app.core.model.LiveOrderAction
import com.pedilo.app.core.model.PublicProductSummary
import com.pedilo.app.core.model.StoreOrderDetail
import com.pedilo.app.core.model.StoreOrderSummary
import com.pedilo.app.core.result.CoreError
import com.pedilo.app.core.result.CoreResult
import com.pedilo.app.core.runtime.storeOrdersUseCase
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

private data class PendingStoreAction(
    val orderId: String,
    val action: LiveOrderAction,
    val expectedVersion: Int,
)

@Composable
fun StoreApp(onSignOutConfirmed: () -> Unit) {
    val storeOrders = remember { storeOrdersUseCase() }
    val scope = rememberCoroutineScope()
    var orders by remember { mutableStateOf<List<StoreOrderSummary>>(emptyList()) }
    var products by remember { mutableStateOf<List<PublicProductSummary>>(emptyList()) }
    var selectedOrderId by remember { mutableStateOf<String?>(null) }
    var detail by remember { mutableStateOf<StoreOrderDetail?>(null) }
    var message by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var pendingAction by remember { mutableStateOf<PendingStoreAction?>(null) }
    var pendingReason by remember { mutableStateOf("") }
    var runningAction by remember { mutableStateOf<PendingStoreAction?>(null) }

    fun refreshDetail(orderId: String) {
        scope.launch {
            when (val result = storeOrders.getDetail(orderId)) {
                is CoreResult.Success -> detail = result.value
                is CoreResult.Failure -> error = result.error.storeErrorMessage()
            }
        }
    }

    fun refreshProducts() {
        scope.launch {
            when (val result = storeOrders.getProducts()) {
                is CoreResult.Success -> products = result.value
                is CoreResult.Failure -> error = result.error.storeErrorMessage()
            }
        }
    }

    fun executeAction(pending: PendingStoreAction, reason: String) {
        if (runningAction != null) return
        runningAction = pending
        scope.launch {
            message = ""
            error = ""
            when (val result = storeOrders.execute(
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
                    error = result.error.storeErrorMessage()
                    pendingAction = null
                    pendingReason = ""
                    runningAction = null
                    refreshDetail(pending.orderId)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshProducts()
        storeOrders.observe().collect { result ->
            when (result) {
                is CoreResult.Success -> orders = result.value
                is CoreResult.Failure -> {
                    orders = emptyList()
                    error = result.error.storeErrorMessage()
                }
            }
        }
    }

    selectedOrderId?.let { LaunchedEffect(it) { refreshDetail(it) } }

    val waitingOrders = orders.count { it.nextAllowedActions.contains(LiveOrderAction.LocalAccept) || it.nextAllowedActions.contains(LiveOrderAction.LocalReject) }
    val preparingOrders = orders.count { it.nextAllowedActions.contains(LiveOrderAction.LocalMarkReady) }
    val readyForDriverOrders = orders.count { it.nextAllowedActions.contains(LiveOrderAction.StoreDriverRequest) }
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
            StoreHeader(
                title = "Local",
                subtitle = "Pedidos propios asignados a tu cuenta",
                action = "Cerrar sesión",
                onAction = onSignOutConfirmed,
            )
        }
        if (message.isNotBlank()) item { StoreInfoCard("Resultado", message, PediloGreen) }
        if (error.isNotBlank()) item { StoreInfoCard("Error", error, PediloWarning) }
        runningAction?.let { action ->
            item {
                StoreInfoCard(
                    "Procesando acción",
                    "${action.action.storeLabel()} en curso. Esperá la confirmación antes de tocar otra acción del pedido.",
                    PediloOrange,
                )
            }
        }
        if (selectedOrderId == null) {
            item {
                StoreWorkPlanCard(
                    title = "Qué hacer ahora",
                    steps = listOf(
                        "Aceptá o rechazá pedidos nuevos.",
                        "Marcá preparación y listo cuando cambie el estado real.",
                        "Solicitá repartidor sólo cuando el botón aparezca en el pedido.",
                    ),
                )
            }
            item {
                StoreMetricsRow(
                    waiting = waitingOrders,
                    preparing = preparingOrders,
                    readyForDriver = readyForDriverOrders,
                    problems = problemOrders,
                )
            }
            if (orders.isEmpty()) {
                item {
                    StoreEmptyState(
                        title = "Sin pedidos para operar",
                        message = "Cuando entre un pedido de este local, va a aparecer acá con su estado, versión y acciones permitidas. No hace falta refrescar manualmente.",
                    )
                }
            } else {
                item {
                    StoreSectionHeader(
                        title = "Pedidos propios",
                        note = "${orders.size} visibles para esta cuenta",
                    )
                }
                items(orders) { order ->
                    StoreOrderCard(order = order, onClick = {
                        selectedOrderId = order.id
                        message = ""
                        error = ""
                    })
                }
            }
            item { StoreCatalogPanel(products = products, onRefresh = { refreshProducts() }) }
            item { StoreInfoCard("Solicitud de repartidor", "Disponible dentro de cada pedido cuando el sistema habilita la acción.", PediloOrange) }
            item { StoreInfoCard("Finanzas", "El pedido trae estado financiero y cobro operativo. La revisión bancaria sigue fuera de esta pantalla.", PediloMuted) }
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
                item { StoreInfoCard("Pedido", "Cargando pedido.", PediloMuted) }
            } else {
                item { StoreOrderDetailCard(current) }
                item {
                    StoreWorkPlanCard(
                        title = "Siguiente paso",
                        steps = current.nextAllowedActions.map { it.storeLabel() }.ifEmpty {
                            listOf("No hay acciones disponibles ahora. Revisá estado, comunicación e historial desde Admin si hace falta.")
                        },
                    )
                }
                item {
                    StoreInfoCard(
                        "Comunicación",
                        current.communicationStatus.storeCommunicationLabel(),
                        if (current.communicationStatus == "disabled") PediloMuted else PediloOrange,
                    )
                }
                if (current.assistanceSummary.isNotBlank()) {
                    item {
                        StoreInfoCard(
                            "Ayuda operativa",
                            current.assistanceSummary,
                            if (current.requiresHumanReview) PediloWarning else PediloMuted,
                        )
                    }
                }
                if (current.activeIncident) {
                    item { StoreInfoCard("Incidencia activa", "El pedido está bajo revisión operativa.", PediloWarning) }
                }
                if (current.nextAllowedActions.isEmpty()) {
                    item { StoreInfoCard("Sin acciones disponibles", "El sistema no habilita acciones para este pedido o versión. Si el pedido está cerrado, no hay acciones normales.", PediloMuted) }
                } else {
                    item { StoreInfoCard("Acciones", "Permitidas para la versión ${current.version}.", PediloOrange) }
                    items(current.nextAllowedActions) { action ->
                        StoreActionCard(action = action, onClick = {
                            if (runningAction == null) {
                                pendingAction = PendingStoreAction(current.id, action, current.version)
                                pendingReason = ""
                            }
                        })
                    }
                }
            }
        }
    }

    pendingAction?.let { pending ->
        val requiresReason = pending.action.requiresStoreReason()
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
                    Text(pending.action.storeLabel())
                    Text(pending.action.storeImpact())
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
}

@Composable
private fun StoreHeader(title: String, subtitle: String, action: String, onAction: () -> Unit) {
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
private fun StoreOrderCard(order: StoreOrderSummary, onClick: () -> Unit) {
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
        Text(order.publicStatus.ifBlank { order.operationalStatus }, color = PediloOrange, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text("Siguiente: ${order.storeNextStepLabel()}", color = PediloText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(order.itemsSummary.joinToString(" · ").ifBlank { "Productos no informados" }, color = PediloMuted, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text("${order.nextAllowedActions.size} acciones disponibles", color = PediloMuted, fontSize = 12.sp)
    }
}

@Composable
private fun StoreMetricsRow(waiting: Int, preparing: Int, readyForDriver: Int, problems: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StoreMetricTile("Nuevos", waiting.toString(), PediloOrange, Modifier.weight(1f))
        StoreMetricTile("Prep.", preparing.toString(), PediloGreen, Modifier.weight(1f))
        StoreMetricTile("Reparto", readyForDriver.toString(), PediloOrange, Modifier.weight(1f))
        StoreMetricTile("Rev.", problems.toString(), PediloWarning, Modifier.weight(1f))
    }
}

@Composable
private fun StoreMetricTile(title: String, value: String, tone: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
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
private fun StoreSectionHeader(title: String, note: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, color = PediloText, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Text(note, color = PediloMuted, fontSize = 13.sp)
    }
}

@Composable
private fun StoreWorkPlanCard(title: String, steps: List<String>) {
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
private fun StoreEmptyState(title: String, message: String) {
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
        Text("Catálogo y estado operativo quedan visibles abajo para revisar que el local esté listo.", color = PediloOrange, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StoreOrderDetailCard(order: StoreOrderDetail) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PediloPanelSoft, RoundedCornerShape(14.dp))
            .border(1.dp, PediloLine, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Pedido #${order.visibleNumber}", color = PediloText, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Text(order.publicStatus.ifBlank { order.operationalStatus }, color = PediloOrange, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text("Estado operativo: ${order.operationalStatus.ifBlank { "No informado" }}", color = PediloMuted, fontSize = 13.sp)
        Text("Persona: ${order.contactName.ifBlank { "No informado" }}", color = PediloMuted, fontSize = 13.sp)
        order.itemsSummary.forEach {
            Text(it, color = PediloText, fontSize = 14.sp, lineHeight = 18.sp)
        }
        Text("Total: ${order.total.ifBlank { "No informado" }}", color = PediloMuted, fontSize = 13.sp)
        Text("Pago: ${order.paymentMethod.storePaymentLabel()} · ${order.financialStatus.storeFinancialLabel()}", color = PediloMuted, fontSize = 13.sp)
        Text("Comunicación: ${order.communicationStatus.storeCommunicationLabel()}", color = PediloMuted, fontSize = 13.sp)
        if (order.assistanceSummary.isNotBlank()) {
            Text("Ayuda: ${order.assistanceSummary}", color = PediloMuted, fontSize = 13.sp)
        }
        if (order.collectionRequired) {
            Text("Cobro al recibir: ${order.amountToCollect.storeMoneyLabel()}", color = PediloOrange, fontSize = 13.sp)
        }
    }
}

@Composable
private fun StoreCatalogPanel(products: List<PublicProductSummary>, onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PediloPanelSoft, RoundedCornerShape(14.dp))
            .border(1.dp, PediloLine, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Productos y stock", color = PediloText, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Text("Catálogo propio leído desde Firestore", color = PediloMuted, fontSize = 12.sp)
            }
            TextButton(onClick = onRefresh) { Text("Actualizar") }
        }
        if (products.isEmpty()) {
            Text("No hay productos cargados para este local.", color = PediloMuted, fontSize = 13.sp, lineHeight = 17.sp)
        } else {
            products.take(8).forEach { product ->
                StoreProductRow(product)
            }
            if (products.size > 8) {
                Text("+${products.size - 8} productos más", color = PediloMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun StoreProductRow(product: PublicProductSummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PediloPanel, RoundedCornerShape(10.dp))
            .border(1.dp, PediloLine, RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(product.name, color = PediloText, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(product.priceCents.storeCatalogMoneyLabel(), color = PediloMuted, fontSize = 12.sp)
        }
        Text(product.storeCatalogStateLabel(), color = product.storeCatalogTone(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StoreActionCard(action: LiveOrderAction, onClick: () -> Unit) {
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
        Text(action.storeLabel(), color = PediloText, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        Text(action.storeImpact(), color = PediloMuted, fontSize = 13.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun StoreInfoCard(title: String, message: String, tone: androidx.compose.ui.graphics.Color) {
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

private fun LiveOrderAction.storeLabel(): String =
    when (this) {
        LiveOrderAction.LocalAccept -> "Aceptar pedido"
        LiveOrderAction.LocalReject -> "Rechazar pedido"
        LiveOrderAction.LocalMarkPreparing -> "Marcar en preparación"
        LiveOrderAction.LocalMarkReady -> "Marcar listo"
        LiveOrderAction.StoreDriverRequest -> "Solicitar repartidor"
        LiveOrderAction.CancelOrder -> "Cancelar pedido"
        LiveOrderAction.OpenIncident -> "Reportar problema"
        else -> "Acción no disponible"
    }

private fun LiveOrderAction.storeImpact(): String =
    when (this) {
        LiveOrderAction.LocalAccept -> "Confirma que el local toma el pedido."
        LiveOrderAction.LocalReject -> "Cierra el pedido con motivo auditado."
        LiveOrderAction.LocalMarkPreparing -> "Informa que el pedido está en preparación."
        LiveOrderAction.LocalMarkReady -> "Deja el pedido listo para retiro."
        LiveOrderAction.StoreDriverRequest -> "Registra la solicitud y deja el pedido disponible para repartidores."
        LiveOrderAction.CancelOrder -> "Cancela el pedido con motivo auditado si el estado lo permite."
        LiveOrderAction.OpenIncident -> "Usalo para producto no disponible, demora o problema operativo con motivo claro."
        else -> "El sistema no habilitó esta acción para el local."
    }

private fun LiveOrderAction.requiresStoreReason(): Boolean =
    this in setOf(LiveOrderAction.LocalReject, LiveOrderAction.CancelOrder, LiveOrderAction.OpenIncident)

private fun String.storePaymentLabel(): String =
    when (trim()) {
        "cash" -> "Efectivo"
        "transfer" -> "Transferencia declarada"
        "already_paid" -> "Pago declarado"
        else -> "Pago en revisión"
    }

private fun String.storeFinancialLabel(): String =
    when (trim()) {
        "collect_on_delivery" -> "Cobro en entrega"
        "transfer_declared_pending" -> "Transferencia pendiente"
        "paid_declared" -> "Pago declarado"
        "pending_review" -> "Revisión financiera"
        else -> ifBlank { "Estado financiero no informado" }
    }

private fun String.storeCommunicationLabel(): String =
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

private fun String.storeMoneyLabel(): String {
    val cents = toLongOrNull() ?: return ifBlank { "No informado" }
    return "\$${cents / 100}"
}

private fun Long?.storeCatalogMoneyLabel(): String =
    this?.let { "\$${it / 100}" } ?: "Sin precio informado"

private fun PublicProductSummary.storeCatalogStateLabel(): String =
    when {
        visible && available -> "Disponible"
        visible && !available -> "Sin stock"
        else -> "Oculto"
    }

private fun PublicProductSummary.storeCatalogTone(): androidx.compose.ui.graphics.Color =
    when {
        visible && available -> PediloGreen
        visible && !available -> PediloOrange
        else -> PediloMuted
    }

private fun StoreOrderSummary.storeNextStepLabel(): String =
    when {
        nextAllowedActions.contains(LiveOrderAction.LocalAccept) -> "aceptar o rechazar"
        nextAllowedActions.contains(LiveOrderAction.LocalMarkPreparing) -> "marcar en preparación"
        nextAllowedActions.contains(LiveOrderAction.LocalMarkReady) -> "marcar listo"
        nextAllowedActions.contains(LiveOrderAction.StoreDriverRequest) -> "solicitar repartidor"
        nextAllowedActions.contains(LiveOrderAction.OpenIncident) -> "operar o reportar problema"
        activeIncident -> "revisar incidencia"
        requiresHumanReview -> "revisión humana"
        else -> "sin acción local disponible"
    }

private fun CoreError.storeErrorMessage(): String =
    when (this) {
        is CoreError.Operational -> humanMessage
        CoreError.NotAvailable -> "No pudimos cargar tus pedidos. Revisá conexión o volvé a iniciar sesión si la cuenta venció."
        CoreError.IncompleteData -> "Faltan datos para operar el pedido."
        is CoreError.Validation -> "Revisá los datos antes de confirmar."
        CoreError.Unknown -> "No pudimos completar la operación."
    }
