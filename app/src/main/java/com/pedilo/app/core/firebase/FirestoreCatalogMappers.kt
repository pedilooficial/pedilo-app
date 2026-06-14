package com.pedilo.app.core.firebase

import com.google.firebase.firestore.DocumentSnapshot
import com.pedilo.app.core.model.PublicProductSummary
import com.pedilo.app.core.model.PublicStoreSummary

internal fun DocumentSnapshot.toPublicStoreSummaryOrNull(): PublicStoreSummary? {
    if (optionalBoolean("visible") != true) return null
    if (optionalBoolean("operational") == false) return null
    if (optionalBoolean("acceptsOrders") == false) return null

    val name = optionalText("name") ?: return null
    if (isKnownSeedCatalogStore(id, name)) return null
    val category = optionalText("category")
        ?: optionalText("mainCategory")
        ?: optionalText("categoryName")
        ?: ""

    return PublicStoreSummary(
        id = id,
        name = name,
        category = category,
        description = optionalText("description").orEmpty(),
        imageUrl = optionalText("imageUrl"),
        address = optionalText("address"),
        phone = optionalText("phone"),
        openingHours = optionalText("openingHours") ?: optionalText("hours"),
        visible = true,
        isOpen = optionalBoolean("isOpen")
            ?: optionalBoolean("open")
            ?: optionalBoolean("operational")
            ?: optionalBoolean("acceptsOrders")
            ?: true,
    )
}

internal fun DocumentSnapshot.toPublicProductSummaryOrNull(storeId: String): PublicProductSummary? {
    if (optionalBoolean("visible") != true) return null
    if (optionalBoolean("available") == false) return null

    val name = optionalText("name") ?: return null
    val mappedStoreId = optionalText("storeId") ?: storeId

    return PublicProductSummary(
        id = id,
        storeId = mappedStoreId,
        name = name,
        description = optionalText("description").orEmpty(),
        imageUrl = optionalText("imageUrl"),
        priceCents = optionalLong("priceCents") ?: optionalPriceCentsFromPrice(),
        visible = true,
        available = optionalBoolean("available") ?: true,
    )
}

private fun DocumentSnapshot.optionalText(field: String): String? =
    getString(field)?.trim()?.takeIf { it.isNotBlank() }

private fun DocumentSnapshot.optionalBoolean(field: String): Boolean? =
    get(field) as? Boolean

private fun DocumentSnapshot.optionalLong(field: String): Long? =
    when (val value = get(field)) {
        is Long -> value
        is Int -> value.toLong()
        is Double -> value.toLong()
        is Float -> value.toLong()
        else -> null
    }

private fun DocumentSnapshot.optionalPriceCentsFromPrice(): Long? =
    when (val value = get("price")) {
        is Long -> value * 100
        is Int -> value.toLong() * 100
        is Double -> (value * 100).toLong()
        is Float -> (value * 100).toLong()
        else -> null
    }

private fun isKnownSeedCatalogStore(id: String, name: String): Boolean {
    val normalizedName = name.lowercase()
    return id == "pizzeria-roma" || normalizedName == "pizzería roma" || normalizedName == "pizzeria roma"
}
