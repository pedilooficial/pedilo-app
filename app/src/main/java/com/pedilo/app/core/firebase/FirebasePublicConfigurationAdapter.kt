package com.pedilo.app.core.firebase

import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.pedilo.app.core.model.DEFAULT_INFORMATION_BUTTON_TEXT
import com.pedilo.app.core.model.DEFAULT_INFORMATION_IMAGE_URL
import com.pedilo.app.core.model.DEFAULT_INFORMATION_TEXT
import com.pedilo.app.core.model.DEFAULT_QUICK_ACCESS_IMAGE_URL
import com.pedilo.app.core.model.DEFAULT_SUBTITLE
import com.pedilo.app.core.model.DEFAULT_TITLE
import com.pedilo.app.core.model.PublicAdvertisingConfig
import com.pedilo.app.core.model.PublicConfiguration
import com.pedilo.app.core.model.PublicConfigurationMutationResult
import com.pedilo.app.core.model.PublicConfigurationUpdateRequest
import com.pedilo.app.core.model.PublicDailyInfoConfig
import com.pedilo.app.core.model.PublicInformationConfig
import com.pedilo.app.core.model.PublicQuickAccessConfig
import com.pedilo.app.core.model.defaultQuickAccessConfig
import com.pedilo.app.core.port.PublicConfigurationPort
import com.pedilo.app.core.result.CoreError
import com.pedilo.app.core.result.CoreResult
import java.util.UUID
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebasePublicConfigurationAdapter(
    private val db: FirebaseFirestore = Firebase.firestore,
    private val functions: FirebaseFunctions = Firebase.functions(REGION),
    private val storage: FirebaseStorage = Firebase.storage,
) : PublicConfigurationPort {
    override fun observePublicConfiguration(): Flow<CoreResult<PublicConfiguration>> =
        callbackFlow {
            val registration = db.collection(PUBLIC_CONFIG).document(PUBLIC_CONFIG_HOME)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(CoreResult.Failure(CoreError.NotAvailable))
                        return@addSnapshotListener
                    }
                    trySend(CoreResult.Success(snapshot.toPublicConfiguration()))
                }
            awaitClose { registration.remove() }
        }

    override suspend fun updatePublicConfiguration(
        request: PublicConfigurationUpdateRequest,
    ): CoreResult<PublicConfigurationMutationResult> =
        runCatching {
            val result = functions.getHttpsCallable(ADMIN_UPDATE_PUBLIC_CONFIG).call(request.toCallablePayload()).await()
            @Suppress("UNCHECKED_CAST")
            val data = result.getData() as? Map<String, Any?> ?: emptyMap()
            PublicConfigurationMutationResult(
                message = data["message"].asText().ifBlank { "Configuración pública guardada." },
                configuration = data["config"].asMap().takeIf { it.isNotEmpty() }?.toPublicConfiguration(),
            )
        }.fold(
            onSuccess = { CoreResult.Success(it) },
            onFailure = {
                CoreResult.Failure(
                    CoreError.Operational(
                        (it as? FirebaseFunctionsException)?.message ?: "No pudimos guardar la configuración pública.",
                    ),
                )
            },
        )

    override suspend fun uploadPublicImage(localUri: Uri, pathSegment: String): CoreResult<String> =
        runCatching {
            val cleanSegment = pathSegment.filter { it.isLetterOrDigit() || it == '-' || it == '_' }
                .ifBlank { "public" }
            val ref = storage.reference
                .child("public_config")
                .child(cleanSegment)
                .child("${UUID.randomUUID()}.jpg")
            ref.putFile(localUri).await()
            ref.downloadUrl.await().toString()
        }.fold(
            onSuccess = { CoreResult.Success(it) },
            onFailure = { CoreResult.Failure(CoreError.Operational("No pudimos subir la imagen.")) },
        )

    private fun DocumentSnapshot?.toPublicConfiguration(): PublicConfiguration =
        if (this == null || !exists()) {
            PublicConfiguration()
        } else {
            data.orEmpty().toPublicConfiguration((get(UPDATED_AT) as? Timestamp)?.toDate()?.time)
        }

    private fun Map<String, Any?>.toPublicConfiguration(updatedAtMillis: Long? = null): PublicConfiguration {
        val quickAccess = this[QUICK_ACCESS].asMapList()
            .map { it.toQuickAccess() }
            .takeIf { it.size == REQUIRED_QUICK_ACCESS_COUNT }
            ?: defaultQuickAccessConfig()
        return PublicConfiguration(
            title = this[TITLE].asText().ifBlank { DEFAULT_TITLE },
            subtitle = this[SUBTITLE].asText().ifBlank { DEFAULT_SUBTITLE },
            quickAccess = quickAccess,
            advertising = this[ADVERTISING].asMap().toAdvertising(),
            information = this[INFORMATION].asMap().toInformation(),
            updatedAtMillis = updatedAtMillis,
        )
    }

    private fun Map<String, Any?>.toQuickAccess(): PublicQuickAccessConfig =
        PublicQuickAccessConfig(
            title = this[TITLE].asText(),
            storeType = this[STORE_TYPE].asText(),
            imageUrl = this[IMAGE_URL].asText().ifBlank { DEFAULT_QUICK_ACCESS_IMAGE_URL },
        )

    private fun Map<String, Any?>.toAdvertising(): PublicAdvertisingConfig =
        PublicAdvertisingConfig(
            text = this[TEXT].asText(),
            imageUrls = this[IMAGE_URLS].asTextList().take(3),
        )

    private fun Map<String, Any?>.toInformation(): PublicInformationConfig =
        PublicInformationConfig(
            mainText = this[MAIN_TEXT].asText().ifBlank { DEFAULT_INFORMATION_TEXT },
            mainImageUrl = this[MAIN_IMAGE_URL].asText().ifBlank { DEFAULT_INFORMATION_IMAGE_URL },
            buttonText = this[BUTTON_TEXT].asText().ifBlank { DEFAULT_INFORMATION_BUTTON_TEXT },
            daily = this[DAILY].asMap().toDaily(),
            importantNotice = this[IMPORTANT_NOTICE].asText(),
            usefulTip = this[USEFUL_TIP].asText(),
            news = this[NEWS].asText(),
        )

    private fun Map<String, Any?>.toDaily(): PublicDailyInfoConfig =
        PublicDailyInfoConfig(
            text = this[TEXT].asText(),
            description = this[DESCRIPTION].asText(),
            imageUrl = this[IMAGE_URL].asText(),
        )

    private fun PublicConfigurationUpdateRequest.toCallablePayload(): Map<String, Any?> =
        buildMap {
            title?.let { put(TITLE, it) }
            subtitle?.let { put(SUBTITLE, it) }
            quickAccess?.let { items ->
                put(QUICK_ACCESS, items.map {
                    mapOf(TITLE to it.title, STORE_TYPE to it.storeType, IMAGE_URL to it.imageUrl)
                })
            }
            advertising?.let {
                put(ADVERTISING, mapOf(TEXT to it.text, IMAGE_URLS to it.imageUrls.take(3)))
            }
            information?.let {
                put(
                    INFORMATION,
                    mapOf(
                        MAIN_TEXT to it.mainText,
                        MAIN_IMAGE_URL to it.mainImageUrl,
                        BUTTON_TEXT to it.buttonText,
                        DAILY to mapOf(TEXT to it.daily.text, DESCRIPTION to it.daily.description, IMAGE_URL to it.daily.imageUrl),
                        IMPORTANT_NOTICE to it.importantNotice,
                        USEFUL_TIP to it.usefulTip,
                        NEWS to it.news,
                    ),
                )
            }
        }

    private fun Any?.asText(): String = this as? String ?: ""

    @Suppress("UNCHECKED_CAST")
    private fun Any?.asMap(): Map<String, Any?> = this as? Map<String, Any?> ?: emptyMap()

    @Suppress("UNCHECKED_CAST")
    private fun Any?.asMapList(): List<Map<String, Any?>> =
        (this as? List<*>).orEmpty().mapNotNull { it as? Map<String, Any?> }

    private fun Any?.asTextList(): List<String> =
        (this as? List<*>).orEmpty().mapNotNull { it as? String }.filter { it.isNotBlank() }

    private companion object {
        const val REGION = "southamerica-east1"
        const val PUBLIC_CONFIG = "public_config"
        const val PUBLIC_CONFIG_HOME = "home"
        const val ADMIN_UPDATE_PUBLIC_CONFIG = "adminUpdatePublicConfig"
        const val REQUIRED_QUICK_ACCESS_COUNT = 4
        const val TITLE = "title"
        const val SUBTITLE = "subtitle"
        const val QUICK_ACCESS = "quickAccess"
        const val STORE_TYPE = "storeType"
        const val IMAGE_URL = "imageUrl"
        const val ADVERTISING = "advertising"
        const val TEXT = "text"
        const val IMAGE_URLS = "imageUrls"
        const val INFORMATION = "information"
        const val MAIN_TEXT = "mainText"
        const val MAIN_IMAGE_URL = "mainImageUrl"
        const val BUTTON_TEXT = "buttonText"
        const val DAILY = "daily"
        const val DESCRIPTION = "description"
        const val IMPORTANT_NOTICE = "importantNotice"
        const val USEFUL_TIP = "usefulTip"
        const val NEWS = "news"
        const val UPDATED_AT = "updatedAt"
    }
}
