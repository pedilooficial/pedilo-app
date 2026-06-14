package com.pedilo.app.core.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.pedilo.app.core.model.TeamLoginRequest
import com.pedilo.app.core.model.TeamLoginResult
import com.pedilo.app.core.model.TeamRole
import com.pedilo.app.core.model.TeamSession
import com.pedilo.app.core.port.TeamAccessPort
import kotlinx.coroutines.tasks.await

class FirebaseTeamAccessAdapter(
    private val auth: FirebaseAuth = Firebase.auth,
    private val db: FirebaseFirestore = Firebase.firestore,
) : TeamAccessPort {
    override suspend fun login(request: TeamLoginRequest): TeamLoginResult =
        runCatching {
            val authResult = auth
                .signInWithEmailAndPassword(request.user, request.secret)
                .await()
            val user = authResult.user ?: return@runCatching TeamLoginResult.NoAccess
            val profile = db.collection(USERS).document(user.uid).get().await()
            if (!profile.exists() || profile.getBoolean(ACTIVE) != true) {
                auth.signOut()
                return@runCatching TeamLoginResult.NoAccess
            }

            val role = TeamRole.fromWire(profile.getString(ROLE).orEmpty())
                ?: run {
                    auth.signOut()
                    return@runCatching TeamLoginResult.NoAccess
                }

            val session = TeamSession(
                uid = user.uid,
                email = profile.getString(EMAIL).orEmpty().ifBlank { user.email.orEmpty() },
                displayName = profile.getString(DISPLAY_NAME).orEmpty(),
                role = role,
                keepSignedIn = request.keepSignedIn,
            )
            if (!request.keepSignedIn) {
                auth.signOut()
            }
            TeamLoginResult.Success(session)
        }.getOrElse {
            auth.signOut()
            TeamLoginResult.NoAccess
        }

    override fun signOut() {
        auth.signOut()
    }

    private companion object {
        const val USERS = "users"
        const val ACTIVE = "active"
        const val ROLE = "role"
        const val EMAIL = "email"
        const val DISPLAY_NAME = "displayName"
    }
}
