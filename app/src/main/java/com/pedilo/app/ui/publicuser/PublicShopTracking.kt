package com.pedilo.app.ui.publicuser

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pedilo.app.core.model.PublicOrderStatus
import com.pedilo.app.core.model.PublicTrackingState
import com.pedilo.app.core.result.CoreError
import com.pedilo.app.core.result.CoreResult
import com.pedilo.app.core.usecase.GetPublicTrackingUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class PublicOrderStep(val label: String) {
    Received("Recibido"),
    Preparing("En preparación"),
    OnTheWay("En camino"),
    Delivered("Entregado"),
}

private data class PublicTrackingData(
    val orderNumber: String,
    val mainStatus: String,
    val subtitle: String,
    val activeStep: PublicOrderStep,
    val eta: String,
    val address: String,
    val city: String,
)

@Composable
fun PublicShopTrackingScreen(
    orderNumber: String,
    current: PublicBottomDestination = PublicBottomDestination.Shop,
    getTracking: GetPublicTrackingUseCase,
    onHome: () -> Unit,
    onPlus: () -> Unit,
    onShop: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var query by remember(orderNumber) { mutableStateOf(orderNumber) }
    var tracking by remember { mutableStateOf<PublicTrackingState?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isCancelling by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var cancellationMessage by remember { mutableStateOf<String?>(null) }
    var cancellationContact by remember { mutableStateOf("") }
    var cancellationReason by remember { mutableStateOf("") }

    fun submitTracking() {
        val clean = normalizePublicTrackingInput(query)
        if (!isValidPublicTrackingNumber(clean)) {
            error = "Ingresá un número de pedido válido."
            tracking = null
            return
        }
        query = clean
        scope.launch {
            isLoading = true
            error = null
            cancellationMessage = null
            val result = withContext(Dispatchers.IO) { getTracking(clean) }
            isLoading = false
            when (result) {
                is CoreResult.Success -> tracking = result.value
                is CoreResult.Failure -> {
                    tracking = null
                    error = result.error.toTrackingMessage()
                }
            }
        }
    }

    fun requestCancellation(state: PublicTrackingState) {
        scope.launch {
            isCancelling = true
            error = null
            cancellationMessage = null
            val result = withContext(Dispatchers.IO) {
                getTracking.requestCancellation(state.trackingNumber, cancellationContact, cancellationReason)
            }
            isCancelling = false
            when (result) {
                is CoreResult.Success -> {
                    cancellationMessage = result.value
                    cancellationContact = ""
                    cancellationReason = ""
                    submitTracking()
                }
                is CoreResult.Failure -> error = result.error.toCancellationMessage()
            }
        }
    }

    LaunchedEffect(orderNumber) {
        if (orderNumber.isNotBlank()) {
            submitTracking()
        }
    }

    PublicShell(
        current = current,
        onHome = onHome,
        onPlus = onPlus,
        onShop = onShop,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .background(PediloBg),
            contentPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { TrackingHeader() }
            item { TrackingLookupForm(query, isLoading, onQueryChange = { query = it }, onSubmit = { submitTracking() }) }
            error?.let { item { TrackingMessageCard("No pudimos consultar", it, TrackingIconKind.Warning) } }
            if (isLoading) {
                item { TrackingMessageCard("Consultando", "Estamos buscando el estado actual del pedido.", TrackingIconKind.Clock) }
            }
            tracking?.let { state ->
                item { OrderNumberCard(state.trackingNumber) }
                if (state.found) {
                    item { TrackingProgressCard(state.toTrackingData()) }
                    item {
                        TrackingInfoCard(
                            icon = TrackingIconKind.Clock,
                            label = "Estado actual",
                            value = state.publicStatus,
                            detail = state.humanMessage,
                        )
                    }
                    if (!state.isClosed && state.summary.isNotBlank()) {
                        item {
                            TrackingInfoCard(
                                icon = TrackingIconKind.Box,
                                label = state.orderType.ifBlank { "Pedido" },
                                value = state.summary,
                                detail = state.storeName.takeIf { it.isNotBlank() },
                            )
                        }
                    }
                    if (state.paymentLabel.isNotBlank() || state.publicTotal.isNotBlank()) {
                        item {
                            TrackingInfoCard(
                                icon = TrackingIconKind.Clock,
                                label = "Pago",
                                value = listOf(state.paymentLabel, state.publicTotal).filter { it.isNotBlank() }.joinToString(" · "),
                                detail = state.collectionMessage.takeIf { it.isNotBlank() },
                            )
                        }
                    }
                    if (state.canCancel) {
                        item {
                            TrackingCancelCard(
                                contact = cancellationContact,
                                reason = cancellationReason,
                                isCancelling = isCancelling,
                                onContactChange = { cancellationContact = it },
                                onReasonChange = { cancellationReason = it },
                                onCancel = { requestCancellation(state) },
                            )
                        }
                    }
                    cancellationMessage?.let {
                        item { TrackingMessageCard("Cancelación", it, TrackingIconKind.Check) }
                    }
                } else {
                    item { TrackingMessageCard(state.publicStatus, state.humanMessage, TrackingIconKind.Warning) }
                }
            }
        }
    }
}

@Composable
private fun TrackingLookupForm(
    value: String,
    isLoading: Boolean,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pediloCardDepth(RoundedCornerShape(15.dp))
            .background(PediloCardBrush, RoundedCornerShape(15.dp))
            .border(1.dp, PediloLine.copy(alpha = 0.90f), RoundedCornerShape(15.dp))
            .padding(14.dp),
    ) {
        Text("Número de pedido", color = PediloMuted, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .background(PediloPanelSoft, RoundedCornerShape(10.dp))
                    .border(1.dp, PediloLine, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isBlank()) {
                    Text("PDL-XXXXXX", color = PediloMuted.copy(alpha = 0.68f), fontSize = 14.sp)
                }
                BasicTextField(
                    value = value,
                    onValueChange = { onQueryChange(normalizePublicTrackingInput(it)) },
                    textStyle = TextStyle(color = PediloText, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    singleLine = true,
                    cursorBrush = SolidColor(PediloText),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .height(46.dp)
                    .width(104.dp)
                    .pediloButtonDepth(RoundedCornerShape(10.dp))
                    .background(PediloPrimaryBrush, RoundedCornerShape(10.dp))
                    .clickable(enabled = isValidPublicTrackingNumber(value) && !isLoading, role = Role.Button, onClick = onSubmit),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (isLoading) "..." else "Consultar", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TrackingHeader() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Seguimiento del pedido",
            fontSize = 28.sp,
            lineHeight = 31.sp,
            fontWeight = FontWeight.ExtraBold,
            style = TextStyle(brush = Brush.verticalGradient(listOf(PediloWarning, PediloOrangeSoft, PediloOrangeDark))),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "Consulta el estado actual de tu pedido",
            color = PediloMuted,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun PublicTrackingState.toTrackingData(): PublicTrackingData =
    PublicTrackingData(
        orderNumber = trackingNumber,
        mainStatus = publicStatus,
        subtitle = humanMessage,
        activeStep = when (status) {
            PublicOrderStatus.RECEIVED -> PublicOrderStep.Received
            PublicOrderStatus.PREPARING -> PublicOrderStep.Preparing
            PublicOrderStatus.ON_THE_WAY -> PublicOrderStep.OnTheWay
            PublicOrderStatus.DELIVERED -> PublicOrderStep.Delivered
            PublicOrderStatus.CANCELLED,
            PublicOrderStatus.UNDER_REVIEW -> PublicOrderStep.Received
        },
        eta = publicStatus,
        address = summary,
        city = orderType,
    )

private fun CoreError.toTrackingMessage(): String = when (this) {
    is CoreError.Validation -> "Ingresá un número de pedido válido."
    is CoreError.Operational -> "No pudimos consultar el pedido. Probá de nuevo."
    CoreError.IncompleteData -> "Ingresá el número de pedido."
    CoreError.NotAvailable -> "No pudimos consultar el pedido. Probá de nuevo."
    CoreError.Unknown -> "Ocurrió un error al consultar el pedido."
}

private fun CoreError.toCancellationMessage(): String = when (this) {
    is CoreError.Validation -> "Revisá teléfono y motivo."
    is CoreError.Operational -> humanMessage
    CoreError.IncompleteData -> "Ingresá teléfono y motivo para cancelar."
    CoreError.NotAvailable -> "No pudimos cancelar el pedido. Probá de nuevo."
    CoreError.Unknown -> "No pudimos cancelar el pedido."
}

@Composable
private fun TrackingMessageCard(title: String, message: String, icon: TrackingIconKind) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pediloCardDepth(RoundedCornerShape(15.dp))
            .background(PediloCardBrush, RoundedCornerShape(15.dp))
            .border(1.dp, PediloLine.copy(alpha = 0.90f), RoundedCornerShape(15.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .background(PediloPanelSoft, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            TrackingIcon(icon, tint = PediloOrange, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = PediloText, fontSize = 18.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold)
            Text(message, color = PediloMuted, fontSize = 13.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun TrackingCancelCard(
    contact: String,
    reason: String,
    isCancelling: Boolean,
    onContactChange: (String) -> Unit,
    onReasonChange: (String) -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pediloCardDepth(RoundedCornerShape(15.dp))
            .background(PediloCardBrush, RoundedCornerShape(15.dp))
            .border(1.dp, PediloWarning.copy(alpha = 0.5f), RoundedCornerShape(15.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Cancelar pedido", color = PediloWarning, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        Text("Disponible solo mientras el pedido lo permite. Se valida con el teléfono usado al pedir.", color = PediloMuted, fontSize = 13.sp, lineHeight = 17.sp)
        TrackingSmallInput(value = contact, hint = "Teléfono del pedido", onValueChange = onContactChange, singleLine = true)
        TrackingSmallInput(value = reason, hint = "Motivo de cancelación", onValueChange = onReasonChange, singleLine = false)
        TrackingActionButton(
            label = if (isCancelling) "Cancelando..." else "Confirmar cancelación",
            icon = TrackingIconKind.Cancel,
            filled = false,
            enabled = !isCancelling && contact.trim().length >= 8 && reason.trim().length >= 4,
            onClick = onCancel,
        )
    }
}

@Composable
private fun TrackingSmallInput(
    value: String,
    hint: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (singleLine) 46.dp else 74.dp)
            .background(PediloPanelSoft, RoundedCornerShape(10.dp))
            .border(1.dp, PediloLine, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        if (value.isBlank()) {
            Text(hint, color = PediloMuted.copy(alpha = 0.68f), fontSize = 14.sp)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = PediloText, fontSize = 14.sp, lineHeight = 18.sp),
            singleLine = singleLine,
            cursorBrush = SolidColor(PediloText),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun OrderNumberCard(orderNumber: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pediloCardDepth(RoundedCornerShape(15.dp))
            .background(PediloCardBrush, RoundedCornerShape(15.dp))
            .border(1.dp, PediloLine.copy(alpha = 0.90f), RoundedCornerShape(15.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Número de pedido", color = PediloMuted, fontSize = 13.sp)
            Text(orderNumber, color = PediloText, fontSize = 25.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold)
        }
        TrackingIcon(TrackingIconKind.Copy, tint = PediloOrange, modifier = Modifier.size(30.dp))
    }
}

@Composable
private fun TrackingProgressCard(tracking: PublicTrackingData) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pediloCardDepth(RoundedCornerShape(16.dp))
            .background(PediloWarmPanelBrush, RoundedCornerShape(16.dp))
            .border(1.dp, PediloGoldLine.copy(alpha = 0.62f), RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tracking.mainStatus,
                    color = PediloOrange,
                    fontSize = 28.sp,
                    lineHeight = 31.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(7.dp))
                Text(tracking.subtitle, color = PediloMuted, fontSize = 14.sp)
            }
            TrackingIcon(TrackingIconKind.Scooter, tint = PediloOrange, modifier = Modifier.size(74.dp))
        }
        Spacer(Modifier.height(22.dp))
        TrackingSteps(activeStep = tracking.activeStep)
    }
}

@Composable
private fun TrackingSteps(activeStep: PublicOrderStep) {
    val steps = PublicOrderStep.entries
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        steps.forEachIndexed { index, step ->
            val passed = step.ordinal < activeStep.ordinal
            val active = step == activeStep
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .background(if (passed || active) PediloOrange else PediloLine),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(if (active) 54.dp else 44.dp)
                            .clip(CircleShape)
                            .background(if (passed) PediloOrange else if (active) PediloPanel else PediloLine)
                            .border(2.dp, if (active) PediloOrange else Color.Transparent, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        TrackingIcon(
                            kind = if (step == PublicOrderStep.Delivered) TrackingIconKind.Box else if (active) TrackingIconKind.Scooter else TrackingIconKind.Check,
                            tint = if (passed) Color.White else if (active) PediloOrange else PediloMuted,
                            modifier = Modifier.size(if (active) 28.dp else 22.dp),
                        )
                    }
                    if (index < steps.lastIndex) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .background(if (passed) PediloOrange else PediloLine),
                        )
                    }
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    text = step.label,
                    color = if (active) PediloOrange else PediloMuted,
                    fontSize = 10.sp,
                    lineHeight = 11.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun TrackingInfoCard(
    icon: TrackingIconKind,
    label: String,
    value: String,
    detail: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pediloCardDepth(RoundedCornerShape(15.dp))
            .background(PediloCardBrush, RoundedCornerShape(15.dp))
            .border(1.dp, PediloLine.copy(alpha = 0.90f), RoundedCornerShape(15.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .background(PediloPanelSoft, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            TrackingIcon(icon, tint = PediloOrange, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = PediloMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, color = PediloText, fontSize = 22.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold)
            detail?.let {
                Text(it, color = PediloMuted, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun TrackingActionButton(
    label: String,
    icon: TrackingIconKind,
    filled: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .pediloButtonDepth(RoundedCornerShape(13.dp))
            .background(
                if (filled) PediloPrimaryBrush else Brush.verticalGradient(listOf(PediloBg, PediloBg)),
                RoundedCornerShape(13.dp),
            )
            .border(1.dp, PediloOrange, RoundedCornerShape(13.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        TrackingIcon(icon, tint = if (filled) Color.White else PediloOrange, modifier = Modifier.size(25.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = if (filled) Color.White else PediloOrange, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

private enum class TrackingIconKind {
    Check,
    Scooter,
    Box,
    Clock,
    Pin,
    Cancel,
    Warning,
    Copy,
}

@Composable
private fun TrackingIcon(kind: TrackingIconKind, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = size.minDimension * 0.1f, cap = StrokeCap.Round)
        when (kind) {
            TrackingIconKind.Check -> {
                drawLine(tint, Offset(size.width * 0.24f, size.height * 0.52f), Offset(size.width * 0.42f, size.height * 0.7f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.42f, size.height * 0.7f), Offset(size.width * 0.78f, size.height * 0.28f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            TrackingIconKind.Scooter -> {
                drawCircle(tint, size.minDimension * 0.12f, Offset(size.width * 0.27f, size.height * 0.72f), style = stroke)
                drawCircle(tint, size.minDimension * 0.12f, Offset(size.width * 0.72f, size.height * 0.72f), style = stroke)
                drawLine(tint, Offset(size.width * 0.3f, size.height * 0.7f), Offset(size.width * 0.55f, size.height * 0.5f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.55f, size.height * 0.5f), Offset(size.width * 0.72f, size.height * 0.7f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.52f, size.height * 0.36f), Offset(size.width * 0.74f, size.height * 0.36f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.58f, size.height * 0.36f), Offset(size.width * 0.5f, size.height * 0.2f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            TrackingIconKind.Box -> {
                val box = Path().apply {
                    moveTo(size.width * 0.5f, size.height * 0.16f)
                    lineTo(size.width * 0.82f, size.height * 0.34f)
                    lineTo(size.width * 0.82f, size.height * 0.68f)
                    lineTo(size.width * 0.5f, size.height * 0.86f)
                    lineTo(size.width * 0.18f, size.height * 0.68f)
                    lineTo(size.width * 0.18f, size.height * 0.34f)
                    close()
                }
                drawPath(box, tint, style = stroke)
                drawLine(tint, Offset(size.width * 0.18f, size.height * 0.34f), Offset(size.width * 0.5f, size.height * 0.52f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.82f, size.height * 0.34f), Offset(size.width * 0.5f, size.height * 0.52f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.5f, size.height * 0.52f), Offset(size.width * 0.5f, size.height * 0.86f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            TrackingIconKind.Clock -> {
                drawCircle(tint, size.minDimension * 0.36f, Offset(size.width * 0.5f, size.height * 0.5f), style = stroke)
                drawLine(tint, Offset(size.width * 0.5f, size.height * 0.28f), Offset(size.width * 0.5f, size.height * 0.52f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.5f, size.height * 0.52f), Offset(size.width * 0.66f, size.height * 0.62f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            TrackingIconKind.Pin -> {
                drawCircle(tint, size.minDimension * 0.34f, Offset(size.width * 0.5f, size.height * 0.4f), style = stroke)
                drawCircle(tint, size.minDimension * 0.08f, Offset(size.width * 0.5f, size.height * 0.4f))
                drawLine(tint, Offset(size.width * 0.5f, size.height * 0.72f), Offset(size.width * 0.5f, size.height * 0.9f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            TrackingIconKind.Cancel -> {
                drawCircle(tint, size.minDimension * 0.36f, Offset(size.width * 0.5f, size.height * 0.5f), style = stroke)
                drawLine(tint, Offset(size.width * 0.34f, size.height * 0.34f), Offset(size.width * 0.66f, size.height * 0.66f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.66f, size.height * 0.34f), Offset(size.width * 0.34f, size.height * 0.66f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            TrackingIconKind.Warning -> {
                val triangle = Path().apply {
                    moveTo(size.width * 0.5f, size.height * 0.16f)
                    lineTo(size.width * 0.85f, size.height * 0.78f)
                    lineTo(size.width * 0.15f, size.height * 0.78f)
                    close()
                }
                drawPath(triangle, tint, style = stroke)
                drawLine(tint, Offset(size.width * 0.5f, size.height * 0.36f), Offset(size.width * 0.5f, size.height * 0.58f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawCircle(tint, size.minDimension * 0.04f, Offset(size.width * 0.5f, size.height * 0.68f))
            }
            TrackingIconKind.Copy -> {
                drawRoundRect(tint, Offset(size.width * 0.28f, size.height * 0.24f), Size(size.width * 0.44f, size.height * 0.5f), CornerRadius(size.width * 0.08f), style = stroke)
                drawRoundRect(tint, Offset(size.width * 0.14f, size.height * 0.12f), Size(size.width * 0.44f, size.height * 0.5f), CornerRadius(size.width * 0.08f), style = stroke)
            }
        }
    }
}
