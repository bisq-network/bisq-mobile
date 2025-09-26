package network.bisq.mobile.presentation.ui.uicases.take_offer

import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager
import network.bisq.mobile.domain.utils.CoroutineExceptionHandlerSetup
import network.bisq.mobile.i18n.I18nSupport

/**
 * With BasePresenter#blockInteractivityOnAttached, the Review screen prevents rapid taps
 * right after navigation by keeping isInteractive false briefly, then re-enabling it.
 * This test verifies that behavior at the presenter layer.
 */
class TakeOfferReviewInteractivityTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        startKoin {
            modules(
                module {
                    single { CoroutineExceptionHandlerSetup() }
                    factory<CoroutineJobsManager> {
                        DefaultCoroutineJobsManager().apply {
                            get<CoroutineExceptionHandlerSetup>().setupExceptionHandler(this)
                        }
                    }
                }
            )
        }
        I18nSupport.initialize("en")
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    private class TestPresenter(mainPresenter: MainPresenter) : BasePresenter(mainPresenter) {
        override val blockInteractivityOnAttached: Boolean = true
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun interactivity_isBlockedBriefly_onViewAttached_thenReEnabled() = runTest(testDispatcher) {
        // Arrange
        val mainPresenter = mockk<MainPresenter>(relaxed = true)
        val presenter = TestPresenter(mainPresenter)

        // Sanity: starts interactive by default
        assertTrue(presenter.isInteractive.value)

        // Act: attach view triggers BasePresenter blockInteractivityOnAttached path
        presenter.onViewAttached()

        // Immediately after attach, interactivity is disabled
        assertFalse(presenter.isInteractive.value)

        // After the smallest perceptive delay, interactivity is re-enabled
        advanceTimeBy(BasePresenter.SMALLEST_PERCEPTIVE_DELAY)
        runCurrent()
        assertTrue(presenter.isInteractive.value)
    }
}

