package com.pedilo.app.ui.publicuser

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
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
import com.pedilo.app.core.model.PublicProductSummary
import com.pedilo.app.core.model.PublicStoreSummary

enum class PublicBottomDestination {
    Home,
    Plus,
    Shop,
}

private enum class PediloIconKind {
    Home,
    Plus,
    Shop,
    Search,
    Cart,
    Bottle,
    Pill,
    Paw,
    Tag,
    Star,
    Clock,
    Megaphone,
    Info,
    Check,
}

private data class QuickAccess(
    val title: String,
    val icon: PediloIconKind,
    val query: String,
)

private val quickAccessItems = listOf(
    QuickAccess("Supermercado", PediloIconKind.Cart, "Supermercado"),
    QuickAccess("Bebidas", PediloIconKind.Bottle, "Bebidas"),
    QuickAccess("Farmacia", PediloIconKind.Pill, "Farmacia"),
    QuickAccess("Mascotas", PediloIconKind.Paw, "Mascotas"),
)

@Composable
fun PublicHomeScreen(
    catalogState: PublicCatalogState = PublicCatalogState(isLoading = false),
    onHome: () -> Unit,
    onPlus: () -> Unit,
    onShop: () -> Unit,
    onTeam: () -> Unit,
    onSearch: () -> Unit,
    onConventions: () -> Unit,
    onCategory: (String) -> Unit,
    onOffer: (String) -> Unit,
    onAllOffers: () -> Unit,
    onLocal: (String) -> Unit,
    onAllLocals: () -> Unit,
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
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            item { PublicHeader(onTeam = onTeam) }
            item { SearchBlock(onSearch = onSearch) }
            item { PublicFlowCard(onShop = onShop, onConventions = onConventions) }
            item { QuickAccessSection(onCategory = onCategory) }
            item { OffersSection(catalogState = catalogState, onOffer = onOffer, onAllOffers = onAllOffers) }
            item { LocalsSection(catalogState = catalogState, onLocal = onLocal, onAllLocals = onAllLocals) }
            item { HomeBanner(onConventions = onConventions) }
        }
    }
}

@Composable
fun PublicShell(
    current: PublicBottomDestination,
    onHome: () -> Unit,
    onPlus: () -> Unit,
    onShop: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PediloBg),
    ) {
        content()
        PublicBottomBar(
            current = current,
            onHome = onHome,
            onPlus = onPlus,
            onShop = onShop,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun PublicHeader(onTeam: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Pédilo!",
                fontSize = 50.sp,
                lineHeight = 50.sp,
                fontWeight = FontWeight.ExtraBold,
                style = TextStyle(
                    brush = Brush.verticalGradient(listOf(PediloWarning, PediloOrangeSoft, PediloOrange, PediloOrangeDark)),
                    shadow = Shadow(PediloOrangeDark.copy(alpha = 0.68f), Offset(0f, 5f), 16f),
                ),
            )
            Text(
                text = "todos tus pedidos en un solo lugar",
                color = PediloText,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 14.dp),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp)
                .shadow(5.dp, RoundedCornerShape(13.dp), ambientColor = Color.Black.copy(alpha = 0.24f), spotColor = PediloOrange.copy(alpha = 0.10f))
                .background(Brush.verticalGradient(listOf(PediloPanelSoft.copy(alpha = 0.86f), PediloPanel.copy(alpha = 0.94f))), RoundedCornerShape(13.dp))
                .border(1.dp, PediloOrange.copy(alpha = 0.44f), RoundedCornerShape(13.dp))
                .clickable(role = Role.Button, onClick = onTeam)
                .semantics { contentDescription = "Equipo" }
                .padding(horizontal = 7.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TeamMiniIcon(tint = PediloOrange, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(3.dp))
                Text("Equipo", color = PediloText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SearchBlock(onSearch: () -> Unit) {
    SurfacePanel {
        Text(
            text = "¿Qué necesitás?",
            color = PediloText,
            fontSize = 20.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        SearchInput(onSearch = onSearch)
    }
}

@Composable
private fun SearchInput(onSearch: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .shadow(10.dp, RoundedCornerShape(15.dp), ambientColor = Color.Black.copy(alpha = 0.28f), spotColor = PediloOrange.copy(alpha = 0.10f))
            .background(Brush.horizontalGradient(listOf(PediloPanelSoft, PediloPanel, PediloPanelSoft)), RoundedCornerShape(15.dp))
            .border(1.dp, PediloGoldLine.copy(alpha = 0.55f), RoundedCornerShape(15.dp))
            .clickable(role = Role.Button, onClick = onSearch)
            .semantics { contentDescription = "Buscar desde Home" }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PediloLineIcon(PediloIconKind.Search, tint = PediloMuted, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            Text(
                text = "Buscar productos, locales o categorías...",
                color = PediloMuted,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PublicFlowCard(onShop: () -> Unit, onConventions: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pediloCardDepth(RoundedCornerShape(15.dp))
            .background(PediloCardBrush, RoundedCornerShape(15.dp))
            .border(1.dp, PediloLine.copy(alpha = 0.90f), RoundedCornerShape(15.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Pedí sin vueltas", color = PediloText, fontSize = 17.sp, lineHeight = 20.sp, fontWeight = FontWeight.ExtraBold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PublicFlowStep("1", "Local", Modifier.weight(1f))
            PublicFlowStep("2", "Datos", Modifier.weight(1f))
            PublicFlowStep("3", "Seguí", Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PublicFlowButton("Ver tienda", PediloIconKind.Shop, onShop, Modifier.weight(1f))
            PublicFlowButton("Ayuda", PediloIconKind.Check, onConventions, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PublicFlowStep(number: String, label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(PediloPanelSoft, RoundedCornerShape(10.dp))
            .border(1.dp, PediloLine, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(PediloOrange, RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(number, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.width(6.dp))
        Text(label, color = PediloMuted, fontSize = 11.sp, lineHeight = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PublicFlowButton(label: String, icon: PediloIconKind, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(44.dp)
            .background(PediloPanelSoft, RoundedCornerShape(12.dp))
            .border(1.dp, PediloGoldLine.copy(alpha = 0.48f), RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        PediloLineIcon(icon, tint = PediloOrange, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(7.dp))
        Text(label, color = PediloText, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun QuickAccessSection(onCategory: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        quickAccessItems.forEach { item ->
            QuickAccessCard(item, onClick = { onCategory(item.query) }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun QuickAccessCard(item: QuickAccess, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .height(78.dp)
            .shadow(9.dp, RoundedCornerShape(12.dp), ambientColor = Color.Black.copy(alpha = 0.30f), spotColor = PediloOrange.copy(alpha = 0.10f))
            .background(Brush.verticalGradient(listOf(PediloPanelSoft, PediloPanel)), RoundedCornerShape(12.dp))
            .border(1.dp, PediloLine.copy(alpha = 0.86f), RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = item.title }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PediloLineIcon(item.icon, tint = PediloOrange, modifier = Modifier.size(25.dp))
        Spacer(Modifier.height(5.dp))
        Text(
            text = item.title,
            color = PediloText,
            fontSize = 9.sp,
            lineHeight = 10.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun OffersSection(catalogState: PublicCatalogState, onOffer: (String) -> Unit, onAllOffers: () -> Unit) {
    val promo = remember(catalogState) { catalogState.realPromoProduct() }
    SectionHeader(icon = PediloIconKind.Tag, title = "Ofertas", action = "Ver todas", onAction = onAllOffers)
    when {
        catalogState.isLoading -> EmptyCatalogCard("Cargando ofertas...")
        catalogState.loadFailed -> EmptyCatalogCard("No pudimos cargar las ofertas.")
        promo == null -> EmptyCatalogCard("Todavía no hay ofertas disponibles.")
        else -> OfferCard(product = promo, onClick = { onOffer(promo.storeId) })
    }
}

private fun PublicCatalogState.realPromoProduct(): PublicProductSummary? =
    productsByStore.values
        .flatten()
        .firstOrNull { product ->
            product.visible &&
                product.available &&
                (product.id.contains("promo", ignoreCase = true) ||
                    product.name.contains("promo", ignoreCase = true) ||
                    product.description.contains("promo", ignoreCase = true))
        }

@Composable
private fun OfferCard(product: PublicProductSummary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp)
            .shadow(12.dp, RoundedCornerShape(12.dp), ambientColor = Color.Black.copy(alpha = 0.32f), spotColor = PediloOrange.copy(alpha = 0.10f))
            .background(Brush.verticalGradient(listOf(PediloPanelSoft, PediloPanel)), RoundedCornerShape(12.dp))
            .border(1.dp, PediloGoldLine.copy(alpha = 0.48f), RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Brush.radialGradient(listOf(PediloWarning, PediloOrangeDark, PediloPanelSoft))),
            contentAlignment = Alignment.Center,
        ) {
            PediloLineIcon(PediloIconKind.Tag, tint = Color.White, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(product.name, color = PediloText, fontSize = 15.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(product.description, color = PediloMuted, fontSize = 11.sp, lineHeight = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            product.priceCents?.let {
                Spacer(Modifier.height(4.dp))
                Text(formatLocalMoney((it / 100L).toInt()), color = PediloOrange, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EmptyCatalogCard(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PediloPanel, RoundedCornerShape(10.dp))
            .border(1.dp, PediloLine, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = message, color = PediloMuted, fontSize = 13.sp, lineHeight = 17.sp)
        Text(
            text = "Cuando el entorno de prueba cargue catálogo autorizado, vas a poder elegir local y confirmar pedido desde acá.",
            color = PediloOrange,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    }
}

@Composable
private fun LocalsSection(catalogState: PublicCatalogState, onLocal: (String) -> Unit, onAllLocals: () -> Unit) {
    SectionHeader(icon = PediloIconKind.Shop, title = "Locales disponibles", action = "Ver todos", onAction = onAllLocals)
    when {
        catalogState.isLoading -> EmptyCatalogCard("Cargando locales...")
        catalogState.loadFailed -> EmptyCatalogCard("No pudimos cargar los locales.")
        catalogState.stores.isEmpty() -> EmptyCatalogCard("Todavía no hay locales disponibles.")
        else -> LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(catalogState.stores) { local ->
                LocalCard(local, onClick = { onLocal(local.id) })
            }
        }
    }
}

@Composable
private fun LocalCard(local: PublicStoreSummary, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(132.dp)
            .height(142.dp)
            .shadow(12.dp, RoundedCornerShape(12.dp), ambientColor = Color.Black.copy(alpha = 0.34f), spotColor = PediloOrange.copy(alpha = 0.10f))
            .background(Brush.verticalGradient(listOf(PediloPanelSoft, PediloPanel)), RoundedCornerShape(12.dp))
            .border(1.dp, PediloLine.copy(alpha = 0.90f), RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(7.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.radialGradient(listOf(PediloOrangeDark, PediloPanelSoft))),
        ) {
            PediloLineIcon(
                PediloIconKind.Shop,
                tint = PediloWarning,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(34.dp),
            )
            Text(
                text = if (local.isOpen) "Abierto" else "Cerrado",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .background(PediloOrange, RoundedCornerShape(5.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(local.name, color = PediloText, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(local.description.ifBlank { local.category }, color = PediloMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(local.openingHours ?: "Horario no informado", color = PediloOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun HomeBanner(onConventions: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(126.dp)
            .shadow(18.dp, RoundedCornerShape(18.dp), ambientColor = Color.Black.copy(alpha = 0.40f), spotColor = PediloOrange.copy(alpha = 0.26f))
            .background(Brush.horizontalGradient(listOf(PediloOrangeDark, PediloWarmDepth, PediloPanel, PediloPanelSoft)), RoundedCornerShape(18.dp))
            .border(1.dp, PediloWarning.copy(alpha = 0.62f), RoundedCornerShape(18.dp))
            .semantics { contentDescription = "Aviso de envíos más rápidos" }
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Brush.horizontalGradient(listOf(PediloOrangeDark, PediloWarmDepth, PediloPanel, PediloPanelSoft)), RoundedCornerShape(18.dp)),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.10f), Color.Black.copy(alpha = 0.30f), Color.Black.copy(alpha = 0.52f))), RoundedCornerShape(18.dp)),
        )
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(PediloWarning.copy(alpha = 0.10f), radius = size.minDimension * 0.62f, center = Offset(size.width * 0.88f, size.height * 0.46f))
            repeat(3) { index ->
                val y = size.height * (0.32f + index * 0.17f)
                drawLine(
                    color = PediloWarning.copy(alpha = 0.16f - index * 0.03f),
                    start = Offset(size.width * (0.58f + index * 0.05f), y),
                    end = Offset(size.width * 0.95f, y - size.height * 0.10f),
                    strokeWidth = 2.6f,
                    cap = StrokeCap.Round,
                )
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Text(
                "¡Envíos más rápidos!",
                color = Color.White,
                fontSize = 18.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text("Tus locales favoritos, ahora más cerca.", color = Color.White.copy(alpha = 0.92f), fontSize = 12.sp, lineHeight = 15.sp)
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(118.dp)
                    .height(34.dp)
                    .background(Brush.verticalGradient(listOf(Color.White, PediloCream)), RoundedCornerShape(17.dp))
                    .border(1.dp, PediloWarning.copy(alpha = 0.46f), RoundedCornerShape(17.dp))
                    .clickable(role = Role.Button, onClick = onConventions)
                    .semantics { contentDescription = "Ver más" },
                contentAlignment = Alignment.Center,
            ) {
                Text("Ver más", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun TeamMiniIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = size.minDimension * 0.10f, cap = StrokeCap.Round)
        drawCircle(tint, radius = size.minDimension * 0.13f, center = Offset(size.width * 0.37f, size.height * 0.34f), style = stroke)
        drawCircle(tint, radius = size.minDimension * 0.13f, center = Offset(size.width * 0.64f, size.height * 0.34f), style = stroke)
        drawRoundRect(tint, Offset(size.width * 0.16f, size.height * 0.58f), Size(size.width * 0.34f, size.height * 0.22f), CornerRadius(size.width * 0.12f), style = stroke)
        drawRoundRect(tint, Offset(size.width * 0.50f, size.height * 0.58f), Size(size.width * 0.34f, size.height * 0.22f), CornerRadius(size.width * 0.12f), style = stroke)
    }
}

@Composable
private fun SectionHeader(icon: PediloIconKind, title: String, action: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PediloLineIcon(icon, tint = PediloOrange, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, color = PediloText, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier.clickable(role = Role.Button, onClick = onAction),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(action, color = PediloOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(" >", color = PediloOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SurfacePanel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(18.dp), ambientColor = Color.Black.copy(alpha = 0.30f), spotColor = PediloOrange.copy(alpha = 0.08f))
            .background(Brush.verticalGradient(listOf(PediloPanelSoft.copy(alpha = 0.82f), PediloOverlay)), RoundedCornerShape(18.dp))
            .border(1.dp, PediloLine.copy(alpha = 0.92f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        content = content,
    )
}

@Composable
private fun PublicBottomBar(
    current: PublicBottomDestination,
    onHome: () -> Unit,
    onPlus: () -> Unit,
    onShop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp)
                .shadow(14.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), ambientColor = Color.Black.copy(alpha = 0.36f), spotColor = PediloOrange.copy(alpha = 0.10f))
                .background(Brush.verticalGradient(listOf(PediloPanelSoft.copy(alpha = 0.95f), PediloPanel, PediloBg)), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .border(1.dp, PediloLine.copy(alpha = 0.82f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .padding(horizontal = 52.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomItem(
                icon = PediloIconKind.Home,
                label = "Inicio",
                selected = current == PublicBottomDestination.Home,
                onClick = onHome,
            )
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .shadow(12.dp, CircleShape, ambientColor = PediloOrangeDark.copy(alpha = 0.32f), spotColor = PediloOrange.copy(alpha = 0.42f))
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(PediloWarning, PediloOrangeSoft, PediloOrangeDark)))
                    .border(1.dp, PediloWarning.copy(alpha = 0.45f), CircleShape)
                    .clickable(role = Role.Button, onClick = onPlus)
                    .semantics { contentDescription = "Abrir boton mas" },
                contentAlignment = Alignment.Center,
            ) {
                PediloLineIcon(PediloIconKind.Plus, tint = Color.White, modifier = Modifier.size(29.dp))
            }
            BottomItem(
                icon = PediloIconKind.Shop,
                label = "Tienda",
                selected = current == PublicBottomDestination.Shop,
                onClick = onShop,
            )
        }
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
                .background(PediloBg),
        )
    }
}

@Composable
private fun BottomItem(
    icon: PediloIconKind,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PediloLineIcon(icon, tint = if (selected) PediloOrange else PediloMuted, modifier = Modifier.size(25.dp))
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = if (selected) PediloOrange else PediloMuted,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

@Composable
private fun PediloLineIcon(
    kind: PediloIconKind,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = size.minDimension * 0.09f, cap = StrokeCap.Round)
        when (kind) {
            PediloIconKind.Home -> {
                val roof = Path().apply {
                    moveTo(size.width * 0.15f, size.height * 0.48f)
                    lineTo(size.width * 0.5f, size.height * 0.18f)
                    lineTo(size.width * 0.85f, size.height * 0.48f)
                }
                drawPath(roof, tint, style = stroke)
                drawRoundRect(tint, Offset(size.width * 0.25f, size.height * 0.46f), Size(size.width * 0.5f, size.height * 0.38f), CornerRadius(size.width * 0.05f), style = stroke)
            }
            PediloIconKind.Plus -> {
                drawLine(tint, Offset(size.width * 0.5f, size.height * 0.2f), Offset(size.width * 0.5f, size.height * 0.8f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.2f, size.height * 0.5f), Offset(size.width * 0.8f, size.height * 0.5f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            PediloIconKind.Shop -> {
                drawRoundRect(tint, Offset(size.width * 0.2f, size.height * 0.42f), Size(size.width * 0.6f, size.height * 0.42f), CornerRadius(size.width * 0.05f), style = stroke)
                drawLine(tint, Offset(size.width * 0.18f, size.height * 0.38f), Offset(size.width * 0.82f, size.height * 0.38f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.28f, size.height * 0.18f), Offset(size.width * 0.18f, size.height * 0.38f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.72f, size.height * 0.18f), Offset(size.width * 0.82f, size.height * 0.38f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.5f, size.height * 0.18f), Offset(size.width * 0.5f, size.height * 0.38f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            PediloIconKind.Search -> {
                drawCircle(tint, radius = size.minDimension * 0.27f, center = Offset(size.width * 0.43f, size.height * 0.43f), style = stroke)
                drawLine(tint, Offset(size.width * 0.62f, size.height * 0.62f), Offset(size.width * 0.84f, size.height * 0.84f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            PediloIconKind.Cart -> {
                drawLine(tint, Offset(size.width * 0.18f, size.height * 0.25f), Offset(size.width * 0.3f, size.height * 0.25f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawRoundRect(tint, Offset(size.width * 0.32f, size.height * 0.34f), Size(size.width * 0.48f, size.height * 0.28f), CornerRadius(size.width * 0.04f), style = stroke)
                drawCircle(tint, size.minDimension * 0.06f, Offset(size.width * 0.42f, size.height * 0.78f))
                drawCircle(tint, size.minDimension * 0.06f, Offset(size.width * 0.72f, size.height * 0.78f))
            }
            PediloIconKind.Bottle -> {
                drawRoundRect(tint, Offset(size.width * 0.36f, size.height * 0.28f), Size(size.width * 0.28f, size.height * 0.56f), CornerRadius(size.width * 0.09f), style = stroke)
                drawLine(tint, Offset(size.width * 0.42f, size.height * 0.18f), Offset(size.width * 0.58f, size.height * 0.18f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.45f, size.height * 0.18f), Offset(size.width * 0.45f, size.height * 0.28f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.55f, size.height * 0.18f), Offset(size.width * 0.55f, size.height * 0.28f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            PediloIconKind.Pill -> {
                drawRoundRect(tint, Offset(size.width * 0.22f, size.height * 0.35f), Size(size.width * 0.56f, size.height * 0.3f), CornerRadius(size.width * 0.16f), style = stroke)
                drawLine(tint, Offset(size.width * 0.5f, size.height * 0.35f), Offset(size.width * 0.5f, size.height * 0.65f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            PediloIconKind.Paw -> {
                drawCircle(tint, size.minDimension * 0.12f, Offset(size.width * 0.5f, size.height * 0.58f))
                drawCircle(tint, size.minDimension * 0.07f, Offset(size.width * 0.32f, size.height * 0.38f))
                drawCircle(tint, size.minDimension * 0.07f, Offset(size.width * 0.45f, size.height * 0.28f))
                drawCircle(tint, size.minDimension * 0.07f, Offset(size.width * 0.58f, size.height * 0.28f))
                drawCircle(tint, size.minDimension * 0.07f, Offset(size.width * 0.7f, size.height * 0.38f))
            }
            PediloIconKind.Tag -> {
                val tag = Path().apply {
                    moveTo(size.width * 0.2f, size.height * 0.32f)
                    lineTo(size.width * 0.5f, size.height * 0.18f)
                    lineTo(size.width * 0.82f, size.height * 0.5f)
                    lineTo(size.width * 0.5f, size.height * 0.82f)
                    close()
                }
                drawPath(tag, tint, style = stroke)
                drawCircle(tint, size.minDimension * 0.04f, Offset(size.width * 0.47f, size.height * 0.37f))
            }
            PediloIconKind.Star -> {
                val star = Path().apply {
                    moveTo(size.width * 0.5f, size.height * 0.18f)
                    lineTo(size.width * 0.6f, size.height * 0.42f)
                    lineTo(size.width * 0.85f, size.height * 0.44f)
                    lineTo(size.width * 0.66f, size.height * 0.6f)
                    lineTo(size.width * 0.72f, size.height * 0.84f)
                    lineTo(size.width * 0.5f, size.height * 0.7f)
                    lineTo(size.width * 0.28f, size.height * 0.84f)
                    lineTo(size.width * 0.34f, size.height * 0.6f)
                    lineTo(size.width * 0.15f, size.height * 0.44f)
                    lineTo(size.width * 0.4f, size.height * 0.42f)
                    close()
                }
                drawPath(star, tint)
            }
            PediloIconKind.Clock -> {
                drawCircle(tint, size.minDimension * 0.34f, Offset(size.width * 0.5f, size.height * 0.5f), style = stroke)
                drawLine(tint, Offset(size.width * 0.5f, size.height * 0.32f), Offset(size.width * 0.5f, size.height * 0.52f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.5f, size.height * 0.52f), Offset(size.width * 0.64f, size.height * 0.62f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            PediloIconKind.Megaphone -> {
                val horn = Path().apply {
                    moveTo(size.width * 0.2f, size.height * 0.45f)
                    lineTo(size.width * 0.68f, size.height * 0.22f)
                    lineTo(size.width * 0.68f, size.height * 0.78f)
                    lineTo(size.width * 0.2f, size.height * 0.55f)
                    close()
                }
                drawPath(horn, tint, style = stroke)
                drawLine(tint, Offset(size.width * 0.32f, size.height * 0.58f), Offset(size.width * 0.42f, size.height * 0.84f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.78f, size.height * 0.36f), Offset(size.width * 0.9f, size.height * 0.28f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.8f, size.height * 0.5f), Offset(size.width * 0.94f, size.height * 0.5f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.78f, size.height * 0.64f), Offset(size.width * 0.9f, size.height * 0.72f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            PediloIconKind.Info -> {
                drawCircle(tint, size.minDimension * 0.38f, Offset(size.width * 0.5f, size.height * 0.5f), style = stroke)
                drawCircle(tint, size.minDimension * 0.04f, Offset(size.width * 0.5f, size.height * 0.34f))
                drawLine(tint, Offset(size.width * 0.5f, size.height * 0.48f), Offset(size.width * 0.5f, size.height * 0.68f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            PediloIconKind.Check -> {
                drawCircle(tint, size.minDimension * 0.36f, Offset(size.width * 0.5f, size.height * 0.5f), style = stroke)
                drawLine(tint, Offset(size.width * 0.32f, size.height * 0.52f), Offset(size.width * 0.45f, size.height * 0.65f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.45f, size.height * 0.65f), Offset(size.width * 0.72f, size.height * 0.36f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
        }
    }
}
