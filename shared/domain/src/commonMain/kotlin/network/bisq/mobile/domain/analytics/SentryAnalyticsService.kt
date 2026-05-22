package network.bisq.mobile.domain.analytics

import kotlinx.atomicfu.atomic
import network.bisq.mobile.domain.utils.Logging

/**
 * The Sentry-backed [AnalyticsService]. Bound only when the build-time gate
 * (`BuildConfig.ANALYTICS_ENABLED`) is true; otherwise the DI module wires
 * [NoOpAnalyticsService] and this class is never instantiated. R8 then prunes
 * both this class and the Sentry-KMP SDK from release builds.
 *
 * Two runtime guards on top of the build-time gate:
 *
 *  1. **Init guard.** [init] is idempotent — Sentry-KMP keeps internal state
 *     and a second `Sentry.init` is undefined; we silently no-op the second
 *     and beyond. A blank DSN is also a no-op (rather than dialing nowhere /
 *     a typo destination).
 *  2. **Runtime opt-in gate.** Every emit checks [runtimeOptInProvider]
 *     immediately before calling into the SDK. for now it is harcoded to
 *     `true` (so dev-mode opt-in via `feature.analyticsEnabled` is enough to
 *     verify ingestion end-to-end). TODO: swap in a provider that reads `SettingsRepository.analyticsEnabled`.
 *
 * @param sentryClient Indirection over Sentry-KMP for test substitution. See
 *  [DefaultSentryClient] for the production wiring.
 * @param redactor Defence-in-depth scrubber applied via `beforeSend` in the
 *  SDK init. Tested separately at [AnalyticsRedactorTest].
 * @param runtimeOptInProvider Cheap function returning the user's current
 *  consent state. Called on every emit — must NOT block.
 */
class SentryAnalyticsService internal constructor(
    private val sentryClient: SentryClient = DefaultSentryClient,
    private val redactor: AnalyticsRedactor = AnalyticsRedactor(),
    private val runtimeOptInProvider: () -> Boolean = { true },
) : AnalyticsService,
    Logging {
    /**
     * Public constructor for production use. Tests use the internal
     * constructor to inject a fake [SentryClient] / fixed opt-in provider.
     *
     * `runtimeOptInProvider` defaults to permissive — Phase 0 verifies
     * ingestion end-to-end with build-time opt-in alone. Phase 1 will pass
     * a real provider reading `SettingsRepository.analyticsEnabled`.
     */
    constructor() : this(sentryClient = DefaultSentryClient)

    private val initialized = atomic(false)

    override fun init(
        dsn: String,
        environment: String,
        release: String,
    ) {
        // Idempotent: Sentry-KMP holds internal init state and re-init is undefined.
        if (!initialized.compareAndSet(expect = false, update = true)) return
        // Refuse to dial a blank/missing DSN — better to silently do nothing
        // than to send events to a misconfigured destination.
        if (dsn.isBlank()) return
        sentryClient.init(dsn, environment, release, redactor)
        log.d { "Sentry initialized" }
    }

    override fun track(event: AnalyticsEvent) {
        if (!isReadyToEmit()) return
        log.d { "Sentry: Tracking event $event" }
        sentryClient.captureMessage(event.name)
    }

    override fun captureException(throwable: Throwable) {
        if (!isReadyToEmit()) return
        log.w { "Sentry: Tracking exception ${throwable.message}" }
        sentryClient.captureException(throwable)
    }

    private fun isReadyToEmit(): Boolean = initialized.value && runtimeOptInProvider()
}
