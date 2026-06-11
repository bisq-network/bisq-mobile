package network.bisq.mobile.domain.analytics

/**
 * Inert [AnalyticsService] implementation. Used as the default for test
 * fixtures (see `TestApplicationLifecycleService`) and as a safe substitute in
 * code paths that don't care about analytics emission. Never bound in
 * production DI — both apps always bind [SentryAnalyticsService] now; the two
 * runtime gates (dev-only build flag + user-settings toggle) handle suppression.
 *
 * By construction, when this implementation is wired:
 *  - Sentry-KMP is never touched.
 *  - No DSN is dialled, no network traffic is generated.
 *  - All calls return [Unit] without observable side effects.
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
        socksProxyHost: String?,
        socksProxyPort: Int?,
    ) {
        Unit // build-time gate selected NoOp, so there's nothing to dial
    }

    override fun track(event: AnalyticsEvent) {
        Unit // build-time gate selected NoOp, so there's nothing to track
    }

    override fun trackImmediate(event: AnalyticsEvent) {
        Unit // build-time gate selected NoOp; immediate semantics are still no-op
    }

    override fun captureException(throwable: Throwable) {
        Unit // build-time gate selected NoOp, so there's nothing to capture
    }

    override fun captureExceptionImmediate(throwable: Throwable) {
        Unit // build-time gate selected NoOp; immediate semantics are still no-op
    }
}
