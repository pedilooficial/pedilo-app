package com.pedilo.app.core.port

import android.net.Uri
import com.pedilo.app.core.model.PublicConfiguration
import com.pedilo.app.core.model.PublicConfigurationMutationResult
import com.pedilo.app.core.model.PublicConfigurationUpdateRequest
import com.pedilo.app.core.result.CoreResult
import kotlinx.coroutines.flow.Flow

interface PublicConfigurationPort {
    fun observePublicConfiguration(): Flow<CoreResult<PublicConfiguration>>
    suspend fun updatePublicConfiguration(request: PublicConfigurationUpdateRequest): CoreResult<PublicConfigurationMutationResult>
    suspend fun uploadPublicImage(localUri: Uri, pathSegment: String): CoreResult<String>
}
