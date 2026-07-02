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
    fun scheduleHideLoading_cancelsScheduledShow_beforeGraceDelayExpires() =
        runTest(testDispatcher) {
            // Given
            val globalUiManager = GlobalUiManager(testDispatcher)

            // When: Schedule show
            globalUiManager.scheduleShowLoading()

            // Then: Not shown yet
            assertFalse(globalUiManager.showLoadingDialog.value)

            // When: Hide before show grace delay expires
            testScheduler.advanceTimeBy(100) // Only 100ms of 150ms
            globalUiManager.scheduleHideLoading()

            // Then: Still blocked, dialog still false
            assertTrue(globalUiManager.isLoadingBlocking.value)
            assertFalse(globalUiManager.showLoadingDialog.value)

            // When: Wait for hide grace delay
            testScheduler.advanceTimeBy(150)
            testScheduler.runCurrent()

            // Then: Unblocked and dialog still false
            assertFalse(globalUiManager.isLoadingBlocking.value)
            assertFalse(globalUiManager.showLoadingDialog.value)

            // When: Wait past original show grace delay
            testScheduler.advanceTimeBy(100)

            // Then: Still false (show job was cancelled)
            assertFalse(globalUiManager.showLoadingDialog.value)
        }

    @Test
    fun scheduleHideLoading_hidesDialog_afterGraceDelayExpires() =
        runTest(testDispatcher) {
            // Given
            val globalUiManager = GlobalUiManager(testDispatcher)

            // When: Schedule and wait for show grace delay
            globalUiManager.scheduleShowLoading()
            testScheduler.advanceTimeBy(150)
            testScheduler.runCurrent()

            // Then: Dialog shown
            assertTrue(globalUiManager.showLoadingDialog.value)

            // When: Hide
            globalUiManager.scheduleHideLoading()

            // Then: Still visible and blocked during hide grace delay
            assertTrue(globalUiManager.isLoadingBlocking.value)
            assertTrue(globalUiManager.showLoadingDialog.value)

            // When: Wait for hide grace delay
            testScheduler.advanceTimeBy(150)
            testScheduler.runCurrent()

            // Then: Dialog hidden and unblocked
            assertFalse(globalUiManager.isLoadingBlocking.value)
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
    fun scheduleHideLoading_whenNotShowing_stillDelaysUnblock() =
        runTest(testDispatcher) {
            // Given
            val globalUiManager = GlobalUiManager(testDispatcher)

            // When: Hide without scheduling
            globalUiManager.scheduleHideLoading()

            // Then: Still false (no error)
            assertFalse(globalUiManager.showLoadingDialog.value)
            assertFalse(globalUiManager.isLoadingBlocking.value)

            // When: Wait for hide grace delay
            testScheduler.advanceTimeBy(150)
            testScheduler.runCurrent()

            // Then: Still false
            assertFalse(globalUiManager.showLoadingDialog.value)
            assertFalse(globalUiManager.isLoadingBlocking.value)
        }

    @Test
    fun graceDelay_preventsFlickerOnFastOperations() =
        runTest(testDispatcher) {
            // Given
            val globalUiManager = GlobalUiManager(testDispatcher)

            // When: Simulate fast operation (completes in 50ms)
            globalUiManager.scheduleShowLoading()
            testScheduler.advanceTimeBy(50)
            globalUiManager.scheduleHideLoading()

            // Then: Dialog never shown, still blocked during hide grace delay
            assertFalse(globalUiManager.showLoadingDialog.value)
            assertTrue(globalUiManager.isLoadingBlocking.value)

            // When: Wait for hide grace delay
            testScheduler.advanceTimeBy(150)
            testScheduler.runCurrent()

            // Then: Unblocked, dialog still not shown
            assertFalse(globalUiManager.isLoadingBlocking.value)
            assertFalse(globalUiManager.showLoadingDialog.value)

            // When: Wait past show grace delay
            testScheduler.advanceTimeBy(200)

            // Then: Still not shown (show job was cancelled)
            assertFalse(globalUiManager.showLoadingDialog.value)
        }

    @Test
    fun graceDelay_showsDialogOnSlowOperations() =
        runTest(testDispatcher) {
            // Given
            val globalUiManager = GlobalUiManager(testDispatcher)

            // When: Simulate slow operation (takes 300ms)
            globalUiManager.scheduleShowLoading()
            testScheduler.advanceTimeBy(150) // Show grace delay expires
            testScheduler.runCurrent()

            // Then: Dialog shown
            assertTrue(globalUiManager.showLoadingDialog.value)

            // When: Operation completes
            testScheduler.advanceTimeBy(150) // Total 300ms
            globalUiManager.scheduleHideLoading()

            // When: Wait for hide grace delay
            testScheduler.advanceTimeBy(150)
            testScheduler.runCurrent()

            // Then: Dialog hidden
            assertFalse(globalUiManager.showLoadingDialog.value)
            assertFalse(globalUiManager.isLoadingBlocking.value)
        }

    @Test
    fun initialState_isLoadingBlockingIsFalse() {
        // Given
        val globalUiManager = GlobalUiManager(testDispatcher)

        // Then
        assertFalse(globalUiManager.isLoadingBlocking.value)
    }

    @Test
    fun scheduleShowLoading_isLoadingBlockingIsTrueImmediately() =
        runTest(testDispatcher) {
            // Given
            val globalUiManager = GlobalUiManager(testDispatcher)

            // When
            globalUiManager.scheduleShowLoading()

            // Then: isLoadingBlocking is true immediately (no grace delay)
            assertTrue(globalUiManager.isLoadingBlocking.value)
            // But showLoadingDialog is still false (grace delay not expired)
            assertFalse(globalUiManager.showLoadingDialog.value)
        }

    @Test
    fun scheduleHideLoading_setsIsLoadingBlockingToFalseAfterGraceDelay() =
        runTest(testDispatcher) {
            // Given
            val globalUiManager = GlobalUiManager(testDispatcher)

            // When: Schedule and verify blocking is active
            globalUiManager.scheduleShowLoading()
            assertTrue(globalUiManager.isLoadingBlocking.value)

            // When: Hide loading
            globalUiManager.scheduleHideLoading()

            // Then: Still blocked during hide grace delay
            assertTrue(globalUiManager.isLoadingBlocking.value)
            assertFalse(globalUiManager.showLoadingDialog.value)

            // When: Wait for hide grace delay
            testScheduler.advanceTimeBy(150)
            testScheduler.runCurrent()

            // Then: Both states are false
            assertFalse(globalUiManager.isLoadingBlocking.value)
            assertFalse(globalUiManager.showLoadingDialog.value)
        }

    @Test
    fun scheduleShowLoading_afterGraceDelay_bothStatesTrue() =
        runTest(testDispatcher) {
            // Given
            val globalUiManager = GlobalUiManager(testDispatcher)

            // When
            globalUiManager.scheduleShowLoading()

            // Then: Initially blocking is true, dialog is false
            assertTrue(globalUiManager.isLoadingBlocking.value)
            assertFalse(globalUiManager.showLoadingDialog.value)

            // When: Wait for grace delay (150ms)
            testScheduler.advanceTimeBy(150)
            testScheduler.runCurrent()

            // Then: Both states are true
            assertTrue(globalUiManager.isLoadingBlocking.value)
            assertTrue(globalUiManager.showLoadingDialog.value)
        }

    @Test
    fun scheduleShowLoading_cancelsPendingHide() =
        runTest(testDispatcher) {
            // Given
            val globalUiManager = GlobalUiManager(testDispatcher)

            globalUiManager.scheduleShowLoading()
            testScheduler.advanceTimeBy(50)
            globalUiManager.scheduleHideLoading()
            testScheduler.advanceTimeBy(50)

            // When: New operation starts before hide grace delay completes
            globalUiManager.scheduleShowLoading()

            // Then: Blocking stays active
            assertTrue(globalUiManager.isLoadingBlocking.value)
            assertFalse(globalUiManager.showLoadingDialog.value)

            // When: Wait for show grace delay
            testScheduler.advanceTimeBy(150)
            testScheduler.runCurrent()

            // Then: Dialog shown again
            assertTrue(globalUiManager.showLoadingDialog.value)
        }

    @Test
    fun dispose_clearsLoadingStateImmediately() =
        runTest(testDispatcher) {
            // Given
            val globalUiManager = GlobalUiManager(testDispatcher)

            globalUiManager.scheduleShowLoading()
            globalUiManager.scheduleHideLoading()

            // When: Dispose during hide grace delay
            globalUiManager.dispose()

            // Then: Cleared immediately
            assertFalse(globalUiManager.isLoadingBlocking.value)
            assertFalse(globalUiManager.showLoadingDialog.value)

            // When: Wait past hide grace delay
            testScheduler.advanceTimeBy(200)
            testScheduler.runCurrent()

            // Then: Still cleared
            assertFalse(globalUiManager.isLoadingBlocking.value)
            assertFalse(globalUiManager.showLoadingDialog.value)
        }

    @Test
    fun reset_clearsLoadingStateImmediately() =
        runTest(testDispatcher) {
            // Given
            val globalUiManager = GlobalUiManager(testDispatcher)

            globalUiManager.scheduleShowLoading()
            globalUiManager.scheduleHideLoading()

            // When: Reset during hide grace delay
            globalUiManager.reset()

            // Then: Cleared immediately
            assertFalse(globalUiManager.isLoadingBlocking.value)
            assertFalse(globalUiManager.showLoadingDialog.value)

            // When: Wait past hide grace delay
            testScheduler.advanceTimeBy(200)
            testScheduler.runCurrent()

            // Then: Still cleared (pending jobs were cancelled)
            assertFalse(globalUiManager.isLoadingBlocking.value)
            assertFalse(globalUiManager.showLoadingDialog.value)
        }

    @Test
    fun reset_keepsScopeUsable_forSubsequentLoading() =
        runTest(testDispatcher) {
            // Given: a manager that was reset on a previous presenter teardown
            val globalUiManager = GlobalUiManager(testDispatcher)
            globalUiManager.scheduleShowLoading()
            globalUiManager.reset()

            // When: a new session schedules loading (regression guard for issue #1562)
            globalUiManager.scheduleShowLoading()

            // Then: blocking flips true immediately...
            assertTrue(globalUiManager.isLoadingBlocking.value)

            // ...and the dialog still appears after the grace delay (scope not cancelled)
            assertFalse(globalUiManager.showLoadingDialog.value)
            testScheduler.advanceTimeBy(150)
            testScheduler.runCurrent()
            assertTrue(globalUiManager.showLoadingDialog.value)
        }
}
