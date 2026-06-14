package com.pedilo.app.ui.publicuser

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val MaxPhoneChars = 16
private const val MaxTrackingChars = 14
private val PublicTrackingPattern = Regex("^PDL-[A-Z0-9]{4,10}$")
private val publicPlaceholderValues = setOf(
    "nombre",
    "tu nombre",
    "telefono",
    "teléfono",
    "direccion",
    "dirección",
    "pedido",
    "producto",
    "paquete",
)

fun normalizePublicPhoneInput(raw: String): String {
    val trimmed = raw.take(MaxPhoneChars * 2)
    val builder = StringBuilder()
    trimmed.forEachIndexed { index, char ->
        when {
            char.isDigit() -> builder.append(char)
            char == '+' && index == 0 -> builder.append(char)
        }
    }
    return builder.toString().take(MaxPhoneChars)
}

fun publicPhoneDigitCount(phone: String): Int =
    phone.count(Char::isDigit)

fun isValidPublicPhone(phone: String): Boolean {
    val digits = publicPhoneDigitCount(phone)
    return digits in 8..15 && (phone.count { it == '+' } <= 1) && (!phone.contains('+') || phone.startsWith("+"))
}

fun normalizePublicTrackingInput(raw: String): String =
    raw.uppercase()
        .filter { it.isLetterOrDigit() || it == '-' }
        .take(MaxTrackingChars)

fun isValidPublicTrackingNumber(value: String): Boolean {
    val normalized = normalizePublicTrackingInput(value)
    return normalized.startsWith("PDL-") && PublicTrackingPattern.matches(normalized)
}

fun isPublicPlaceholder(value: String): Boolean =
    value.trim().lowercase() in publicPlaceholderValues

fun hasPublicValue(value: String): Boolean =
    value.isNotBlank() && !isPublicPlaceholder(value)

@Composable
fun PublicTextInput(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = 58.dp,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    errorText: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .pediloCardDepth(RoundedCornerShape(15.dp))
            .background(PediloCardBrush, RoundedCornerShape(15.dp))
            .border(1.dp, if (errorText == null) PediloLine.copy(alpha = 0.92f) else PediloWarning, RoundedCornerShape(15.dp))
            .padding(13.dp),
    ) {
        Text(label, color = PediloMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(7.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = PediloText, fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            cursorBrush = SolidColor(PediloText),
            modifier = Modifier
                .fillMaxWidth()
                .height(minHeight),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.horizontalGradient(listOf(PediloPanelSoft, PediloPanel, PediloPanelSoft)), RoundedCornerShape(11.dp))
                        .border(1.dp, PediloGoldLine.copy(alpha = 0.35f), RoundedCornerShape(11.dp))
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                ) {
                    if (value.isBlank()) {
                        Text(
                            text = placeholder,
                            color = PediloMuted.copy(alpha = 0.68f),
                            fontSize = 16.sp,
                            maxLines = if (singleLine) 1 else 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
            },
        )
        errorText?.let {
            Spacer(Modifier.height(7.dp))
            Text(it, color = PediloWarning, fontSize = 12.sp, lineHeight = 15.sp)
        }
    }
}

@Composable
fun PublicPhoneInput(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val error = if (value.isNotBlank() && !isValidPublicPhone(value)) "Ingresá un teléfono válido." else null
    PublicTextInput(
        label = label,
        value = value,
        placeholder = placeholder,
        onValueChange = { onValueChange(normalizePublicPhoneInput(it)) },
        modifier = modifier,
        keyboardType = KeyboardType.Phone,
        errorText = error,
    )
}

@Composable
fun PublicTrackingInput(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val error = if (value.isNotBlank() && !isValidPublicTrackingNumber(value)) "Ingresá un número de pedido válido." else null
    PublicTextInput(
        label = label,
        value = value,
        placeholder = placeholder,
        onValueChange = { onValueChange(normalizePublicTrackingInput(it)) },
        modifier = modifier,
        keyboardType = KeyboardType.Ascii,
        errorText = error,
    )
}
