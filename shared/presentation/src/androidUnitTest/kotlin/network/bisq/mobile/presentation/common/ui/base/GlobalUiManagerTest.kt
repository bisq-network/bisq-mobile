package network.bisq.mobile.presentation.common.ui.base

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GlobalUiManagerTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_showLoadingDialogIsFalse() {
        // Given
        val globalUiManager = GlobalUiManager(testDispatcher)

        // Then
        assertFalse(globalUiManager.showLoadingDialog.value)
    }

    @Test
    fun scheduleShowLoading_doesNotShowImmediately() =
        runTest(testDispatcher) {
            // Given
            val globalUiManager = GlobalUiManager(testDispatcher)

            // When
            globalUiManager.scheduleShowLoading()

            // Then: Dialog not shown immediately (grace delay not expired)
            assertFalse(globalUiManager.showLoadingDialog.value)
        }

    @Test
    fun scheduleShowLoading_showsDialogAfterGraceDelay() =
        runTest(testDispatcher) {
            // Given
            val globalUiManager = GlobalUiManager(testDispatcher)

            // When
            globalUiManager.scheduleShowLoading()

            // Then: Initially false
            assertFalse(globalUiManager.showLoadingDialog.value)

            // When: Wait for grace delay (150ms)
            testScheduler.advanceTimeBy(150)
            testScheduler.runCurrent()

            // Then: Now true
            assertTrue(globalUiManager.showLoadingDialog.value)
        }

    @Test
    fun hideLoading_cancelsScheduledShow_beforeGraceDelayExpires() =
        runTest(testDispatcher) {
            // Given
            val globalUiManager = GlobalUiManager(testDispatcher)

            // When: Schedule show
            globalUiManager.scheduleShowLoading()

            // Then: Not shown yet
            assertFalse(globalUiManager.showLoadingDialog.value)

            // When: Hide before grace delay expires
            testScheduler.advanceTimeBy(100) // Only 100ms of 150ms
            globalUiManager.hideLoading()

            // Then: Still false
            assertFalse(globalUiManager.showLoadingDialog.value)

            // When: Wait past original grace delay
            testScheduler.advanceTimeBy(100) // Total 200ms

            // Then: Still false (job was cancelled)
            assertFalse(globalUiManager.showLoadingDialog.value)
        }

    @Test
    fun hideLoading_hidesDialog_afterGraceDelayExpires() =
        runTest(testDispatcher) {
            // Given
            val globalUiManager = GlobalUiManager(testDispatcher)

            // When: Schedule and wait for grace delay
            globalUiManager.scheduleShowLoading()
            testScheduler.advanceTimeBy(150)
            testScheduler.runCurrent()

            // Then: Dialog shown
            assertTrue(globalUiManager.showLoadingDialog.value)

            // When: Hide
            globalUiManager.hideLoading()

            // Then: Dialog hidden immediately
            assertFalse(globalUiManager.showLoadingDialog.value)
        }

    @Test
    fun multipleScheduleShowLoading_cancelsPreviousJob() =
        runTest(testDispatcher) {
            // Given
            val globalUiManager = GlobalUiManager(testDispatcher)

            // When: First schedule
            globalUiManager.scheduleShowLoading()
            testScheduler.advanceTimeBy(100) // Wait 100ms (not enough for grace delay)

            // Then: Not shown yet
            assertFalse(globalUiManager.showLoadingDialog.value)

            // When: Second schedule (should cancel first)
            globalUiManager.scheduleShowLoading()
            testScheduler.advanceTimeBy(100) // Another 100ms (total 200ms from first schedule)

            // Then: Still not shown (first job was cancelled, second needs 150ms)
            assertFalse(globalUiManager.showLoadingDialog.value)

            // When: Wait for second grace delay to complete
            testScheduler.advanceTimeBy(50) // Total 150ms from second schedule
            testScheduler.runCurrent()

            // Then: Now shown
            assertTrue(globalUiManager.showLoadingDialog.value)
        }

    @Test
    fun hideLoading_whenNotShowing_doesNothing() =
        runTest(testDispatcher) {
            // Given
            val globalUiManager = GlobalUiManager(testDispatcher)

            // When: Hide without scheduling
            globalUiManager.hideLoading()

            // Then: Still false (no error)
            assertFalse(globalUiManager.showLoadingDialog.value)
        }

    @Test
    fun graceDelay_preventsFlickerOnFastOperations() =
        runTest(testDispatcher) {
            // Given
            val globalUiManager = GlobalUiManager(testDispatcher)

            // When: Simulate fast operation (completes in 50ms)
            globalUiManager.scheduleShowLoading()
            testScheduler.advanceTimeBy(50)
            globalUiManager.hideLoading()

            // Then: Dialog never shown (operation completed before grace delay)
            assertFalse(globalUiManager.showLoadingDialog.value)

            // When: Wait past grace delay
            testScheduler.advanceTimeBy(200)

            // Then: Still not shown (job was cancelled)
            assertFalse(globalUiManager.showLoadingDialog.value)
        }

    @Test
    fun graceDelay_showsDialogOnSlowOperations() =
        runTest(testDispatcher) {
            // Given
            val globalUiManager = GlobalUiManager(testDispatcher)

            // When: Simulate slow operation (takes 300ms)
            globalUiManager.scheduleShowLoading()
            testScheduler.advanceTimeBy(150) // Grace delay expires
            testScheduler.runCurrent()

            // Then: Dialog shown
            assertTrue(globalUiManager.showLoadingDialog.value)

            // When: Operation completes
            testScheduler.advanceTimeBy(150) // Total 300ms
            globalUiManager.hideLoading()

            // Then: Dialog hidden
            assertFalse(globalUiManager.showLoadingDialog.value)
        }
}
