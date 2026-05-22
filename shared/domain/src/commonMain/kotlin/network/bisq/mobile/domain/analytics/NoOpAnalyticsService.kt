package network.bisq.mobile.domain.analytics

/**
 * The [AnalyticsService] implementation bound when `BuildConfig.ANALYTICS_ENABLED`
 * is false — i.e. in every production build and every contributor fresh-clone
 * build. By construction, when this implementation is wired:
 *
 *  - Sentry-KMP is not loaded (build-time gate, see DI module).
 *  - No DSN is dialled, no network traffic is generated.
 *  - All calls return [Unit] without observable side effects.
 *
 * Existing because the rest of the codebase calls `analyticsService.track(...)`
 * unconditionally — having an injectable no-op avoids sprinkling `if`s at every
 * call site and keeps the contract identical between gated and ungated builds.
 */
object NoOpAnalyticsService : AnalyticsService {
    override fun init(
        dsn: String,
        environment: String,
        release: String,
    ) = Unit

    override fun track(event: AnalyticsEvent) = Unit

    override fun captureException(throwable: Throwable) = Unit
}
