package com.pedilo.app.ui.admin.configuration

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pedilo.app.core.model.PublicAdvertisingConfig
import com.pedilo.app.core.model.PublicConfiguration
import com.pedilo.app.core.model.PublicConfigurationUpdateRequest
import com.pedilo.app.core.model.PublicDailyInfoConfig
import com.pedilo.app.core.model.PublicInformationConfig
import com.pedilo.app.core.model.PublicQuickAccessConfig
import com.pedilo.app.ui.components.PediloTextField
import com.pedilo.app.ui.publicuser.PediloCyan
import com.pedilo.app.ui.publicuser.PediloGreen
import com.pedilo.app.ui.publicuser.PediloLine
import com.pedilo.app.ui.publicuser.PediloMuted
import com.pedilo.app.ui.publicuser.PediloOrange
import com.pedilo.app.ui.publicuser.PediloPanel
import com.pedilo.app.ui.publicuser.PediloPanelSoft
import com.pedilo.app.ui.publicuser.PediloPink
import com.pedilo.app.ui.publicuser.PediloText

@Composable
fun AdminPublicEditHomeScreen(
    onTitles: () -> Unit,
    onQuickAccess: () -> Unit,
    onAdvertising: () -> Unit,
    onInformation: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, top = 18.dp, end = 16.dp, bottom = 152.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { AdminPublicHeader("Editar Público", "Contenido visible para Usuario Público") }
        item { AdminPublicNavCard("Editar títulos", "Título y subtítulo del Home", PediloOrange, onTitles) }
        item { AdminPublicNavCard("Editar accesos rápidos", "4 cards con tipo de local e imagen", PediloCyan, onQuickAccess) }
        item { AdminPublicNavCard("Editar publicidad", "Texto opcional y hasta 3 imágenes", PediloPink, onAdvertising) }
        item { AdminPublicNavCard("Editar información", "Banner, botón y contenidos informativos", PediloGreen, onInformation) }
    }
}

@Composable
fun AdminPublicTitlesScreen(
    config: PublicConfiguration,
    message: String,
    error: String,
    onSave: (PublicConfigurationUpdateRequest) -> Unit,
) {
    var title by remember(config.title) { mutableStateOf(config.title) }
    var subtitle by remember(config.subtitle) { mutableStateOf(config.subtitle) }
    var localError by remember { mutableStateOf("") }
    AdminPublicFormScaffold("Editar títulos", message, error, localError) {
        item {
            AdminPublicPanel {
                PediloTextField(title, { title = it }, "Título principal", singleLine = true)
                PediloTextField(subtitle, { subtitle = it }, "Subtítulo", singleLine = true)
                AdminPublicSaveButton {
                    if (title.isBlank() || subtitle.isBlank()) {
                        localError = "Título y subtítulo son obligatorios."
                    } else {
                        localError = ""
                        onSave(PublicConfigurationUpdateRequest(title = title.trim(), subtitle = subtitle.trim()))
                    }
                }
            }
        }
    }
}

@Composable
fun AdminPublicQuickAccessScreen(
    config: PublicConfiguration,
    message: String,
    error: String,
    onUploadImage: (Uri, String, (String) -> Unit) -> Unit,
    onSave: (PublicConfigurationUpdateRequest) -> Unit,
) {
    val items = remember(config.quickAccess) { mutableStateListOf(*config.quickAccess.toTypedArray()) }
    var selectedIndex by remember { mutableStateOf(0) }
    var localError by remember { mutableStateOf("") }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            onUploadImage(it, "quick_access_$selectedIndex") { url ->
                items[selectedIndex] = items[selectedIndex].copy(imageUrl = url)
            }
        }
    }
    AdminPublicFormScaffold("Editar accesos rápidos", message, error, localError) {
        itemsIndexed(items) { index, item ->
            AdminPublicPanel {
                Text("Acceso rápido ${index + 1}", color = PediloText, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                PediloTextField(item.title, { value -> items[index] = item.copy(title = value) }, "Título", singleLine = true)
                PediloTextField(item.storeType, { value -> items[index] = item.copy(storeType = value) }, "Tipo de local", singleLine = true)
                AdminImageRow(item.imageUrl.ifBlank { "Sin imagen" }) {
                    selectedIndex = index
                    launcher.launch("image/*")
                }
            }
        }
        item {
            AdminPublicSaveButton {
                val invalid = items.any { it.title.isBlank() || it.storeType.isBlank() || it.imageUrl.isBlank() }
                if (items.size != 4 || invalid) {
                    localError = "Los 4 accesos necesitan título, tipo de local e imagen."
                } else {
                    localError = ""
                    onSave(PublicConfigurationUpdateRequest(quickAccess = items.map { it.cleaned() }))
                }
            }
        }
    }
}

@Composable
fun AdminPublicAdvertisingScreen(
    config: PublicConfiguration,
    message: String,
    error: String,
    onUploadImage: (Uri, String, (String) -> Unit) -> Unit,
    onSave: (PublicConfigurationUpdateRequest) -> Unit,
) {
    var text by remember(config.advertising.text) { mutableStateOf(config.advertising.text) }
    val images = remember(config.advertising.imageUrls) { mutableStateListOf(*config.advertising.imageUrls.take(3).toTypedArray()) }
    var selectedIndex by remember { mutableStateOf(0) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            onUploadImage(it, "advertising_$selectedIndex") { url ->
                if (selectedIndex < images.size) images[selectedIndex] = url else if (images.size < 3) images.add(url)
            }
        }
    }
    AdminPublicFormScaffold("Editar publicidad", message, error, "") {
        item {
            AdminPublicPanel {
                PediloTextField(text, { text = it }, "Texto publicitario opcional", singleLine = false)
                repeat(3) { index ->
                    AdminImageRow(images.getOrNull(index) ?: "Sin imagen") {
                        selectedIndex = index
                        launcher.launch("image/*")
                    }
                }
                AdminPublicSaveButton {
                    onSave(PublicConfigurationUpdateRequest(advertising = PublicAdvertisingConfig(text.trim(), images.filter { it.isNotBlank() }.take(3))))
                }
            }
        }
    }
}

@Composable
fun AdminPublicInformationScreen(
    config: PublicConfiguration,
    message: String,
    error: String,
    onUploadImage: (Uri, String, (String) -> Unit) -> Unit,
    onSave: (PublicConfigurationUpdateRequest) -> Unit,
) {
    var mainText by remember(config.information.mainText) { mutableStateOf(config.information.mainText) }
    var mainImage by remember(config.information.mainImageUrl) { mutableStateOf(config.information.mainImageUrl) }
    var buttonText by remember(config.information.buttonText) { mutableStateOf(config.information.buttonText) }
    var dailyText by remember(config.information.daily.text) { mutableStateOf(config.information.daily.text) }
    var dailyDescription by remember(config.information.daily.description) { mutableStateOf(config.information.daily.description) }
    var dailyImage by remember(config.information.daily.imageUrl) { mutableStateOf(config.information.daily.imageUrl) }
    var importantNotice by remember(config.information.importantNotice) { mutableStateOf(config.information.importantNotice) }
    var usefulTip by remember(config.information.usefulTip) { mutableStateOf(config.information.usefulTip) }
    var news by remember(config.information.news) { mutableStateOf(config.information.news) }
    var localError by remember { mutableStateOf("") }
    var imageTarget by remember { mutableStateOf("main") }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            onUploadImage(it, "information_$imageTarget") { url ->
                if (imageTarget == "daily") dailyImage = url else mainImage = url
            }
        }
    }
    AdminPublicFormScaffold("Editar información", message, error, localError) {
        item {
            AdminPublicPanel {
                Text("Información principal", color = PediloText, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                PediloTextField(mainText, { mainText = it }, "Texto informativo principal", singleLine = false)
                AdminImageRow(mainImage.ifBlank { "Sin imagen" }) {
                    imageTarget = "main"
                    launcher.launch("image/*")
                }
                PediloTextField(buttonText, { buttonText = it }, "Texto del botón", singleLine = true)
            }
        }
        item {
            AdminPublicPanel {
                Text("Información del día", color = PediloText, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                PediloTextField(dailyText, { dailyText = it }, "Texto opcional", singleLine = false)
                PediloTextField(dailyDescription, { dailyDescription = it }, "Subtexto opcional", singleLine = false)
                AdminImageRow(dailyImage.ifBlank { "Sin imagen" }) {
                    imageTarget = "daily"
                    launcher.launch("image/*")
                }
            }
        }
        item {
            AdminPublicPanel {
                Text("Cards informativas", color = PediloText, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                PediloTextField(importantNotice, { importantNotice = it }, "Aviso importante opcional", singleLine = false)
                PediloTextField(usefulTip, { usefulTip = it }, "Dato útil opcional", singleLine = false)
                PediloTextField(news, { news = it }, "Novedad opcional", singleLine = false)
                AdminPublicSaveButton {
                    if (mainText.isBlank() || mainImage.isBlank() || buttonText.isBlank()) {
                        localError = "Texto principal, imagen principal y texto del botón son obligatorios."
                    } else {
                        localError = ""
                        onSave(
                            PublicConfigurationUpdateRequest(
                                information = PublicInformationConfig(
                                    mainText = mainText.trim(),
                                    mainImageUrl = mainImage.trim(),
                                    buttonText = buttonText.trim(),
                                    daily = PublicDailyInfoConfig(dailyText.trim(), dailyDescription.trim(), dailyImage.trim()),
                                    importantNotice = importantNotice.trim(),
                                    usefulTip = usefulTip.trim(),
                                    news = news.trim(),
                                ),
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminPublicFormScaffold(
    title: String,
    message: String,
    error: String,
    localError: String,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, top = 18.dp, end = 16.dp, bottom = 152.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { AdminPublicHeader(title, "Configuración pública real") }
        if (message.isNotBlank()) item { AdminPublicNotice("Guardado", message, PediloGreen) }
        if (error.isNotBlank()) item { AdminPublicNotice("Error", error, PediloOrange) }
        if (localError.isNotBlank()) item { AdminPublicNotice("Revisar", localError, PediloOrange) }
        content()
    }
}

@Composable
private fun AdminPublicHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, color = PediloText, fontSize = 25.sp, lineHeight = 29.sp, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, color = PediloMuted, fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AdminPublicNavCard(title: String, subtitle: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    AdminPublicPanel(
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
        borderColor = color,
    ) {
        Text(title, color = PediloText, fontSize = 19.sp, lineHeight = 23.sp, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, color = PediloMuted, fontSize = 13.sp, lineHeight = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AdminPublicPanel(
    modifier: Modifier = Modifier,
    borderColor: androidx.compose.ui.graphics.Color = PediloLine,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PediloPanel, RoundedCornerShape(8.dp))
            .border(1.dp, borderColor.copy(alpha = 0.52f), RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
private fun AdminImageRow(label: String, onPick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PediloPanelSoft, RoundedCornerShape(8.dp))
            .border(1.dp, PediloLine.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, color = PediloMuted, fontSize = 12.sp, lineHeight = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Button(onClick = onPick) { Text("Cargar imagen") }
    }
}

@Composable
private fun AdminPublicSaveButton(onClick: () -> Unit) {
    Spacer(Modifier.height(2.dp))
    Button(onClick = onClick) {
        Text("Guardar")
    }
}

@Composable
private fun AdminPublicNotice(title: String, text: String, color: androidx.compose.ui.graphics.Color) {
    AdminPublicPanel(borderColor = color) {
        Text(title, color = color, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        Text(text, color = PediloText, fontSize = 13.sp, lineHeight = 17.sp)
    }
}

private fun PublicQuickAccessConfig.cleaned(): PublicQuickAccessConfig =
    copy(title = title.trim(), storeType = storeType.trim(), imageUrl = imageUrl.trim())
