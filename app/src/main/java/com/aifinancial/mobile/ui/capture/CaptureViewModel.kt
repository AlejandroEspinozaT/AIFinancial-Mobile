package com.aifinancial.mobile.ui.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aifinancial.mobile.core.queue.PendingDocument
import com.aifinancial.mobile.data.repository.DocumentCaptureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val documentCaptureRepository: DocumentCaptureRepository,
) : ViewModel() {

    /**
     * Backed directly by Room via the repository — this is the architectural
     * proof point of specs/architecture/overview.md: the UI never needs the
     * network to show correct, current queue state.
     */
    val queue: StateFlow<List<PendingDocument>> = documentCaptureRepository.observeQueue()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Called once the camera intent reports a file was actually written. The
     * repository's `enqueue` suspends until the Room insert completes —
     * nothing here reports success to the UI before that row is durable.
     */
    fun onPhotoCaptured(localFilePath: String) {
        viewModelScope.launch {
            documentCaptureRepository.enqueue(localFilePath)
        }
    }

    /**
     * Only meaningful once Stage 2 (background sync) exists and can
     * actually produce a FAILED document — included now because the
     * decision logic itself ([com.aifinancial.mobile.core.queue.QueueStateMachine])
     * is already built and tested, and wiring it here is a cheap, correct
     * placeholder rather than something to redo later.
     */
    fun onRetry(localId: String) {
        viewModelScope.launch {
            documentCaptureRepository.retry(localId)
        }
    }
}
