package com.aifinancial.mobile.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aifinancial.mobile.data.local.ChatSessionStore
import com.aifinancial.mobile.data.repository.ChatRepository
import com.aifinancial.mobile.data.repository.ChatRole
import com.aifinancial.mobile.data.repository.ChatUiMessage
import com.aifinancial.mobile.data.repository.LoadHistoryOutcome
import com.aifinancial.mobile.data.repository.SendMessageOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ChatUiState {
    data object Idle : ChatUiState
    data object Sending : ChatUiState
    data class Error(val message: String) : ChatUiState
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val chatSessionStore: ChatSessionStore,
) : ViewModel() {

    private var sessionId: String? = chatSessionStore.getSessionId()

    private val _messages = MutableStateFlow<List<ChatUiMessage>>(emptyList())
    val messages: StateFlow<List<ChatUiMessage>> = _messages.asStateFlow()

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        sessionId?.let(::loadHistory)
    }

    private fun loadHistory(id: String) {
        viewModelScope.launch {
            when (val outcome = chatRepository.loadHistory(id)) {
                is LoadHistoryOutcome.Loaded -> _messages.value = outcome.messages
                LoadHistoryOutcome.NotFound -> sessionId = null
                // A transient load failure (offline, backend hiccup) leaves
                // the screen empty rather than stuck -- the user can still
                // start typing; the next send either resumes this session
                // or starts a new one, never a dead end.
                LoadHistoryOutcome.Failed -> Unit
            }
        }
    }

    fun onSendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _uiState.value is ChatUiState.Sending) return

        val userMessage = ChatUiMessage(
            id = UUID.randomUUID().toString(),
            role = ChatRole.User,
            content = trimmed,
            createdAtUtc = Instant.now().toString(),
        )
        // Appended before the network call, same principle as every other
        // write in this app: the user's own action is reflected immediately,
        // never waiting on a round-trip to be visible.
        _messages.value = _messages.value + userMessage
        _uiState.value = ChatUiState.Sending

        viewModelScope.launch {
            when (val outcome = chatRepository.sendMessage(trimmed, sessionId)) {
                is SendMessageOutcome.Sent -> {
                    sessionId = outcome.sessionId
                    chatSessionStore.setSessionId(outcome.sessionId)
                    _messages.value = _messages.value + outcome.assistantMessage
                    _uiState.value = ChatUiState.Idle
                }

                is SendMessageOutcome.RateLimited -> _uiState.value = ChatUiState.Error(outcome.message)
                SendMessageOutcome.ServiceUnavailable ->
                    _uiState.value = ChatUiState.Error("The assistant is temporarily unavailable — try again shortly.")
                is SendMessageOutcome.Failed -> _uiState.value = ChatUiState.Error(outcome.reason)
            }
        }
    }
}
