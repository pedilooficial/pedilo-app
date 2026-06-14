package com.pedilo.app.ui.publicuser

import android.content.Context
import com.pedilo.app.core.model.TeamRole
import com.pedilo.app.core.model.TeamSession

private const val PREFS_NAME = "pedilo_team_session"
private const val KEY_UID = "team_uid"
private const val KEY_EMAIL = "team_email"
private const val KEY_DISPLAY_NAME = "team_display_name"
private const val KEY_ROLE = "team_role"

class TeamSessionStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun readPersistedSession(): TeamSession? {
        val uid = prefs.getString(KEY_UID, null)?.takeIf { it.isNotBlank() } ?: return null
        val roleName = prefs.getString(KEY_ROLE, null) ?: return null
        val role = TeamRole.fromWire(roleName) ?: return null
        return TeamSession(
            uid = uid,
            email = prefs.getString(KEY_EMAIL, null).orEmpty(),
            displayName = prefs.getString(KEY_DISPLAY_NAME, null).orEmpty(),
            role = role,
            keepSignedIn = true,
        )
    }

    fun save(session: TeamSession) {
        if (session.keepSignedIn) {
            prefs.edit()
                .putString(KEY_UID, session.uid)
                .putString(KEY_EMAIL, session.email)
                .putString(KEY_DISPLAY_NAME, session.displayName)
                .putString(KEY_ROLE, session.role.wireName)
                .apply()
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
