package network.bisq.mobile.presentation.common.service

import network.bisq.mobile.domain.analytics.AnalyticsBootstrapConfig
import network.bisq.mobile.domain.analytics.AnalyticsEvent
import network.bisq.mobile.domain.analytics.AnalyticsService
import network.bisq.mobile.presentation.common.test_utils.TestApplicationLifecycleService
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Pins the analytics-bootstrap behaviour of `ApplicationLifecycleService`:
 *
 *  1. `initialize()` forwards the [AnalyticsBootstrapConfig] values verbatim to
 *     [AnalyticsService.init].
 *  2. Exceptions thrown by `AnalyticsService.init` are swallowed so analytics
 *     failures never take the app down.
 *
 * The second point is the genuinely load-bearing one — analytics is opt-in,
 * non-critical, and absolutely must not interfere with normal app startup.
 * A regression here (e.g. someone removes the try/catch during a refactor)
 * would be silent in dev (NoOp doesn't throw) but catastrophic in production
 * (Sentry-KMP init failure crashes the app on launch).
 */
class ApplicationLifecycleServiceBootstrapTest {
    /** Records `init` calls + can be told to throw on the next call. */
    private class RecordingAnalyticsService(
        private val throwOnInit: Throwable? = null,
    ) : AnalyticsService {
        var initCalls = 0
            private set
        var lastDsn: String? = null
            private set
        var lastEnvironment: String? = null
            private set
        var lastRelease: String? = null
            private set
        var lastIsDebug: Boolean? = null
            private set

        override fun init(
            dsn: String,
            environment: String,
            release: String,
            isDebug: Boolean,
        ) {
            initCalls++
            lastDsn = dsn
            lastEnvironment = environment
            lastRelease = release
            lastIsDebug = isDebug
            throwOnInit?.let { throw it }
        }

        override fun track(event: AnalyticsEvent) = Unit

        override fun captureException(throwable: Throwable) = Unit
    }

    @Test
    fun `initialize forwards every AnalyticsBootstrapConfig value to AnalyticsService_init`() {
        val analytics = RecordingAnalyticsService()
        val config =
            AnalyticsBootstrapConfig(
                dsn = "http://abc@localhost:8000/3",
                environment = "development",
                release = "bisq-connect@0.5.0",
                isDebug = true,
            )
        val service =
            TestApplicationLifecycleService(
                analyticsService = analytics,
                analyticsBootstrapConfig = config,
            )

        // `initialize()` runs `bootstrapAnalytics()` FIRST and then dispatches
        // service activation through `serviceScope.launch`. The downstream
        // service-activation path may throw inside the test fixture (mocked
        // facades, no real Tor, etc.) — we only care about the analytics
        // bootstrap side here. Tolerate the post-analytics failure with
        // runCatching and assert on the recorded analytics state.
        runCatching { service.initialize() }

        assertEquals(1, analytics.initCalls, "AnalyticsService.init must be called exactly once during bootstrap")
        assertEquals(config.dsn, analytics.lastDsn)
        assertEquals(config.environment, analytics.lastEnvironment)
        assertEquals(config.release, analytics.lastRelease)
        assertEquals(config.isDebug, analytics.lastIsDebug)
    }

    @Test
    fun `initialize swallows exceptions thrown by AnalyticsService_init - analytics never crashes the app`() {
        // The whole point of the try/catch around bootstrapAnalytics: analytics
        // is opt-in and non-critical; a Sentry-KMP init crash on a malformed DSN
        // or platform quirk must NOT prevent the app from starting. Regression
        // pin — if someone removes the try/catch during refactor, the analytics
        // exception would propagate out of initialize().
        val boom = RuntimeException("Sentry-KMP refused to dial")
        val analytics = RecordingAnalyticsService(throwOnInit = boom)
        val service =
            TestApplicationLifecycleService(
                analyticsService = analytics,
                analyticsBootstrapConfig =
                    AnalyticsBootstrapConfig(
                        dsn = "http://x@example.org/1",
                        environment = "production",
                        release = "test",
                        isDebug = false,
                    ),
            )

        // `initialize()` may still throw later (downstream service activation),
        // but the analytics-exception MUST be swallowed before that path runs
        // — otherwise the analytics RuntimeException would surface as the
        // first/only exception out of the call. We capture whatever escapes
        // and assert it's NOT our injected RuntimeException.
        val outcome = runCatching { service.initialize() }
        outcome.exceptionOrNull()?.let { escaped ->
            assertEquals(
                false,
                escaped === boom || escaped.cause === boom,
                "Analytics init exception must be swallowed by bootstrap try/catch — got $escaped",
            )
        }

        // Init WAS attempted (proves we actually exercised the try-branch, not
        // the case where init is silently skipped).
        assertEquals(1, analytics.initCalls)
        assertNotNull(analytics.lastDsn)
    }
}
