package com.pedilo.app.ui.publicuser

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pedilo.app.core.model.TeamRole

@Composable
fun TeamLoginScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onLogin: (user: String, secret: String, keepSignedIn: Boolean) -> Unit,
) {
    val context = LocalContext.current
    var user by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    var keepSignedIn by remember { mutableStateOf(false) }
    var siteError by remember { mutableStateOf<String?>(null) }
    val canSubmit = user.isNotBlank() && secret.isNotBlank() && !isLoading

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PediloBg)
            .padding(horizontal = 24.dp, vertical = 36.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pediloCardDepth(RoundedCornerShape(20.dp))
                .background(PediloWarmPanelBrush, RoundedCornerShape(20.dp))
                .border(1.dp, PediloGoldLine, RoundedCornerShape(20.dp))
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "Equipo",
                color = PediloOrange,
                fontSize = 34.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                style = TextStyle(brush = PediloPrimaryBrush),
            )
            Text(
                "Ingresá con tu acceso de Pédilo.",
                color = PediloMuted,
                fontSize = 15.sp,
                lineHeight = 20.sp,
            )
            PublicTextInput(
                label = "Usuario",
                value = user,
                onValueChange = { user = it.take(80) },
                placeholder = "tu usuario",
                modifier = Modifier.fillMaxWidth(),
            )
            PublicTextInput(
                label = "Contraseña",
                value = secret,
                onValueChange = { secret = it.take(120) },
                placeholder = "tu clave",
                keyboardType = KeyboardType.Password,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Checkbox, onClick = { keepSignedIn = !keepSignedIn })
                    .padding(end = 8.dp),
            ) {
                Checkbox(checked = keepSignedIn, onCheckedChange = { keepSignedIn = it })
                Text("Mantener sesión iniciada", color = PediloText, fontSize = 14.sp)
            }
            errorMessage?.let {
                Text(it, color = PediloWarning, fontSize = 14.sp, lineHeight = 18.sp)
            }
            siteError?.let {
                Text(it, color = PediloWarning, fontSize = 13.sp, lineHeight = 17.sp)
            }
            TeamPrimaryButton(
                text = if (isLoading) "Ingresando" else "Ingresar",
                enabled = canSubmit,
                onClick = { onLogin(user.trim(), secret, keepSignedIn) },
                isLoading = isLoading,
            )
            if (errorMessage != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "¿Querés formar parte de Pédilo?",
                    color = PediloMuted,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Text(
                    "Ir a pediloapp.shop",
                    color = PediloOrange,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button) {
                            siteError = openTeamSite(context)
                        }
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun TeamRolePlaceholderScreen(
    role: TeamRole,
    onSignOutConfirmed: () -> Unit,
) {
    var showConfirm by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PediloBg)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pediloCardDepth(RoundedCornerShape(20.dp))
                .background(PediloCardBrush, RoundedCornerShape(20.dp))
                .border(1.dp, PediloLine, RoundedCornerShape(20.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Text(
                role.screenTitle,
                color = PediloOrange,
                fontSize = 31.sp,
                lineHeight = 35.sp,
                fontWeight = FontWeight.ExtraBold,
                style = TextStyle(brush = PediloPrimaryBrush),
                textAlign = TextAlign.Center,
            )
            TeamPrimaryButton(
                text = "Cerrar sesión",
                enabled = true,
                onClick = { showConfirm = true },
            )
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("¿Querés cerrar sesión?") },
            confirmButton = {
                TextButton(onClick = onSignOutConfirmed) {
                    Text("Sí")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text("No")
                }
            },
        )
    }
}

@Composable
private fun TeamPrimaryButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    isLoading: Boolean = false,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .pediloButtonDepth(RoundedCornerShape(16.dp))
            .background(
                if (enabled) PediloPrimaryBrush else Brush.verticalGradient(listOf(PediloPanelSoft, PediloPanel)),
                RoundedCornerShape(16.dp),
            )
            .border(1.dp, PediloWarning.copy(alpha = if (enabled) 0.42f else 0.12f), RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

private fun openTeamSite(context: android.content.Context): String? =
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://pediloapp.shop")))
        null
    } catch (_: ActivityNotFoundException) {
        "No pudimos abrir el navegador. Visitá pediloapp.shop."
    } catch (_: SecurityException) {
        "No pudimos abrir el navegador. Visitá pediloapp.shop."
    }
