package com.pedilo.app.core.model

data class PublicConfiguration(
    val title: String = DEFAULT_TITLE,
    val subtitle: String = DEFAULT_SUBTITLE,
    val quickAccess: List<PublicQuickAccessConfig> = defaultQuickAccessConfig(),
    val advertising: PublicAdvertisingConfig = PublicAdvertisingConfig(),
    val information: PublicInformationConfig = PublicInformationConfig(),
    val updatedAtMillis: Long? = null,
)

data class PublicQuickAccessConfig(
    val title: String,
    val storeType: String,
    val imageUrl: String,
)

data class PublicAdvertisingConfig(
    val text: String = "",
    val imageUrls: List<String> = emptyList(),
) {
    val isVisible: Boolean get() = imageUrls.any { it.isNotBlank() }
}

data class PublicInformationConfig(
    val mainText: String = DEFAULT_INFORMATION_TEXT,
    val mainImageUrl: String = DEFAULT_INFORMATION_IMAGE_URL,
    val buttonText: String = DEFAULT_INFORMATION_BUTTON_TEXT,
    val daily: PublicDailyInfoConfig = PublicDailyInfoConfig(),
    val importantNotice: String = "",
    val usefulTip: String = "",
    val news: String = "",
)

data class PublicDailyInfoConfig(
    val text: String = "",
    val description: String = "",
    val imageUrl: String = "",
) {
    val isVisible: Boolean get() = text.isNotBlank() || description.isNotBlank() || imageUrl.isNotBlank()
}

data class PublicConfigurationUpdateRequest(
    val title: String? = null,
    val subtitle: String? = null,
    val quickAccess: List<PublicQuickAccessConfig>? = null,
    val advertising: PublicAdvertisingConfig? = null,
    val information: PublicInformationConfig? = null,
)

data class PublicConfigurationMutationResult(
    val message: String,
    val configuration: PublicConfiguration? = null,
)

fun defaultQuickAccessConfig(): List<PublicQuickAccessConfig> = listOf(
    PublicQuickAccessConfig("Supermercado", "Supermercado", DEFAULT_QUICK_ACCESS_IMAGE_URL),
    PublicQuickAccessConfig("Bebidas", "Bebidas", DEFAULT_QUICK_ACCESS_IMAGE_URL),
    PublicQuickAccessConfig("Farmacia", "Farmacia", DEFAULT_QUICK_ACCESS_IMAGE_URL),
    PublicQuickAccessConfig("Mascotas", "Mascotas", DEFAULT_QUICK_ACCESS_IMAGE_URL),
)

const val DEFAULT_TITLE = "Pédilo!"
const val DEFAULT_SUBTITLE = "todos tus pedidos en un solo lugar"
const val DEFAULT_QUICK_ACCESS_IMAGE_URL = "pedilo://default/quick-access"
const val DEFAULT_INFORMATION_TEXT = "Tus locales favoritos, ahora más cerca."
const val DEFAULT_INFORMATION_BUTTON_TEXT = "Ver más"
const val DEFAULT_INFORMATION_IMAGE_URL = "pedilo://default/information"
