package com.pedilo.app.core.port

import com.pedilo.app.core.model.TeamLoginRequest
import com.pedilo.app.core.model.TeamLoginResult

interface TeamAccessPort {
    suspend fun login(request: TeamLoginRequest): TeamLoginResult
    fun signOut()
}
