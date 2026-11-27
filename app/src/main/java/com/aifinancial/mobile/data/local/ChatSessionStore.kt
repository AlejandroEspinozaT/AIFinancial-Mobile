package com.aifinancial.mobile.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Just the current chat session id -- not a credential, so plain
 * `SharedPreferences`, unlike `pairing/AuthTokenStore`'s encrypted one.
 * See specs/phases/stage-5-ai-chat-assistant.md: message *content* is
 * never cached locally, only this id, so reopening the app resumes the
 * same conversation via `GET /api/chat/sessions/{id}` instead of starting
 * over.
 */
@Singleton
class ChatSessionStore @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)

    fun getSessionId(): String? = prefs.getString(KEY_SESSION_ID, null)

    fun setSessionId(sessionId: String) {
        prefs.edit().putString(KEY_SESSION_ID, sessionId).apply()
    }

    /** Called on unlink, or when a stored session id turns out to be gone server-side. */
    fun clear() {
        prefs.edit().remove(KEY_SESSION_ID).apply()
    }

    private companion object {
        const val PREFS_FILE_NAME = "aifinancial_chat"
        const val KEY_SESSION_ID = "session_id"
    }
}
