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
 *     immediately before calling into the SDK. **Defaults to `{ false }` —
 *     callers MUST pass a provider explicitly to enable emission.** This is
 *     defence in depth: the DI module's build-time check already gates
 *     construction, and the secure-by-default lambda here ensures any future
 *     bypass of that gate (a refactor, a test fixture, a new platform binding)
 *     can't silently start emitting events. Initial work wires the DI modules to
 *     pass `{ BuildConfig.ANALYTICS_ENABLED }` — same source as the build-time
 *     gate, doubly-locked. TODO swap that for
 *     `{ settingsRepository.analyticsEnabled.value }` once the Settings UI
 *     toggle ships.
 *
 * @param sentryClient Indirection over Sentry-KMP for test substitution. See
 *  [DefaultSentryClient] for the production wiring.
 * @param redactor Defence-in-depth scrubber applied via `beforeSend` in the
 *  SDK init. Tested separately at [AnalyticsRedactorTest].
 * @param runtimeOptInProvider Cheap function returning the user's current
 *  consent state. Called on every emit — must NOT block. Defaults to
 *  `{ false }` (no emission) — see "Runtime opt-in gate" above.
 */
class SentryAnalyticsService internal constructor(
    private val sentryClient: SentryClient = DefaultSentryClient,
    private val redactor: AnalyticsRedactor = AnalyticsRedactor(),
    private val runtimeOptInProvider: () -> Boolean = { false },
) : AnalyticsService,
    Logging {
    /**
     * Public no-arg constructor. **Will not emit** — `runtimeOptInProvider`
     * defaults to `{ false }`. Kept as a safe-default fallback; DI modules
     * should prefer the [runtimeOptInProvider] overload below.
     *
     * Tests use the [internal] primary constructor to inject a fake
     * [SentryClient] alongside a fixed opt-in provider.
     */
    constructor() : this(sentryClient = DefaultSentryClient)

    /**
     * Public constructor for DI modules. Takes the runtime opt-in source
     * explicitly so the caller's intent (and what gates emission) is visible
     * at the binding site. Initial work wires this to
     * `{ BuildConfig.ANALYTICS_ENABLED }`; TODO swap to
     * `{ settingsRepository.analyticsEnabled.value }`.
     */
    constructor(runtimeOptInProvider: () -> Boolean) : this(
        sentryClient = DefaultSentryClient,
        redactor = AnalyticsRedactor(),
        runtimeOptInProvider = runtimeOptInProvider,
    )

    private val initialized = atomic(false)

    override fun init(
        dsn: String,
        environment: String,
        release: String,
        isDebug: Boolean,
    ) {
        // Idempotent: Sentry-KMP holds internal init state and re-init is undefined.
        if (!initialized.compareAndSet(expect = false, update = true)) return
        // Refuse to dial a blank/missing DSN — better to silently do nothing
        // than to send events to a misconfigured destination.
        if (dsn.isBlank()) return
        sentryClient.init(dsn, environment, release, redactor, isDebug)
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
