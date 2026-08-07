package network.bisq.mobile.presentation.main

import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.domain.analytics.AnalyticsEvent
import network.bisq.mobile.domain.analytics.AnalyticsService
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.presentation.common.test_utils.TestApplicationLifecycleService
import network.bisq.mobile.test.presentation.coroutines.PlatformPresentationKoinTestBase
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.test.Test

/**
 * Coverage for the UI-language analytics observer that lives on
 * [MainPresenter]. The observer is what gives us the user-language baseline
 * (rodvar 2026-06-12: "most important so we get to know our userbase
 * preferences on languages") — pin its emission semantics so a future refactor
 * can't silently drop the auto-detect or change semantics without a build break.
 *
 * Tests deliberately exercise the observer through `onViewAttached()` (the
 * production trigger) rather than calling the private `startLanguageAnalytics...`
 * method directly — that catches both the "observer started" path AND the
 * "observer started exactly once" guard.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainPresenterLanguageAnalyticsTest : PlatformPresentationKoinTestBase() {
    private val analyticsService: AnalyticsService = mockk(relaxed = true)

    override fun additionalModules(): List<Module> =
        listOf(
            module {
                single<AnalyticsService> { analyticsService }
            },
        )

    @Test
    fun `auto-detected baseline emits LanguageChanged on first non-blank value`() =
        runTest {
            val languageCode = MutableStateFlow("")
            val presenter =
                MainPresenterTestFactory.create(
                    languageCode = languageCode,
                    applicationLifecycleService = TestApplicationLifecycleService(),
                )
            presenter.onViewAttached()
            advanceUntilIdle()

            // No emit while blank — observer filters out the empty initial value.
            verify(exactly = 0) { analyticsService.track(any<AnalyticsEvent.Settings.LanguageChanged>()) }

            // First non-blank value = auto-detected baseline.
            languageCode.value = "en"
            advanceUntilIdle()

            verify(exactly = 1) { analyticsService.track(AnalyticsEvent.Settings.LanguageChanged("en")) }
        }

    @Test
    fun `user change to a tracked code emits LanguageChanged for that code`() =
        runTest {
            val languageCode = MutableStateFlow("en")
            val presenter =
                MainPresenterTestFactory.create(
                    languageCode = languageCode,
                    applicationLifecycleService = TestApplicationLifecycleService(),
                )
            presenter.onViewAttached()
            advanceUntilIdle()

            // Baseline ("en") emitted once.
            verify(exactly = 1) { analyticsService.track(AnalyticsEvent.Settings.LanguageChanged("en")) }

            // User switches to Spanish via Settings → backend → settingsService.languageCode flow.
            languageCode.value = "es"
            advanceUntilIdle()

            verify(exactly = 1) { analyticsService.track(AnalyticsEvent.Settings.LanguageChanged("es")) }
        }

    @Test
    fun `repeated same code does not emit`() =
        runTest {
            // distinctUntilChanged keeps us from inflating session count if the
            // backend re-emits the same value (e.g. after getSettings() refresh).
            val languageCode = MutableStateFlow("en")
            val presenter =
                MainPresenterTestFactory.create(
                    languageCode = languageCode,
                    applicationLifecycleService = TestApplicationLifecycleService(),
                )
            presenter.onViewAttached()
            advanceUntilIdle()

            languageCode.value = "en" // same value
            languageCode.value = "en"
            advanceUntilIdle()

            verify(exactly = 1) { analyticsService.track(AnalyticsEvent.Settings.LanguageChanged("en")) }
        }

    @Test
    fun `untracked language code is silently dropped`() =
        runTest {
            // Defence against the backend handing us a code that isn't in our
            // wire-format allowlist (e.g. a future translation we haven't
            // whitelisted yet, or a typo). No emission, no crash.
            val languageCode = MutableStateFlow("")
            val presenter =
                MainPresenterTestFactory.create(
                    languageCode = languageCode,
                    applicationLifecycleService = TestApplicationLifecycleService(),
                )
            presenter.onViewAttached()
            advanceUntilIdle()

            languageCode.value = "xx-ZZ" // not in TRACKED_LANGUAGE_CODES
            advanceUntilIdle()

            verify(exactly = 0) { analyticsService.track(any<AnalyticsEvent.Settings.LanguageChanged>()) }
        }

    @Test
    fun `pcm-NG and pt-BR are tracked with sanitised wire names`() =
        runTest {
            // Pins the kdoc claim: the data-class .code stays raw, but the wire
            // name is sanitised via Settings.sanitizeCode. Two regressions are
            // covered: (1) the observer continues to recognise hyphenated
            // codes, and (2) the resulting event's wire name is alnum+underscore.
            val languageCode = MutableStateFlow("")
            val presenter =
                MainPresenterTestFactory.create(
                    languageCode = languageCode,
                    applicationLifecycleService = TestApplicationLifecycleService(),
                )
            presenter.onViewAttached()
            advanceUntilIdle()

            languageCode.value = "pcm-NG"
            advanceUntilIdle()
            languageCode.value = "pt-BR"
            advanceUntilIdle()

            verify(exactly = 1) {
                analyticsService.track(
                    match<AnalyticsEvent.Settings.LanguageChanged> {
                        it.code == "pcm-NG" && it.name == "settings.language_changed_pcm_ng"
                    },
                )
            }
            verify(exactly = 1) {
                analyticsService.track(
                    match<AnalyticsEvent.Settings.LanguageChanged> {
                        it.code == "pt-BR" && it.name == "settings.language_changed_pt_br"
                    },
                )
            }
        }

    @Test
    fun `observer survives view re-attach without double-emitting`() =
        runTest {
            // MainPresenter is a Koin `single` — backgrounding the app and
            // foregrounding it may fire onViewAttached() a second time. The
            // once-only guard must prevent a second collector from starting,
            // otherwise the next user change would double-emit.
            val languageCode = MutableStateFlow("en")
            val presenter =
                MainPresenterTestFactory.create(
                    languageCode = languageCode,
                    applicationLifecycleService = TestApplicationLifecycleService(),
                )
            presenter.onViewAttached()
            advanceUntilIdle()
            presenter.onViewAttached() // simulate re-attach
            advanceUntilIdle()

            languageCode.value = "es"
            advanceUntilIdle()

            // Baseline + 1 user change, NOT baseline + 2.
            verify(exactly = 1) { analyticsService.track(AnalyticsEvent.Settings.LanguageChanged("en")) }
            verify(exactly = 1) { analyticsService.track(AnalyticsEvent.Settings.LanguageChanged("es")) }
        }

    @Test
    fun `observer restarts after onViewUnattaching disposes the scope`() =
        runTest {
            // Pins the contract that the once-only guard is RESET on
            // disposal so the next attach can start a fresh collector. Without
            // the reset, an Android config-change cycle (activity recreation
            // → onViewUnattaching → jobsManager.dispose() → new scope →
            // onViewAttached) would leave the observer dead because the stale
            // flag would short-circuit the next start.
            val languageCode = MutableStateFlow("en")
            val presenter =
                MainPresenterTestFactory.create(
                    languageCode = languageCode,
                    applicationLifecycleService = TestApplicationLifecycleService(),
                )

            presenter.onViewAttached()
            advanceUntilIdle()
            verify(exactly = 1) { analyticsService.track(AnalyticsEvent.Settings.LanguageChanged("en")) }

            presenter.onViewUnattaching()
            advanceUntilIdle()

            presenter.onViewAttached()
            advanceUntilIdle()
            languageCode.value = "es"
            advanceUntilIdle()

            verify(exactly = 1) { analyticsService.track(AnalyticsEvent.Settings.LanguageChanged("es")) }
        }
}
