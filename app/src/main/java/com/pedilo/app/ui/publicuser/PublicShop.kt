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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class ShopIconKind {
    Search,
    Tracking,
    Lightning,
    Dining,
    Cake,
    Basket,
    Burger,
    Pizza,
    Empanada,
    Sandwich,
    Salad,
    Grill,
    Sushi,
    Coffee,
    Pharmacy,
    Paw,
    Box,
}

private data class ShopCategoryGroup(
    val title: String,
    val icon: ShopIconKind,
    val accent: Color,
    val items: List<ShopCategoryItem>,
)

private data class ShopCategoryItem(
    val title: String,
    val icon: ShopIconKind,
    val isNew: Boolean = false,
)

private val shopGroups = listOf(
    ShopCategoryGroup(
        title = "Rápido y al paso",
        icon = ShopIconKind.Lightning,
        accent = PediloOrange,
        items = listOf(
            ShopCategoryItem("Hamburguesas", ShopIconKind.Burger),
            ShopCategoryItem("Pizzas", ShopIconKind.Pizza),
            ShopCategoryItem("Empanadas", ShopIconKind.Empanada),
            ShopCategoryItem("Sandwiches", ShopIconKind.Sandwich),
            ShopCategoryItem("Ensaladas", ShopIconKind.Salad),
        ),
    ),
    ShopCategoryGroup(
        title = "Para comer tranquilo",
        icon = ShopIconKind.Dining,
        accent = PediloCyan,
        items = listOf(
            ShopCategoryItem("Restaurantes", ShopIconKind.Dining),
            ShopCategoryItem("Parrillas", ShopIconKind.Grill),
            ShopCategoryItem("Sushi", ShopIconKind.Sushi),
            ShopCategoryItem("Cocina local", ShopIconKind.Basket),
            ShopCategoryItem("Internacional", ShopIconKind.Dining),
        ),
    ),
    ShopCategoryGroup(
        title = "Algo dulce",
        icon = ShopIconKind.Cake,
        accent = PediloPink,
        items = listOf(
            ShopCategoryItem("Heladería", ShopIconKind.Cake, isNew = true),
            ShopCategoryItem("Panadería", ShopIconKind.Sandwich),
            ShopCategoryItem("Cafetería", ShopIconKind.Coffee),
            ShopCategoryItem("Especialidades", ShopIconKind.Cake),
        ),
    ),
    ShopCategoryGroup(
        title = "Lo que necesito",
        icon = ShopIconKind.Box,
        accent = PediloOlive,
        items = listOf(
            ShopCategoryItem("Bebidas", ShopIconKind.Basket),
            ShopCategoryItem("Farmacia", ShopIconKind.Pharmacy),
            ShopCategoryItem("Mascotas", ShopIconKind.Paw),
            ShopCategoryItem("Snacks", ShopIconKind.Box),
            ShopCategoryItem("Supermercado", ShopIconKind.Basket),
        ),
    ),
)

@Composable
fun PublicShopScreen(
    catalogState: PublicCatalogState = PublicCatalogState(isLoading = false),
    onHome: () -> Unit,
    onPlus: () -> Unit,
    onShop: () -> Unit,
    onSearch: (String) -> Unit,
    onTracking: (String) -> Unit,
    onSubcategory: (String) -> Unit,
    onViewLocal: () -> Unit,
) {
    PublicShell(
        current = PublicBottomDestination.Shop,
        onHome = onHome,
        onPlus = onPlus,
        onShop = onShop,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .background(PediloBg),
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            item { ShopHeader() }
            item { ShopSearchCard(onPending = { onSearch("") }) }
            item { TrackingLookupCard(onPending = { onTracking(it) }) }
            items(shopGroups) { group ->
                CategoryGroupCard(group = group, onPending = { onSubcategory(it.title) })
            }
        }
    }
}

@Composable
private fun ShopHeader() {
    Box(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Tienda",
                fontSize = 44.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                style = TextStyle(
                    brush = Brush.verticalGradient(listOf(PediloWarning, PediloOrangeSoft, PediloOrange, PediloOrangeDark)),
                    shadow = androidx.compose.ui.graphics.Shadow(PediloOrangeDark.copy(alpha = 0.56f), Offset(0f, 4f), 13f),
                ),
            )
        }
    }
}

@Composable
private fun ShopSearchCard(onPending: () -> Unit) {
    var query by remember { mutableStateOf("") }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .pediloCardDepth(RoundedCornerShape(15.dp))
            .background(Brush.horizontalGradient(listOf(PediloPanelSoft, PediloPanel, PediloPanelSoft)), RoundedCornerShape(15.dp))
            .border(1.dp, PediloGoldLine.copy(alpha = 0.55f), RoundedCornerShape(15.dp))
            .clickable(role = Role.Button, onClick = onPending)
            .semantics { contentDescription = "Buscar en Tienda" }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShopIcon(ShopIconKind.Search, tint = PediloMuted, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = "Buscar productos, locales o categorías...",
                    color = PediloMuted,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                textStyle = TextStyle(color = PediloText, fontSize = 13.sp),
                singleLine = true,
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TrackingLookupCard(onPending: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pediloCardDepth(RoundedCornerShape(16.dp))
            .background(PediloWarmPanelBrush, RoundedCornerShape(16.dp))
            .border(1.dp, PediloGoldLine.copy(alpha = 0.62f), RoundedCornerShape(16.dp))
            .padding(13.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(PediloPanelSoft, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                ShopIcon(ShopIconKind.Tracking, tint = PediloOrange, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Consultá el estado de tu pedido", color = PediloText, fontSize = 15.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .background(PediloPanelSoft, RoundedCornerShape(10.dp))
                    .border(1.dp, PediloLine, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (code.isEmpty()) {
                    Text("Ej. PDL-XXXXXX", color = PediloMuted.copy(alpha = 0.68f), fontSize = 12.sp)
                }
                BasicTextField(
                    value = code,
                    onValueChange = { code = normalizePublicTrackingInput(it) },
                    textStyle = TextStyle(color = PediloText, fontSize = 12.sp),
                    singleLine = true,
                    cursorBrush = SolidColor(PediloText),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .width(90.dp)
                    .pediloButtonDepth(RoundedCornerShape(10.dp))
                    .background(PediloPrimaryBrush, RoundedCornerShape(10.dp))
                    .clickable(enabled = isValidPublicTrackingNumber(code), role = Role.Button, onClick = { onPending(code) }),
                contentAlignment = Alignment.Center,
            ) {
                Text("Consultar", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CategoryGroupCard(
    group: ShopCategoryGroup,
    onPending: (ShopCategoryItem) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pediloCardDepth(RoundedCornerShape(14.dp))
            .background(groupBrush(group.accent), RoundedCornerShape(14.dp))
            .border(1.dp, PediloLine.copy(alpha = 0.90f), RoundedCornerShape(14.dp))
            .padding(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShopIcon(group.icon, tint = group.accent, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Text(group.title, color = PediloText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(group.items) { item ->
                CategoryTile(item = item, accent = group.accent, onClick = { onPending(item) })
            }
        }
    }
}

@Composable
private fun CategoryTile(
    item: ShopCategoryItem,
    accent: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(86.dp)
            .height(86.dp)
            .pediloCardDepth(RoundedCornerShape(10.dp))
            .background(PediloCardBrush, RoundedCornerShape(10.dp))
            .border(1.dp, PediloLine.copy(alpha = 0.86f), RoundedCornerShape(10.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = item.title }
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Brush.radialGradient(listOf(accent, PediloPanelSoft))),
            contentAlignment = Alignment.Center,
        ) {
            ShopIcon(item.icon, tint = PediloWarning, modifier = Modifier.size(28.dp))
            if (item.isNew) {
                Text(
                    text = "Nuevo",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .background(PediloOrange, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = item.title,
            color = PediloText,
            fontSize = 9.sp,
            lineHeight = 10.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ShopStatusNote(message: String) {
    Text(
        text = message,
        color = PediloMuted,
        fontSize = 12.sp,
        lineHeight = 15.sp,
        modifier = Modifier
            .fillMaxWidth()
            .background(PediloPanel, RoundedCornerShape(10.dp))
            .border(1.dp, PediloLine, RoundedCornerShape(10.dp))
            .padding(12.dp),
    )
}

private fun groupBrush(accent: Color): Brush = Brush.horizontalGradient(
    listOf(accent.copy(alpha = 0.34f), PediloPanelSoft.copy(alpha = 0.82f), PediloPanel),
)

@Composable
private fun ShopIcon(
    kind: ShopIconKind,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = size.minDimension * 0.1f, cap = StrokeCap.Round)
        when (kind) {
            ShopIconKind.Search -> {
                drawCircle(tint, radius = size.minDimension * 0.27f, center = Offset(size.width * 0.42f, size.height * 0.42f), style = stroke)
                drawLine(tint, Offset(size.width * 0.62f, size.height * 0.62f), Offset(size.width * 0.84f, size.height * 0.84f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            ShopIconKind.Tracking -> {
                drawRoundRect(tint, Offset(size.width * 0.28f, size.height * 0.16f), Size(size.width * 0.44f, size.height * 0.68f), CornerRadius(size.width * 0.05f), style = stroke)
                drawLine(tint, Offset(size.width * 0.38f, size.height * 0.34f), Offset(size.width * 0.62f, size.height * 0.34f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.38f, size.height * 0.52f), Offset(size.width * 0.62f, size.height * 0.52f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.38f, size.height * 0.7f), Offset(size.width * 0.54f, size.height * 0.7f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            ShopIconKind.Lightning -> {
                val bolt = Path().apply {
                    moveTo(size.width * 0.58f, size.height * 0.08f)
                    lineTo(size.width * 0.25f, size.height * 0.54f)
                    lineTo(size.width * 0.48f, size.height * 0.54f)
                    lineTo(size.width * 0.38f, size.height * 0.92f)
                    lineTo(size.width * 0.75f, size.height * 0.42f)
                    lineTo(size.width * 0.52f, size.height * 0.42f)
                    close()
                }
                drawPath(bolt, tint)
            }
            ShopIconKind.Dining -> {
                drawLine(tint, Offset(size.width * 0.3f, size.height * 0.16f), Offset(size.width * 0.3f, size.height * 0.84f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.22f, size.height * 0.16f), Offset(size.width * 0.38f, size.height * 0.16f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.22f, size.height * 0.3f), Offset(size.width * 0.38f, size.height * 0.3f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.68f, size.height * 0.16f), Offset(size.width * 0.68f, size.height * 0.84f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawArc(tint, 180f, 180f, false, Offset(size.width * 0.56f, size.height * 0.16f), Size(size.width * 0.24f, size.height * 0.34f), style = stroke)
            }
            ShopIconKind.Cake -> {
                drawRoundRect(tint, Offset(size.width * 0.18f, size.height * 0.48f), Size(size.width * 0.64f, size.height * 0.28f), CornerRadius(size.width * 0.05f), style = stroke)
                drawLine(tint, Offset(size.width * 0.25f, size.height * 0.42f), Offset(size.width * 0.75f, size.height * 0.42f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.38f, size.height * 0.24f), Offset(size.width * 0.38f, size.height * 0.42f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            ShopIconKind.Basket, ShopIconKind.Burger, ShopIconKind.Sandwich, ShopIconKind.Box -> {
                drawRoundRect(tint, Offset(size.width * 0.2f, size.height * 0.4f), Size(size.width * 0.6f, size.height * 0.34f), CornerRadius(size.width * 0.07f), style = stroke)
                drawLine(tint, Offset(size.width * 0.32f, size.height * 0.4f), Offset(size.width * 0.42f, size.height * 0.2f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.68f, size.height * 0.4f), Offset(size.width * 0.58f, size.height * 0.2f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            ShopIconKind.Pizza, ShopIconKind.Empanada -> {
                val slice = Path().apply {
                    moveTo(size.width * 0.24f, size.height * 0.22f)
                    lineTo(size.width * 0.8f, size.height * 0.38f)
                    lineTo(size.width * 0.42f, size.height * 0.86f)
                    close()
                }
                drawPath(slice, tint, style = stroke)
                drawCircle(tint, size.minDimension * 0.04f, Offset(size.width * 0.5f, size.height * 0.45f))
            }
            ShopIconKind.Salad, ShopIconKind.Sushi, ShopIconKind.Coffee -> {
                drawOval(tint, Offset(size.width * 0.18f, size.height * 0.44f), Size(size.width * 0.64f, size.height * 0.28f), style = stroke)
                drawLine(tint, Offset(size.width * 0.3f, size.height * 0.72f), Offset(size.width * 0.7f, size.height * 0.72f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawCircle(tint, size.minDimension * 0.06f, Offset(size.width * 0.42f, size.height * 0.38f))
                drawCircle(tint, size.minDimension * 0.06f, Offset(size.width * 0.58f, size.height * 0.36f))
            }
            ShopIconKind.Grill -> {
                drawLine(tint, Offset(size.width * 0.2f, size.height * 0.58f), Offset(size.width * 0.8f, size.height * 0.58f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.32f, size.height * 0.3f), Offset(size.width * 0.32f, size.height * 0.78f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.5f, size.height * 0.3f), Offset(size.width * 0.5f, size.height * 0.78f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.68f, size.height * 0.3f), Offset(size.width * 0.68f, size.height * 0.78f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            ShopIconKind.Pharmacy -> {
                drawRoundRect(tint, Offset(size.width * 0.22f, size.height * 0.22f), Size(size.width * 0.56f, size.height * 0.56f), CornerRadius(size.width * 0.06f), style = stroke)
                drawLine(tint, Offset(size.width * 0.5f, size.height * 0.34f), Offset(size.width * 0.5f, size.height * 0.66f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.34f, size.height * 0.5f), Offset(size.width * 0.66f, size.height * 0.5f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            ShopIconKind.Paw -> {
                drawCircle(tint, size.minDimension * 0.12f, Offset(size.width * 0.5f, size.height * 0.58f))
                drawCircle(tint, size.minDimension * 0.07f, Offset(size.width * 0.32f, size.height * 0.38f))
                drawCircle(tint, size.minDimension * 0.07f, Offset(size.width * 0.45f, size.height * 0.28f))
                drawCircle(tint, size.minDimension * 0.07f, Offset(size.width * 0.58f, size.height * 0.28f))
                drawCircle(tint, size.minDimension * 0.07f, Offset(size.width * 0.7f, size.height * 0.38f))
            }
        }
    }
}
