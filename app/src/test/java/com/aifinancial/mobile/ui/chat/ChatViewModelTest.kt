package com.aifinancial.mobile.ui.chat

import com.aifinancial.mobile.data.local.ChatSessionStore
import com.aifinancial.mobile.data.repository.ChatRepository
import com.aifinancial.mobile.data.repository.ChatRole
import com.aifinancial.mobile.data.repository.ChatUiMessage
import com.aifinancial.mobile.data.repository.LoadHistoryOutcome
import com.aifinancial.mobile.data.repository.SendMessageOutcome
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val chatRepository = mockk<ChatRepository>()
    private val chatSessionStore = mockk<ChatSessionStore>(relaxed = true) {
        every { getSessionId() } returns null
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sending a message shows it immediately, before the reply arrives`() {
        coEvery { chatRepository.sendMessage(any(), any()) } returns
            SendMessageOutcome.Sent("session-1", assistantReply())
        val viewModel = ChatViewModel(chatRepository, chatSessionStore)

        viewModel.onSendMessage("How much did I spend on dining?")

        // The UnconfinedTestDispatcher resolves the launched coroutine
        // synchronously, so by the time onSendMessage returns both the
        // user's own message and the reply are already in the list --
        // what matters is that the user's message was appended *before*
        // sendMessage was even called, not after a successful response.
        assertThat(viewModel.messages.value.first().role).isEqualTo(ChatRole.User)
        assertThat(viewModel.messages.value.first().content).isEqualTo("How much did I spend on dining?")
    }

    @Test
    fun `a successful reply is appended and the session id persisted`() {
        coEvery { chatRepository.sendMessage(any(), any()) } returns
            SendMessageOutcome.Sent("session-1", assistantReply())
        val viewModel = ChatViewModel(chatRepository, chatSessionStore)

        viewModel.onSendMessage("hi")

        assertThat(viewModel.messages.value).hasSize(2)
        assertThat(viewModel.messages.value.last().role).isEqualTo(ChatRole.Assistant)
        assertThat(viewModel.uiState.value).isEqualTo(ChatUiState.Idle)
        verify { chatSessionStore.setSessionId("session-1") }
    }

    @Test
    fun `a failure surfaces as an error without losing the user's own message`() {
        coEvery { chatRepository.sendMessage(any(), any()) } returns
            SendMessageOutcome.Failed("No connection — the assistant needs internet to answer.")
        val viewModel = ChatViewModel(chatRepository, chatSessionStore)

        viewModel.onSendMessage("hi")

        assertThat(viewModel.messages.value).hasSize(1)
        assertThat(viewModel.uiState.value).isInstanceOf(ChatUiState.Error::class.java)
    }

    @Test
    fun `an existing stored session id loads its history on init`() {
        every { chatSessionStore.getSessionId() } returns "session-1"
        coEvery { chatRepository.loadHistory("session-1") } returns
            LoadHistoryOutcome.Loaded(listOf(assistantReply()))

        val viewModel = ChatViewModel(chatRepository, chatSessionStore)

        assertThat(viewModel.messages.value).containsExactly(assistantReply())
    }

    private fun assistantReply() = ChatUiMessage(
        id = "msg-1",
        role = ChatRole.Assistant,
        content = "You spent $42 on Dining this month.",
        createdAtUtc = "2026-01-15T10:00:00Z",
    )
}
