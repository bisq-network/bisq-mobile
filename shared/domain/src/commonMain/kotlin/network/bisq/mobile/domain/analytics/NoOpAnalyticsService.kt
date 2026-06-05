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
    // Each body has an explicit `Unit` statement so kover sees at least one
    // tracked instruction per method. With `= Unit` shorthand OR comment-only
    // bodies the Kotlin compiler can lower the body to zero instructions,
    // which kover reports as "0 lines covered" even when tests do call the
    // method — making the diff coverage gate red for no real testing reason.
    override fun init(
        dsn: String,
        environment: String,
        release: String,
        isDebug: Boolean,
    ) {
        Unit // build-time gate selected NoOp, so there's nothing to dial
    }

    override fun track(event: AnalyticsEvent) {
        Unit // build-time gate selected NoOp, so there's nothing to track
    }

    override fun captureException(throwable: Throwable) {
        Unit // build-time gate selected NoOp, so there's nothing to capture
    }
}
