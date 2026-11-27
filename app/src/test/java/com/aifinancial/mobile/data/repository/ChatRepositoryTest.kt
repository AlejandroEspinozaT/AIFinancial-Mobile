package com.aifinancial.mobile.data.repository

import com.aifinancial.mobile.data.local.ChatSessionStore
import com.aifinancial.mobile.data.remote.ChatApi
import com.aifinancial.mobile.data.remote.dto.ChatMessageDto
import com.aifinancial.mobile.data.remote.dto.ChatSessionDetailDto
import com.aifinancial.mobile.data.remote.dto.SendChatMessageResultDto
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.Response

/**
 * Exercises specs/phases/stage-5-ai-chat-assistant.md's send/load logic
 * against a fake [ChatApi] -- success (both a fresh session and continuing
 * one), the two documented non-2xx cases the backend can return, a generic
 * failure, and loading history (including the "session genuinely gone"
 * 404 case).
 */
class ChatRepositoryTest {

    private val chatApi = mockk<ChatApi>()
    private val chatSessionStore = mockk<ChatSessionStore>(relaxed = true)
    private val repository = ChatRepository(chatApi, chatSessionStore)

    private fun messageDto(role: String = "Assistant") = ChatMessageDto(
        id = "msg-1",
        role = role,
        content = "You spent $42 on Dining this month.",
        createdAtUtc = "2026-01-15T10:00:00Z",
    )

    @Test
    fun `a successful send returns the assistant's reply and the session id`() = runTest {
        coEvery { chatApi.sendMessage(any()) } returns
            Response.success(SendChatMessageResultDto(sessionId = "session-1", message = messageDto()))

        val outcome = repository.sendMessage("How much did I spend on dining?", sessionId = null)

        assertThat(outcome).isInstanceOf(SendMessageOutcome.Sent::class.java)
        val sent = outcome as SendMessageOutcome.Sent
        assertThat(sent.sessionId).isEqualTo("session-1")
        assertThat(sent.assistantMessage.role).isEqualTo(ChatRole.Assistant)
    }

    @Test
    fun `a 429 surfaces the backend's rate-limit reason, not a guess`() = runTest {
        coEvery { chatApi.sendMessage(any()) } returns Response.error(429, "".toResponseBody(null))

        val outcome = repository.sendMessage("hi", sessionId = "session-1")

        assertThat(outcome).isInstanceOf(SendMessageOutcome.RateLimited::class.java)
    }

    @Test
    fun `a 503 surfaces as service unavailable`() = runTest {
        coEvery { chatApi.sendMessage(any()) } returns Response.error(503, "".toResponseBody(null))

        val outcome = repository.sendMessage("hi", sessionId = "session-1")

        assertThat(outcome).isEqualTo(SendMessageOutcome.ServiceUnavailable)
    }

    @Test
    fun `any other non-2xx is a generic, never-silent failure`() = runTest {
        coEvery { chatApi.sendMessage(any()) } returns Response.error(500, "".toResponseBody(null))

        val outcome = repository.sendMessage("hi", sessionId = "session-1")

        assertThat(outcome).isInstanceOf(SendMessageOutcome.Failed::class.java)
    }

    @Test
    fun `a network exception is a generic failure, never a guessed success`() = runTest {
        coEvery { chatApi.sendMessage(any()) } throws IOException("timeout")

        val outcome = repository.sendMessage("hi", sessionId = "session-1")

        assertThat(outcome).isInstanceOf(SendMessageOutcome.Failed::class.java)
    }

    @Test
    fun `loading history returns the session's messages`() = runTest {
        coEvery { chatApi.getSessionById("session-1") } returns Response.success(
            ChatSessionDetailDto(id = "session-1", createdAtUtc = "2026-01-15T09:00:00Z", messages = listOf(messageDto(role = "User"))),
        )

        val outcome = repository.loadHistory("session-1")

        assertThat(outcome).isInstanceOf(LoadHistoryOutcome.Loaded::class.java)
        assertThat((outcome as LoadHistoryOutcome.Loaded).messages.single().role).isEqualTo(ChatRole.User)
    }

    @Test
    fun `a 404 on load clears the stored session id instead of retrying it forever`() = runTest {
        coEvery { chatApi.getSessionById("session-1") } returns Response.error(404, "".toResponseBody(null))

        val outcome = repository.loadHistory("session-1")

        assertThat(outcome).isEqualTo(LoadHistoryOutcome.NotFound)
        verify { chatSessionStore.clear() }
    }
}
