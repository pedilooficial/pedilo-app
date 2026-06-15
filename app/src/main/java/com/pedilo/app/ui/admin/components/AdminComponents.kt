package com.pedilo.app.ui.admin.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Preview
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import com.pedilo.app.ui.admin.AdminEntry
import com.pedilo.app.ui.admin.AdminRoot
import com.pedilo.app.ui.publicuser.PediloLine
import com.pedilo.app.ui.publicuser.PediloMuted
import com.pedilo.app.ui.publicuser.PediloOrange
import com.pedilo.app.ui.publicuser.PediloCyan
import com.pedilo.app.ui.publicuser.PediloGreen
import com.pedilo.app.ui.publicuser.PediloPanel
import com.pedilo.app.ui.publicuser.PediloPanelSoft
import com.pedilo.app.ui.publicuser.PediloPink
import com.pedilo.app.ui.publicuser.PediloText
import com.pedilo.app.ui.publicuser.PediloWarning
import com.pedilo.app.ui.publicuser.PediloCardBrush
import com.pedilo.app.ui.publicuser.pediloCardDepth

private data class AdminUniverseTone(
    val label: String,
    val primary: Color,
    val secondary: Color,
    val icon: ImageVector,
)

private data class AdminIntentTone(
    val label: String,
    val color: Color,
    val icon: ImageVector,
)

private val AdminOperationTone = AdminUniverseTone("Pedidos vivos", PediloOrange, PediloWarning, Icons.Outlined.Dashboard)
private val AdminConfigurationTone = AdminUniverseTone("Ajustes", PediloCyan, PediloOrange, Icons.Outlined.Settings)
private val AdminRoleAccessTone = AdminUniverseTone("Accesos", PediloPink, PediloCyan, Icons.Outlined.AdminPanelSettings)

@Composable
fun AdminHeader(
    title: String,
    eyebrow: String,
    summary: String,
    onSignOut: () -> Unit,
    showSignOut: Boolean,
) {
    val tone = adminUniverseToneFor(eyebrow, title)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pediloCardDepth(RoundedCornerShape(15.dp))
            .background(
                Brush.linearGradient(
                    listOf(tone.primary.copy(alpha = 0.16f), PediloPanelSoft.copy(alpha = 0.96f), PediloPanel),
                ),
                RoundedCornerShape(15.dp),
            )
            .border(1.dp, tone.primary.copy(alpha = 0.44f), RoundedCornerShape(15.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(tone.primary.copy(alpha = 0.16f), RoundedCornerShape(9.dp))
                    .border(1.dp, tone.primary.copy(alpha = 0.38f), RoundedCornerShape(9.dp))
                    .size(30.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(tone.icon, contentDescription = null, tint = tone.primary, modifier = Modifier.size(18.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(eyebrow, color = tone.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(tone.label, color = tone.secondary, fontSize = 11.sp, lineHeight = 13.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        Text(title, color = PediloText, fontSize = 19.sp, lineHeight = 23.sp, fontWeight = FontWeight.ExtraBold)
        Text(summary, color = PediloMuted, fontSize = 13.sp, lineHeight = 18.sp)
        if (showSignOut) {
            val interactionSource = remember { MutableInteractionSource() }
            val pressed by interactionSource.collectIsPressedAsState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(if (pressed) 0.988f else 1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (pressed) PediloPanel else PediloPanelSoft, RoundedCornerShape(12.dp))
                    .border(1.dp, if (pressed) PediloOrange.copy(alpha = 0.72f) else PediloLine, RoundedCornerShape(12.dp))
                    .clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onSignOut)
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, tint = PediloText)
                Text("Cerrar sesión", color = PediloText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AdminEntryCard(entry: AdminEntry, onClick: () -> Unit) {
    val intent = adminIntentToneFor(entry.title, entry.note)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (pressed) 0.982f else 1f)
            .pediloCardDepth(RoundedCornerShape(15.dp))
            .background(
                if (pressed) {
                    Brush.linearGradient(listOf(intent.color.copy(alpha = 0.18f), PediloPanelSoft))
                } else {
                    PediloCardBrush
                },
                RoundedCornerShape(15.dp),
            )
            .border(1.dp, if (pressed) intent.color.copy(alpha = 0.78f) else intent.color.copy(alpha = 0.28f), RoundedCornerShape(15.dp))
            .clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(intent.color.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
                .border(1.dp, intent.color.copy(alpha = 0.34f), RoundedCornerShape(12.dp))
                .size(38.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(intent.icon, contentDescription = null, tint = intent.color, modifier = Modifier.size(21.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    entry.title,
                    color = PediloText,
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(entry.note, color = PediloMuted, fontSize = 13.sp, lineHeight = 17.sp)
            AdminIntentChip(intent)
        }
    }
}

@Composable
fun AdminInfoPanel(title: String, text: String) {
    val intent = adminIntentToneFor(title, text)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(
                Brush.linearGradient(listOf(intent.color.copy(alpha = 0.10f), PediloPanelSoft)),
                RoundedCornerShape(15.dp),
            )
            .border(1.dp, intent.color.copy(alpha = 0.25f), RoundedCornerShape(15.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(intent.icon, contentDescription = null, tint = intent.color, modifier = Modifier.size(18.dp))
            Text(title, color = PediloText, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
        Text(text, color = PediloMuted, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
fun AdminBottomBar(
    current: AdminRoot,
    onOperation: () -> Unit,
    onConfiguration: () -> Unit,
    onRoleAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .background(Brush.verticalGradient(listOf(PediloPanelSoft.copy(alpha = 0.96f), PediloPanel)), RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .border(1.dp, PediloLine, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AdminBottomItem(AdminOperationTone.icon, "Operación", AdminOperationTone.primary, selected = current == AdminRoot.Operation, onClick = onOperation, modifier = Modifier.weight(1f))
        AdminBottomItem(AdminConfigurationTone.icon, "Configuración", AdminConfigurationTone.primary, selected = current == AdminRoot.Configuration, onClick = onConfiguration, modifier = Modifier.weight(1f))
        AdminBottomItem(AdminRoleAccessTone.icon, "Equipo", AdminRoleAccessTone.primary, selected = current == AdminRoot.RoleAccess, onClick = onRoleAccess, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun AdminBottomItem(
    icon: ImageVector,
    label: String,
    toneColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Column(
        modifier = modifier
            .height(68.dp)
            .scale(if (pressed) 0.98f else 1f)
            .clip(RoundedCornerShape(15.dp))
            .background(if (selected || pressed) toneColor.copy(alpha = 0.18f) else Color.Transparent, RoundedCornerShape(15.dp))
            .border(1.dp, if (selected || pressed) toneColor.copy(alpha = 0.62f) else PediloLine.copy(alpha = 0.45f), RoundedCornerShape(15.dp))
            .clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) toneColor else PediloMuted,
        )
        Text(
            label,
            color = if (selected) toneColor else PediloMuted,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AdminIntentChip(intent: AdminIntentTone) {
    Text(
        text = intent.label,
        color = intent.color,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(intent.color.copy(alpha = 0.11f), RoundedCornerShape(50))
            .border(1.dp, intent.color.copy(alpha = 0.28f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

private fun adminUniverseToneFor(eyebrow: String, title: String): AdminUniverseTone =
    when {
        eyebrow.contains("Alta", ignoreCase = true) || title.contains("rol", ignoreCase = true) || title.contains("cuenta", ignoreCase = true) -> AdminRoleAccessTone
        eyebrow.contains("Configuración", ignoreCase = true) || eyebrow.contains("Público", ignoreCase = true) || title == "Configuración" -> AdminConfigurationTone
        else -> AdminOperationTone
    }

private fun adminIntentToneFor(title: String, note: String): AdminIntentTone {
    val text = "$title $note"
    return when {
        text.contains("Auditor", ignoreCase = true) || text.contains("registro", ignoreCase = true) || text.contains("trazabilidad", ignoreCase = true) ->
            AdminIntentTone("Historial", PediloPink, Icons.Outlined.History)
        text.contains("impacto", ignoreCase = true) || text.contains("emergencia", ignoreCase = true) || text.contains("sensible", ignoreCase = true) ->
            AdminIntentTone("Impacto", PediloWarning, Icons.Outlined.ReportProblem)
        text.contains("paus", ignoreCase = true) || text.contains("detenido", ignoreCase = true) || text.contains("inactivo", ignoreCase = true) ->
            AdminIntentTone("Bloqueo", PediloPink, Icons.Outlined.Block)
        text.contains("revis", ignoreCase = true) ->
            AdminIntentTone("Revisión", PediloCyan, Icons.Outlined.Preview)
        text.contains("edit", ignoreCase = true) || text.contains("ajust", ignoreCase = true) ->
            AdminIntentTone("Editable", PediloOrange, Icons.Outlined.EditNote)
        text.contains("víncul", ignoreCase = true) || text.contains("vincul", ignoreCase = true) ->
            AdminIntentTone("Vínculo", PediloCyan, Icons.Outlined.Link)
        text.contains("rol", ignoreCase = true) || text.contains("cuenta", ignoreCase = true) || text.contains("acceso", ignoreCase = true) ->
            AdminIntentTone("Acceso", AdminRoleAccessTone.primary, Icons.Outlined.ManageAccounts)
        text.contains("activo", ignoreCase = true) || text.contains("listo", ignoreCase = true) || text.contains("publicable", ignoreCase = true) ->
            AdminIntentTone("Listo", PediloGreen, Icons.Outlined.VerifiedUser)
        else -> AdminIntentTone("Lectura", PediloMuted, Icons.Outlined.AssignmentTurnedIn)
    }
}
