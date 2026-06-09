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
    private val sentryClient: SentryClient,
    private val redactor: AnalyticsRedactor = AnalyticsRedactor(),
    private val runtimeOptInProvider: () -> Boolean = { false },
) : AnalyticsService,
    Logging {
    /**
     * Public constructor for DI modules. The DI module is responsible for
     * constructing a [SentryClient] (typically [DefaultSentryClient] wrapping
     * a platform-specific [NativeSentryInitializer]) and passing it here.
     *
     * Takes the runtime opt-in source explicitly so the caller's intent
     * (and what gates emission) is visible at the binding site. Initial work
     * wires this to `{ BuildConfig.ANALYTICS_ENABLED }`; TODO swap to
     * `{ settingsRepository.analyticsEnabled.value }` once the Settings UI
     * toggle ships.
     */
    constructor(
        nativeInitializer: NativeSentryInitializer,
        runtimeOptInProvider: () -> Boolean,
    ) : this(
        sentryClient = DefaultSentryClient(nativeInitializer),
        redactor = AnalyticsRedactor(),
        runtimeOptInProvider = runtimeOptInProvider,
    )

    private val initialized = atomic(false)

    override fun init(
        dsn: String,
        environment: String,
        release: String,
        isDebug: Boolean,
        socksProxyHost: String?,
        socksProxyPort: Int?,
    ) {
        // Idempotent: Sentry-KMP holds internal init state and re-init is undefined.
        if (!initialized.compareAndSet(expect = false, update = true)) return
        // Refuse to dial a blank/missing DSN — better to silently do nothing
        // than to send events to a misconfigured destination.
        if (dsn.isBlank()) return
        // Half-set proxy = misconfiguration; treat as no proxy rather than
        // silently dialling without one (which would leak onion-bound traffic
        // attempts onto clearnet).
        val (effectiveHost, effectivePort) =
            if (socksProxyHost != null && socksProxyPort != null) {
                socksProxyHost to socksProxyPort
            } else {
                if (socksProxyHost != null || socksProxyPort != null) {
                    log.w { "Ignoring half-configured SOCKS proxy (host=$socksProxyHost, port=$socksProxyPort)" }
                }
                null to null
            }
        sentryClient.init(dsn, environment, release, redactor, isDebug, effectiveHost, effectivePort)
        log.d {
            if (effectiveHost != null) {
                "Sentry initialized with SOCKS proxy at $effectiveHost:$effectivePort"
            } else {
                "Sentry initialized (no proxy — dev / SSH-tunnel mode only)"
            }
        }
    }

    override fun track(event: AnalyticsEvent) {
        if (!isReadyToEmit()) return
        log.d { "Sentry: Tracking event $event" }
        sentryClient.captureMessage(event.name)
    }

    /**
     * At the SDK level "immediate" is identical to [track] — Sentry already
     * accepts the event into its own internal queue without blocking on the
     * wire. The priority distinction is enforced by [BufferedAnalyticsService]
     * upstream (HEAD-of-queue placement when the transport isn't ready yet).
     */
    override fun trackImmediate(event: AnalyticsEvent) = track(event)

    override fun captureException(throwable: Throwable) {
        if (!isReadyToEmit()) return
        log.w { "Sentry: Tracking exception ${throwable.message}" }
        sentryClient.captureException(throwable)
    }

    override fun captureExceptionImmediate(throwable: Throwable) = captureException(throwable)

    private fun isReadyToEmit(): Boolean = initialized.value && runtimeOptInProvider()
}
