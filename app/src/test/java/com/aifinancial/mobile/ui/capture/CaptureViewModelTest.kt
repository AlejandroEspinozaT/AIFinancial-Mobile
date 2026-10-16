package com.aifinancial.mobile.ui.capture

import androidx.lifecycle.SavedStateHandle
import com.aifinancial.mobile.data.repository.DocumentCaptureRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CaptureViewModelTest {

    private val repository = mockk<DocumentCaptureRepository>(relaxed = true) {
        every { observeQueue() } returns flowOf(emptyList())
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
    fun `successful capture enqueues the file recorded before the camera opened`() {
        val viewModel = CaptureViewModel(repository, SavedStateHandle())

        viewModel.onCaptureStarted("/captures/a.jpg")
        viewModel.onCaptureResult(success = true)

        coVerify(exactly = 1) { repository.enqueue("/captures/a.jpg") }
    }

    @Test
    fun `capture result after process death still enqueues the pending file`() {
        // Simulates process death while the camera app is in front: the first
        // ViewModel instance is gone, and a new one is restored from the same
        // saved state the system persisted for the screen.
        val savedState = SavedStateHandle()
        CaptureViewModel(repository, savedState).onCaptureStarted("/captures/b.jpg")
        val restored = CaptureViewModel(repository, SavedStateHandle(savedState.keys().associateWith { savedState.get<Any>(it) }))

        restored.onCaptureResult(success = true)

        coVerify(exactly = 1) { repository.enqueue("/captures/b.jpg") }
    }

    @Test
    fun `cancelled capture enqueues nothing and clears the pending file`() {
        val viewModel = CaptureViewModel(repository, SavedStateHandle())

        viewModel.onCaptureStarted("/captures/c.jpg")
        viewModel.onCaptureResult(success = false)
        viewModel.onCaptureResult(success = true)

        coVerify(exactly = 0) { repository.enqueue(any()) }
    }
}
