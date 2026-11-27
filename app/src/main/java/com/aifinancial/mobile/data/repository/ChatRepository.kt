package com.aifinancial.mobile.data.repository

import com.aifinancial.mobile.data.local.ChatSessionStore
import com.aifinancial.mobile.data.remote.ChatApi
import com.aifinancial.mobile.data.remote.dto.ChatMessageDto
import com.aifinancial.mobile.data.remote.dto.SendChatMessageRequest
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

enum class ChatRole { User, Assistant }

/**
 * `createdAtUtc` stays a raw string, deliberately, same reasoning as
 * `data/local/CachedTransactionEntity.kt`'s `date` field: this is only ever
 * displayed, never computed with, and parsing a backend date string into a
 * typed [java.time.Instant] risks a real runtime crash if that string ever
 * lacks an explicit UTC offset/`Z` (unlike one this app generates itself).
 */
data class ChatUiMessage(
    val id: String,
    val role: ChatRole,
    val content: String,
    val createdAtUtc: String,
)

sealed interface SendMessageOutcome {
    data class Sent(val sessionId: String, val assistantMessage: ChatUiMessage) : SendMessageOutcome
    data class RateLimited(val message: String) : SendMessageOutcome
    data object ServiceUnavailable : SendMessageOutcome
    data class Failed(val reason: String) : SendMessageOutcome
}

sealed interface LoadHistoryOutcome {
    data class Loaded(val messages: List<ChatUiMessage>) : LoadHistoryOutcome
    data object NotFound : LoadHistoryOutcome
    data object Failed : LoadHistoryOutcome
}

/**
 * Stage 5's one deliberate exception to specs/architecture/overview.md's
 * offline-first rule — see specs/phases/stage-5-ai-chat-assistant.md. Every
 * other repository in this app either reads from Room or writes to it
 * before touching the network; this one does neither; a chat message is a
 * live question, never queued.
 */
@Singleton
class ChatRepository @Inject constructor(
    private val chatApi: ChatApi,
    private val chatSessionStore: ChatSessionStore,
) {

    suspend fun sendMessage(message: String, sessionId: String?): SendMessageOutcome = try {
        val response = chatApi.sendMessage(SendChatMessageRequest(message = message, sessionId = sessionId))
        when {
            response.isSuccessful -> {
                val body = response.body() ?: return SendMessageOutcome.Failed("Empty response from the assistant.")
                SendMessageOutcome.Sent(sessionId = body.sessionId, assistantMessage = body.message.toUiMessage())
            }

            response.code() == 429 ->
                SendMessageOutcome.RateLimited("Too many messages. Please wait a moment before trying again.")

            response.code() == 503 -> SendMessageOutcome.ServiceUnavailable

            else -> SendMessageOutcome.Failed("The assistant couldn't answer (HTTP ${response.code()}).")
        }
    } catch (_: IOException) {
        SendMessageOutcome.Failed("No connection — the assistant needs internet to answer.")
    }

    /** Called when the Chat screen opens, if [ChatSessionStore] already has a session id. */
    suspend fun loadHistory(sessionId: String): LoadHistoryOutcome = try {
        val response = chatApi.getSessionById(sessionId)
        when {
            response.isSuccessful -> {
                val body = response.body() ?: return LoadHistoryOutcome.Failed
                LoadHistoryOutcome.Loaded(body.messages.map { it.toUiMessage() })
            }

            response.code() == 404 -> {
                // The session is genuinely gone (e.g. backend data reset) --
                // clear it so the next send starts a fresh one instead of
                // retrying a sessionId the backend will keep rejecting.
                chatSessionStore.clear()
                LoadHistoryOutcome.NotFound
            }

            else -> LoadHistoryOutcome.Failed
        }
    } catch (_: IOException) {
        LoadHistoryOutcome.Failed
    }

    private fun ChatMessageDto.toUiMessage() = ChatUiMessage(
        id = id,
        role = if (role == "User") ChatRole.User else ChatRole.Assistant,
        content = content,
        createdAtUtc = createdAtUtc,
    )
}
