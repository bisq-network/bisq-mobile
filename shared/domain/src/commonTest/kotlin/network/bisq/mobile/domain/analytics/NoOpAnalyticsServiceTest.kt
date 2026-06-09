package network.bisq.mobile.domain.analytics

import kotlin.test.Test

class NoOpAnalyticsServiceTest {
    private val service: AnalyticsService = NoOpAnalyticsService

    @Test
    fun `init accepts a real-shaped DSN without throwing`() {
        service.init(
            dsn = "http://abc@localhost:8000/3",
            environment = "development",
            release = "0.4.1",
            isDebug = true,
        )
    }

    @Test
    fun `init accepts blank DSN without throwing - still a no-op`() {
        // Blank DSN is meaningless in the SDK-backed impl, but NoOp ignores
        // everything by design — including bad input.
        service.init(dsn = "", environment = "", release = "", isDebug = false)
    }

    @Test
    fun `track accepts any AnalyticsEvent subtype without throwing`() {
        service.track(AnalyticsEvent.ScreenViewed.Dashboard)
    }

    @Test
    fun `trackImmediate accepts any AnalyticsEvent subtype without throwing`() {
        // Same contract as track for the no-op impl — buffering / priority
        // semantics only matter in BufferedAnalyticsService.
        service.trackImmediate(AnalyticsEvent.ScreenViewed.Dashboard)
    }

    @Test
    fun `captureException accepts any Throwable without throwing`() {
        service.captureException(RuntimeException("boom"))
    }

    @Test
    fun `captureExceptionImmediate accepts any Throwable without throwing`() {
        service.captureExceptionImmediate(RuntimeException("boom"))
    }
}
