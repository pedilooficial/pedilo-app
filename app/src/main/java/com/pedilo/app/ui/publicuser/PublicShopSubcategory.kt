package com.pedilo.app.ui.publicuser

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pedilo.app.core.model.PublicStoreSummary

private fun storesForSubcategory(title: String, catalogState: PublicCatalogState): List<PublicStoreSummary> {
    val terms = searchTermsForSubcategory(title)
    if (terms.isEmpty()) return catalogState.stores
    return catalogState.stores.filter { store ->
        terms.any { term ->
            store.name.contains(term, ignoreCase = true) ||
                store.category.contains(term, ignoreCase = true) ||
                store.description.contains(term, ignoreCase = true)
        } ||
            catalogState.productsByStore[store.id].orEmpty().any {
                product -> terms.any { term ->
                    product.name.contains(term, ignoreCase = true) ||
                        product.description.contains(term, ignoreCase = true)
                }
            }
    }
}

private fun searchTermsForSubcategory(title: String): List<String> {
    val normalized = title.trim().lowercase()
    if (normalized.isBlank()) return emptyList()
    val aliases = when (normalized) {
        "pizzas" -> listOf("pizza", "pizzería", "pizzeria")
        "bebidas" -> listOf("bebida")
        else -> emptyList()
    }
    return (listOf(normalized, normalized.removeSuffix("s")) + aliases)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
}

@Composable
fun PublicShopSubcategoryScreen(
    title: String,
    catalogState: PublicCatalogState = PublicCatalogState(isLoading = false),
    onHome: () -> Unit,
    onPlus: () -> Unit,
    onShop: () -> Unit,
    onViewLocal: (String) -> Unit,
) {
    val stores = remember(title, catalogState) { storesForSubcategory(title, catalogState) }

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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SubcategoryHeader(title = title) }
            when {
                catalogState.isLoading -> item { SubcategoryStatus("Cargando locales...") }
                catalogState.loadFailed -> item { SubcategoryStatus("No pudimos cargar los locales.") }
                stores.isEmpty() -> item { SubcategoryStatus("Todavía no hay locales disponibles.") }
                else -> {
                    item {
                        Text(
                            text = "${stores.size} locales encontrados",
                            color = PediloMuted,
                            fontSize = 16.sp,
                        )
                    }
                    items(stores) { store ->
                        RelatedStoreCard(store = store, onView = onViewLocal)
                    }
                }
            }
        }
    }
}

@Composable
private fun SubcategoryHeader(title: String) {
    Text(
        text = title,
        fontSize = 30.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.ExtraBold,
        style = TextStyle(brush = Brush.verticalGradient(listOf(PediloWarning, PediloOrangeSoft, PediloOrangeDark))),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun SubcategoryStatus(message: String) {
    Text(
        text = message,
        color = PediloText,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .pediloCardDepth(RoundedCornerShape(15.dp))
            .background(PediloCardBrush, RoundedCornerShape(15.dp))
            .border(1.dp, PediloLine.copy(alpha = 0.90f), RoundedCornerShape(15.dp))
            .padding(16.dp),
    )
}

@Composable
private fun RelatedStoreCard(store: PublicStoreSummary, onView: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
            .pediloCardDepth(RoundedCornerShape(15.dp))
            .background(PediloCardBrush, RoundedCornerShape(15.dp))
            .border(1.dp, PediloLine.copy(alpha = 0.90f), RoundedCornerShape(15.dp))
            .clickable(role = Role.Button, onClick = { onView(store.id) })
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 88.dp, height = 104.dp)
                .background(Brush.radialGradient(listOf(PediloWarning, PediloOrangeDark, PediloPanelSoft)), RoundedCornerShape(12.dp)),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(store.name, color = PediloText, fontSize = 16.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(store.description.ifBlank { store.category }, color = PediloMuted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Text(if (store.isOpen) "Abierto" else "Cerrado", color = PediloOrange, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(store.openingHours ?: "Horario no informado", color = PediloMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .width(100.dp)
                    .pediloButtonDepth(RoundedCornerShape(10.dp))
                    .background(PediloPrimaryBrush, RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button, onClick = { onView(store.id) }),
                contentAlignment = Alignment.Center,
            ) {
                Text("Ver local", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}
