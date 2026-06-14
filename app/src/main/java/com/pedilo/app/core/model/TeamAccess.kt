package com.pedilo.app.core.model

enum class TeamRole(val wireName: String, val screenTitle: String) {
    Admin("admin", "Pantalla Admin"),
    Local("store", "Pantalla Local"),
    Driver("driver", "Pantalla Repartidor");

    companion object {
        fun fromWire(value: String): TeamRole? = when (value.trim().lowercase()) {
            "admin" -> Admin
            "store", "local" -> Local
            "driver", "repartidor" -> Driver
            else -> null
        }
    }
}

data class TeamSession(
    val uid: String,
    val email: String,
    val displayName: String,
    val role: TeamRole,
    val keepSignedIn: Boolean,
)

data class TeamLoginRequest(
    val user: String,
    val secret: String,
    val keepSignedIn: Boolean,
)

sealed interface TeamLoginResult {
    data class Success(val session: TeamSession) : TeamLoginResult
    data object NoAccess : TeamLoginResult
}
