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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pedilo.app.core.model.PublicClaimDraft
import com.pedilo.app.core.result.CoreError
import com.pedilo.app.core.result.CoreResult
import com.pedilo.app.core.runtime.publicClaimUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class ConventionIconKind {
    Info,
    Claim,
    Tracking,
    Clock,
    Check,
    Phone,
    Note,
    Alert,
}

private data class ConventionOption(
    val title: String,
    val subtitle: String,
    val badge: String,
    val icon: ConventionIconKind,
    val onClick: () -> Unit,
)

@Composable
fun PublicConventionsScreen(
    onHome: () -> Unit,
    onPlus: () -> Unit,
    onShop: () -> Unit,
    onInfo: () -> Unit,
    onClaim: () -> Unit,
    onTracking: () -> Unit,
) {
    PublicShell(
        current = PublicBottomDestination.Home,
        onHome = onHome,
        onPlus = onPlus,
        onShop = onShop,
    ) {
        val options = listOf(
            ConventionOption("Información del día", "", "Leer", ConventionIconKind.Info, onInfo),
            ConventionOption("Reclamo", "", "Aviso", ConventionIconKind.Claim, onClaim),
            ConventionOption("Seguimiento del pedido", "", "Pedido", ConventionIconKind.Tracking, onTracking),
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .background(PediloBg),
            contentPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { TodayHighlightCard() }
            options.forEach { option ->
                item { ConventionOptionCard(option) }
            }
        }
    }
}

@Composable
fun PublicConventionsInfoScreen(
    onHome: () -> Unit,
    onPlus: () -> Unit,
    onShop: () -> Unit,
) {
    PublicShell(
        current = PublicBottomDestination.Home,
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
            item {
                InformationCard(
                    icon = ConventionIconKind.Alert,
                    title = "Aviso importante",
                    body = "Cuando hay mucha actividad algunos locales pueden tardar unos minutos más de lo habitual.",
                    accent = PediloOrange,
                )
            }
            item {
                InformationCard(
                    icon = ConventionIconKind.Clock,
                    title = "Dato útil",
                    body = "Revisá el tiempo estimado antes de confirmar cualquier compra o retiro.",
                    accent = PediloCyan,
                )
            }
            item {
                InformationCard(
                    icon = ConventionIconKind.Info,
                    title = "Novedad",
                    body = "Usá el número que recibiste al confirmar tu pedido para consultar el estado.",
                    accent = PediloPink,
                )
            }
        }
    }
}

@Composable
fun PublicConventionsClaimScreen(
    onHome: () -> Unit,
    onPlus: () -> Unit,
    onShop: () -> Unit,
) {
    val submitPublicClaim = remember { publicClaimUseCase() }
    val scope = rememberCoroutineScope()
    var orderNumber by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var receiptMessage by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    PublicShell(
        current = PublicBottomDestination.Home,
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
            item {
                ClaimIntroCard(prepared = receiptMessage.isNotBlank())
            }
            item {
                ConventionInput(
                    label = "Número de pedido opcional",
                    value = orderNumber,
                    placeholder = "PDL-XXXXXX",
                    onValueChange = {
                        orderNumber = normalizePublicTrackingInput(it)
                        receiptMessage = ""
                        error = ""
                    },
                )
            }
            item {
                ConventionInput(
                    label = "Nombre",
                    value = name,
                    placeholder = "Tu nombre",
                    onValueChange = {
                        name = it
                        receiptMessage = ""
                        error = ""
                    },
                )
            }
            item {
                PublicPhoneInput(
                    label = "Teléfono / WhatsApp",
                    value = phone,
                    placeholder = "11 5555 5555",
                    onValueChange = {
                        phone = it
                        receiptMessage = ""
                        error = ""
                    },
                )
            }
            item {
                ConventionInput(
                    label = "Motivo",
                    value = reason,
                    placeholder = "Demora, pago, dirección u otro",
                    onValueChange = {
                        reason = it
                        receiptMessage = ""
                        error = ""
                    },
                )
            }
            item {
                ConventionInput(
                    label = "Descripción de lo ocurrido",
                    value = description,
                    placeholder = "Contanos brevemente qué pasó",
                    minHeight = 108.dp,
                    singleLine = false,
                    onValueChange = {
                        description = it
                        receiptMessage = ""
                        error = ""
                    },
                )
            }
            item {
                ConventionPrimaryAction(
                    label = if (isSubmitting) "Registrando" else "Enviar reclamo",
                    icon = if (receiptMessage.isNotBlank()) ConventionIconKind.Check else ConventionIconKind.Claim,
                    enabled = !isSubmitting &&
                        receiptMessage.isBlank() &&
                        hasPublicValue(name) &&
                        isValidPublicPhone(phone) &&
                        hasPublicValue(reason) &&
                        hasPublicValue(description) &&
                        (orderNumber.isBlank() || isValidPublicTrackingNumber(orderNumber)),
                    onClick = {
                        scope.launch {
                            isSubmitting = true
                            error = ""
                            val result = withContext(Dispatchers.IO) {
                                submitPublicClaim(
                                    PublicClaimDraft(
                                        orderNumber,
                                        name,
                                        phone,
                                        reason,
                                        description,
                                    ),
                                )
                            }
                            isSubmitting = false
                            when (result) {
                                is CoreResult.Success -> receiptMessage = result.value.publicMessage
                                is CoreResult.Failure -> error = result.error.publicClaimMessage()
                            }
                        }
                    },
                )
            }
            if (receiptMessage.isNotBlank()) {
                item {
                    ConventionNotice(text = receiptMessage)
                }
            }
            if (error.isNotBlank()) {
                item {
                    ConventionNotice(text = error)
                }
            }
        }
    }
}

private fun CoreError.publicClaimMessage(): String =
    when (this) {
        is CoreError.Validation -> "Revisá el número de pedido y los datos del reclamo."
        CoreError.IncompleteData -> "Completá nombre, teléfono, motivo y descripción."
        is CoreError.Operational -> humanMessage
        CoreError.NotAvailable -> "No pudimos registrar el reclamo. Probá de nuevo."
        CoreError.Unknown -> "No pudimos registrar el reclamo. Probá de nuevo."
    }

@Composable
fun PublicConventionsTrackingEntryScreen(
    onHome: () -> Unit,
    onPlus: () -> Unit,
    onShop: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var orderNumber by remember { mutableStateOf("") }

    PublicShell(
        current = PublicBottomDestination.Home,
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
            item {
                TrackingEntryCard()
            }
            item {
                PublicTrackingInput(
                    label = "Número de pedido",
                    value = orderNumber,
                    placeholder = "PDL-XXXXXX",
                    onValueChange = { orderNumber = it },
                )
            }
            item {
                ConventionPrimaryAction(
                    label = "Consultar pedido",
                    icon = ConventionIconKind.Tracking,
                    enabled = isValidPublicTrackingNumber(orderNumber),
                    onClick = { onSubmit(orderNumber) },
                )
            }
        }
    }
}

@Composable
private fun ConventionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 30.sp,
            lineHeight = 33.sp,
            fontWeight = FontWeight.ExtraBold,
            style = TextStyle(brush = Brush.verticalGradient(listOf(PediloWarning, PediloOrangeSoft, PediloOrangeDark))),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            color = PediloMuted,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TodayHighlightCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pediloCardDepth(RoundedCornerShape(16.dp))
            .background(PediloWarmPanelBrush, RoundedCornerShape(16.dp))
            .border(1.dp, PediloGoldLine.copy(alpha = 0.62f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(PediloOrange),
            contentAlignment = Alignment.Center,
        ) {
            ConventionIcon(ConventionIconKind.Info, tint = Color.White, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Día activo", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text("¡Envíos más rápidos!", color = Color.White, fontSize = 19.sp, lineHeight = 22.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(4.dp))
            Text("Hoy priorizamos los locales con mejor disponibilidad para que tus pedidos lleguen antes.", color = Color.White, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun ConventionOptionCard(option: ConventionOption) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pediloCardDepth(RoundedCornerShape(15.dp))
            .background(PediloCardBrush, RoundedCornerShape(15.dp))
            .border(1.dp, PediloLine.copy(alpha = 0.90f), RoundedCornerShape(15.dp))
            .clickable(role = Role.Button, onClick = option.onClick)
            .semantics { contentDescription = option.title }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .background(PediloPanelSoft, RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center,
        ) {
            ConventionIcon(option.icon, tint = PediloOrange, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(option.title, color = PediloText, fontSize = 18.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
            if (option.subtitle.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(option.subtitle, color = PediloMuted, fontSize = 12.sp, lineHeight = 15.sp)
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            option.badge,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .background(PediloOrange, RoundedCornerShape(18.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun InformationCard(
    icon: ConventionIconKind,
    title: String,
    body: String,
    accent: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pediloCardDepth(RoundedCornerShape(15.dp))
            .background(PediloCardBrush, RoundedCornerShape(15.dp))
            .border(1.dp, PediloLine.copy(alpha = 0.90f), RoundedCornerShape(15.dp))
            .padding(15.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(accent.copy(alpha = 0.22f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            ConventionIcon(icon, tint = accent, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = PediloText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(body, color = PediloMuted, fontSize = 14.sp, lineHeight = 19.sp)
        }
    }
}

@Composable
private fun ClaimIntroCard(prepared: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pediloCardDepth(RoundedCornerShape(15.dp))
            .background(if (prepared) Brush.verticalGradient(listOf(PediloGreen.copy(alpha = 0.18f), PediloPanel)) else PediloCardBrush, RoundedCornerShape(15.dp))
            .border(1.dp, if (prepared) PediloGreen else PediloLine, RoundedCornerShape(15.dp))
            .padding(15.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ConventionIcon(if (prepared) ConventionIconKind.Check else ConventionIconKind.Note, tint = if (prepared) PediloGreen else PediloOrange, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = if (prepared) "Aviso preparado" else "Registro de aviso",
                color = PediloText,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun TrackingEntryCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pediloCardDepth(RoundedCornerShape(16.dp))
            .background(PediloWarmPanelBrush, RoundedCornerShape(16.dp))
            .border(1.dp, PediloGoldLine.copy(alpha = 0.56f), RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text("Carga de número", color = PediloText, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun ConventionInput(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    minHeight: androidx.compose.ui.unit.Dp = 58.dp,
    singleLine: Boolean = true,
) {
    PublicTextInput(
        label = label,
        value = value,
        placeholder = placeholder,
        onValueChange = onValueChange,
        minHeight = minHeight,
        singleLine = singleLine,
    )
}

@Composable
private fun ConventionPrimaryAction(
    label: String,
    icon: ConventionIconKind,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .pediloButtonDepth(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    if (enabled) listOf(PediloWarning, PediloOrangeSoft, PediloOrangeDark) else listOf(PediloPanelSoft, PediloPanelSoft),
                ),
                RoundedCornerShape(14.dp),
            )
            .border(1.dp, if (enabled) PediloOrange else PediloLine, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ConventionIcon(icon, tint = Color.White, modifier = Modifier.size(25.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ConventionNotice(text: String) {
    Text(
        text = text,
        color = PediloMuted,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        modifier = Modifier
            .fillMaxWidth()
            .background(PediloPanel, RoundedCornerShape(12.dp))
            .border(1.dp, PediloLine, RoundedCornerShape(12.dp))
            .padding(12.dp),
        textAlign = TextAlign.Start,
    )
}

@Composable
private fun ConventionIcon(
    kind: ConventionIconKind,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = size.minDimension * 0.09f, cap = StrokeCap.Round)
        when (kind) {
            ConventionIconKind.Info -> {
                drawCircle(tint, size.minDimension * 0.38f, Offset(size.width * 0.5f, size.height * 0.5f), style = stroke)
                drawCircle(tint, size.minDimension * 0.04f, Offset(size.width * 0.5f, size.height * 0.34f))
                drawLine(tint, Offset(size.width * 0.5f, size.height * 0.48f), Offset(size.width * 0.5f, size.height * 0.7f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            ConventionIconKind.Claim -> {
                drawRoundRect(tint, Offset(size.width * 0.2f, size.height * 0.18f), Size(size.width * 0.6f, size.height * 0.66f), CornerRadius(size.width * 0.08f), style = stroke)
                drawLine(tint, Offset(size.width * 0.32f, size.height * 0.36f), Offset(size.width * 0.68f, size.height * 0.36f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.32f, size.height * 0.52f), Offset(size.width * 0.62f, size.height * 0.52f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.32f, size.height * 0.68f), Offset(size.width * 0.52f, size.height * 0.68f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            ConventionIconKind.Tracking -> {
                drawRoundRect(tint, Offset(size.width * 0.16f, size.height * 0.28f), Size(size.width * 0.52f, size.height * 0.35f), CornerRadius(size.width * 0.07f), style = stroke)
                drawLine(tint, Offset(size.width * 0.68f, size.height * 0.39f), Offset(size.width * 0.82f, size.height * 0.39f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.72f, size.height * 0.55f), Offset(size.width * 0.86f, size.height * 0.55f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawCircle(tint, size.minDimension * 0.07f, Offset(size.width * 0.28f, size.height * 0.76f), style = stroke)
                drawCircle(tint, size.minDimension * 0.07f, Offset(size.width * 0.66f, size.height * 0.76f), style = stroke)
            }
            ConventionIconKind.Clock -> {
                drawCircle(tint, size.minDimension * 0.34f, Offset(size.width * 0.5f, size.height * 0.5f), style = stroke)
                drawLine(tint, Offset(size.width * 0.5f, size.height * 0.3f), Offset(size.width * 0.5f, size.height * 0.52f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.5f, size.height * 0.52f), Offset(size.width * 0.66f, size.height * 0.62f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            ConventionIconKind.Check -> {
                drawLine(tint, Offset(size.width * 0.24f, size.height * 0.52f), Offset(size.width * 0.42f, size.height * 0.7f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.42f, size.height * 0.7f), Offset(size.width * 0.78f, size.height * 0.28f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            ConventionIconKind.Phone -> {
                drawRoundRect(tint, Offset(size.width * 0.32f, size.height * 0.14f), Size(size.width * 0.36f, size.height * 0.72f), CornerRadius(size.width * 0.09f), style = stroke)
                drawCircle(tint, size.minDimension * 0.035f, Offset(size.width * 0.5f, size.height * 0.76f))
            }
            ConventionIconKind.Note -> {
                drawRoundRect(tint, Offset(size.width * 0.18f, size.height * 0.18f), Size(size.width * 0.64f, size.height * 0.64f), CornerRadius(size.width * 0.07f), style = stroke)
                drawLine(tint, Offset(size.width * 0.32f, size.height * 0.4f), Offset(size.width * 0.68f, size.height * 0.4f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.32f, size.height * 0.58f), Offset(size.width * 0.62f, size.height * 0.58f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            ConventionIconKind.Alert -> {
                val triangle = Path().apply {
                    moveTo(size.width * 0.5f, size.height * 0.16f)
                    lineTo(size.width * 0.84f, size.height * 0.8f)
                    lineTo(size.width * 0.16f, size.height * 0.8f)
                    close()
                }
                drawPath(triangle, tint, style = stroke)
                drawLine(tint, Offset(size.width * 0.5f, size.height * 0.38f), Offset(size.width * 0.5f, size.height * 0.58f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawCircle(tint, size.minDimension * 0.035f, Offset(size.width * 0.5f, size.height * 0.68f))
            }
        }
    }
}
