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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pedilo.app.core.model.PublicProductSummary
import com.pedilo.app.core.model.PublicStoreSummary

private enum class SearchFilter(val label: String) {
    Nearby("Más cercanos"),
    Rated("Mejor puntuados"),
    Fast("Entrega rápida"),
}

private data class SearchStore(
    val id: String,
    val name: String,
    val description: String,
    val rating: String,
    val reviews: String,
    val distance: String,
    val eta: String,
    val delivery: String,
    val badge: String? = null,
    val icon: SearchIconKind = SearchIconKind.Shop,
)

private fun realStoresForQuery(query: String, catalogState: PublicCatalogState): List<SearchStore> {
    if (!catalogState.hasRealCatalog) return emptyList()
    val normalized = query.trim().lowercase()
    if (normalized.isBlank()) return catalogState.stores.map { it.toSearchStore() }
    return catalogState.stores
        .filter { store ->
            store.name.contains(normalized, ignoreCase = true) ||
                store.category.contains(normalized, ignoreCase = true) ||
                store.description.contains(normalized, ignoreCase = true) ||
                catalogState.productsByStore[store.id].orEmpty().any { it.matches(normalized) }
        }
        .map { it.toSearchStore() }
}

private fun PublicProductSummary.matches(query: String): Boolean =
    name.contains(query, ignoreCase = true) || description.contains(query, ignoreCase = true)

private fun PublicStoreSummary.toSearchStore(): SearchStore =
    SearchStore(
        id = id,
        name = name,
        description = description.ifBlank { category },
        rating = "Nuevo",
        reviews = "",
        distance = "",
        eta = openingHours ?: if (isOpen) "Abierto" else "Cerrado",
        delivery = "",
        icon = if (category.contains("pizza", ignoreCase = true)) SearchIconKind.Pizza else SearchIconKind.Shop,
    )

@Composable
fun PublicShopSearchScreen(
    query: String,
    current: PublicBottomDestination = PublicBottomDestination.Shop,
    catalogState: PublicCatalogState = PublicCatalogState(isLoading = false),
    onHome: () -> Unit,
    onPlus: () -> Unit,
    onShop: () -> Unit,
    onViewLocal: (String) -> Unit,
    titleOverride: String? = null,
) {
    var selectedFilter by remember { mutableStateOf(SearchFilter.Nearby) }
    var activeQuery by remember(query) { mutableStateOf(query) }
    val hasQuery = activeQuery.isNotBlank()
    val relatedStores = realStoresForQuery(activeQuery, catalogState)
    val hasRelatedStores = relatedStores.isNotEmpty()
    val listingMode = titleOverride != null

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
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SearchHeader(
                    query = activeQuery,
                    titleOverride = titleOverride,
                )
            }
            if (!listingMode) {
                item {
                    ActiveSearchBox(
                        query = activeQuery,
                        onQueryChange = { activeQuery = it },
                        onClear = { activeQuery = "" },
                    )
                }
            }
            if (listingMode || hasQuery) {
                when {
                    catalogState.isLoading -> item {
                        SearchStatusMessage("Cargando locales...")
                    }
                    catalogState.loadFailed -> item {
                        SearchStatusMessage("No pudimos cargar los locales.")
                    }
                    hasRelatedStores -> {
                        items(relatedStores) { store ->
                            SearchResultCard(
                                store = store,
                                onView = onViewLocal,
                            )
                        }
                    }
                    else -> item {
                        SearchStatusMessage("Todavía no hay locales disponibles.")
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHeader(query: String, titleOverride: String?) {
    val hasQuery = query.isNotBlank()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titleOverride ?: if (hasQuery) "Resultado: ${query.trim()}" else "Pédilo!",
                fontSize = 24.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.ExtraBold,
                style = TextStyle(brush = if (titleOverride == null && !hasQuery) Brush.verticalGradient(listOf(PediloWarning, PediloOrangeSoft, PediloOrangeDark)) else Brush.verticalGradient(listOf(PediloText, PediloText))),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (hasQuery && titleOverride == null) {
                Text(
                    text = "Locales relacionados",
                    color = PediloMuted,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ActiveSearchBox(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .pediloCardDepth(RoundedCornerShape(15.dp))
            .background(Brush.horizontalGradient(listOf(PediloPanelSoft, PediloPanel, PediloPanelSoft)), RoundedCornerShape(15.dp))
            .border(1.dp, PediloGoldLine.copy(alpha = 0.52f), RoundedCornerShape(15.dp))
            .semantics { contentDescription = "Búsqueda activa" }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SearchIcon(SearchIconKind.Search, tint = PediloText, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            textStyle = TextStyle(color = PediloText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
            singleLine = true,
            cursorBrush = SolidColor(PediloText),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text(
                        text = "¿Qué estás buscando?",
                        color = PediloMuted,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                innerTextField()
            },
        )
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clickable(role = Role.Button, onClick = onClear)
                .semantics { contentDescription = "Limpiar búsqueda" },
            contentAlignment = Alignment.Center,
        ) {
            SearchIcon(SearchIconKind.Close, tint = PediloText, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun SearchStatusMessage(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pediloCardDepth(RoundedCornerShape(15.dp))
            .background(PediloCardBrush, RoundedCornerShape(15.dp))
            .border(1.dp, PediloLine.copy(alpha = 0.90f), RoundedCornerShape(15.dp))
            .padding(16.dp),
    ) {
        Text(
            text = message,
            color = PediloText,
            fontSize = 19.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SearchFilterRow(
    selected: SearchFilter,
    onSelected: (SearchFilter) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SearchFilter.entries.forEach { filter ->
            val active = selected == filter
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .background(
                        if (active) Brush.verticalGradient(listOf(PediloOrangeSoft, PediloOrange)) else Brush.verticalGradient(listOf(PediloPanel, PediloPanel)),
                        RoundedCornerShape(21.dp),
                    )
                    .border(1.dp, if (active) PediloOrange else PediloLine, RoundedCornerShape(21.dp))
                    .clickable(role = Role.Button) { onSelected(filter) }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = filter.label,
                    color = if (active) Color.White else PediloMuted,
                    fontSize = 11.sp,
                    lineHeight = 12.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    store: SearchStore,
    onView: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(136.dp)
            .pediloCardDepth(RoundedCornerShape(15.dp))
            .background(PediloCardBrush, RoundedCornerShape(15.dp))
            .border(1.dp, PediloLine.copy(alpha = 0.90f), RoundedCornerShape(15.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SearchStoreThumbnail(store = store)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = store.name,
                color = PediloText,
                fontSize = 14.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = store.description,
                color = PediloMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SearchIcon(SearchIconKind.Star, tint = PediloOrange, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text(store.rating, color = PediloOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                if (store.reviews.isNotBlank()) {
                    Spacer(Modifier.width(5.dp))
                    Text("(${store.reviews})", color = PediloMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (store.eta.isNotBlank()) {
                    Text(" · ${store.eta}", color = PediloMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(6.dp))
            if (store.distance.isNotBlank() || store.delivery.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SearchIcon(SearchIconKind.Delivery, tint = PediloOrange, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    val deliveryText = listOf(
                        store.distance.takeIf { it.isNotBlank() },
                        store.delivery.takeIf { it.isNotBlank() }?.let { "Envío $it" },
                    ).filterNotNull().joinToString(" · ")
                    Text(deliveryText, color = PediloMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .width(68.dp)
                .height(34.dp)
                .pediloButtonDepth(RoundedCornerShape(10.dp))
                .background(PediloPrimaryBrush, RoundedCornerShape(10.dp))
                .clickable(role = Role.Button, onClick = { onView(store.id) }),
            contentAlignment = Alignment.Center,
        ) {
            Text("Ver local", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun SearchStoreThumbnail(store: SearchStore) {
    Box(
        modifier = Modifier
            .size(width = 78.dp, height = 108.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(
                Brush.radialGradient(
                    listOf(PediloWarning, PediloOrangeDark, PediloPanelSoft),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        SearchIcon(store.icon, tint = Color.White, modifier = Modifier.size(62.dp))
        store.badge?.let { badge ->
            Text(
                text = badge,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .background(PediloOrange, RoundedCornerShape(10.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
            )
        }
    }
}

private enum class SearchIconKind {
    Search,
    Close,
    Star,
    Delivery,
    Pizza,
    Shop,
    Paw,
    Pharmacy,
    Drink,
}

@Composable
private fun SearchIcon(
    kind: SearchIconKind,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = size.minDimension * 0.1f, cap = StrokeCap.Round)
        when (kind) {
            SearchIconKind.Search -> {
                drawCircle(tint, size.minDimension * 0.28f, Offset(size.width * 0.42f, size.height * 0.42f), style = stroke)
                drawLine(tint, Offset(size.width * 0.62f, size.height * 0.62f), Offset(size.width * 0.82f, size.height * 0.82f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            SearchIconKind.Close -> {
                drawLine(tint, Offset(size.width * 0.25f, size.height * 0.25f), Offset(size.width * 0.75f, size.height * 0.75f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.75f, size.height * 0.25f), Offset(size.width * 0.25f, size.height * 0.75f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            SearchIconKind.Star -> {
                val star = Path().apply {
                    moveTo(size.width * 0.5f, size.height * 0.16f)
                    lineTo(size.width * 0.6f, size.height * 0.42f)
                    lineTo(size.width * 0.86f, size.height * 0.44f)
                    lineTo(size.width * 0.66f, size.height * 0.6f)
                    lineTo(size.width * 0.72f, size.height * 0.86f)
                    lineTo(size.width * 0.5f, size.height * 0.72f)
                    lineTo(size.width * 0.28f, size.height * 0.86f)
                    lineTo(size.width * 0.34f, size.height * 0.6f)
                    lineTo(size.width * 0.14f, size.height * 0.44f)
                    lineTo(size.width * 0.4f, size.height * 0.42f)
                    close()
                }
                drawPath(star, tint)
            }
            SearchIconKind.Delivery -> {
                drawRoundRect(tint, Offset(size.width * 0.18f, size.height * 0.42f), Size(size.width * 0.45f, size.height * 0.26f), CornerRadius(size.width * 0.05f), style = stroke)
                drawLine(tint, Offset(size.width * 0.62f, size.height * 0.5f), Offset(size.width * 0.78f, size.height * 0.5f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawCircle(tint, size.minDimension * 0.06f, Offset(size.width * 0.34f, size.height * 0.78f))
                drawCircle(tint, size.minDimension * 0.06f, Offset(size.width * 0.7f, size.height * 0.78f))
            }
            SearchIconKind.Pizza -> {
                val slice = Path().apply {
                    moveTo(size.width * 0.2f, size.height * 0.18f)
                    lineTo(size.width * 0.82f, size.height * 0.34f)
                    lineTo(size.width * 0.42f, size.height * 0.86f)
                    close()
                }
                drawPath(slice, tint, style = stroke)
                drawCircle(tint, size.minDimension * 0.05f, Offset(size.width * 0.48f, size.height * 0.44f))
                drawCircle(tint, size.minDimension * 0.05f, Offset(size.width * 0.58f, size.height * 0.58f))
            }
            SearchIconKind.Shop -> {
                drawRoundRect(tint, Offset(size.width * 0.18f, size.height * 0.36f), Size(size.width * 0.64f, size.height * 0.46f), CornerRadius(size.width * 0.06f), style = stroke)
                drawLine(tint, Offset(size.width * 0.22f, size.height * 0.36f), Offset(size.width * 0.32f, size.height * 0.18f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.78f, size.height * 0.36f), Offset(size.width * 0.68f, size.height * 0.18f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.32f, size.height * 0.18f), Offset(size.width * 0.68f, size.height * 0.18f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            SearchIconKind.Paw -> {
                drawCircle(tint, size.minDimension * 0.11f, Offset(size.width * 0.5f, size.height * 0.62f))
                drawCircle(tint, size.minDimension * 0.07f, Offset(size.width * 0.32f, size.height * 0.42f))
                drawCircle(tint, size.minDimension * 0.07f, Offset(size.width * 0.46f, size.height * 0.32f))
                drawCircle(tint, size.minDimension * 0.07f, Offset(size.width * 0.6f, size.height * 0.32f))
                drawCircle(tint, size.minDimension * 0.07f, Offset(size.width * 0.74f, size.height * 0.42f))
            }
            SearchIconKind.Pharmacy -> {
                drawRoundRect(tint, Offset(size.width * 0.2f, size.height * 0.24f), Size(size.width * 0.6f, size.height * 0.56f), CornerRadius(size.width * 0.08f), style = stroke)
                drawLine(tint, Offset(size.width * 0.5f, size.height * 0.36f), Offset(size.width * 0.5f, size.height * 0.68f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.34f, size.height * 0.52f), Offset(size.width * 0.66f, size.height * 0.52f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            SearchIconKind.Drink -> {
                drawRoundRect(tint, Offset(size.width * 0.34f, size.height * 0.18f), Size(size.width * 0.32f, size.height * 0.66f), CornerRadius(size.width * 0.08f), style = stroke)
                drawLine(tint, Offset(size.width * 0.42f, size.height * 0.18f), Offset(size.width * 0.42f, size.height * 0.08f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.42f, size.height * 0.08f), Offset(size.width * 0.66f, size.height * 0.08f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.38f, size.height * 0.52f), Offset(size.width * 0.62f, size.height * 0.52f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
        }
    }
}
