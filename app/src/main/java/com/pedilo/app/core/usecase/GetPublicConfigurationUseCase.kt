package com.pedilo.app.core.usecase

import android.net.Uri
import com.pedilo.app.core.model.PublicConfigurationUpdateRequest
import com.pedilo.app.core.port.PublicConfigurationPort

class GetPublicConfigurationUseCase(
    private val port: PublicConfigurationPort,
) {
    fun observe() = port.observePublicConfiguration()

    suspend fun savePublicConfiguration(request: PublicConfigurationUpdateRequest) =
        port.updatePublicConfiguration(request)

    suspend fun uploadImage(localUri: Uri, pathSegment: String) =
        port.uploadPublicImage(localUri, pathSegment)
}
