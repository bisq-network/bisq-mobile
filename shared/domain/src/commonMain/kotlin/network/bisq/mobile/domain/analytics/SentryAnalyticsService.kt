package network.bisq.mobile.domain.analytics

import kotlinx.atomicfu.atomic
import network.bisq.mobile.domain.utils.Logging

/**
 * The Sentry-backed [AnalyticsService]. Always bound by the DI module — the
 * runtime gates ([runtimeOptInProvider] below) control emission, not whether
 * the class exists. Release builds ship Sentry-KMP linked in but inert until
 * the user-settings toggle is flipped ON.
 *
 * Two runtime guards:
 *
 *  1. **Init guard.** [init] is idempotent — Sentry-KMP keeps internal state
 *     and a second `Sentry.init` is undefined; we silently no-op the second
 *     and beyond. A blank DSN is also a no-op (rather than dialing nowhere /
 *     a typo destination).
 *  2. **Runtime opt-in gate.** Every emit checks [runtimeOptInProvider]
 *     immediately before calling into the SDK. **Defaults to `{ false }` —
 *     callers MUST pass a provider explicitly to enable emission.** Production
 *     wiring combines `BuildConfig.ANALYTICS_DEV_ENABLED` (dev safety) AND
 *     `SettingsRepository.analyticsEnabled.value` (user-facing) — both gates
 *     must be true for the SDK to fire. See `ClientDomainModule` /
 *     `NodeDomainModule` for the exact binding.
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
     * (and what gates emission) is visible at the binding site. Production
     * wiring is `{ BuildConfig.ANALYTICS_DEV_ENABLED && settingsRepository.analyticsEnabled.value }`
     * — see `ClientDomainModule` / `NodeDomainModule`.
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
        // Thread the runtime opt-in provider into the native SDK init so the
        // platform's beforeSend can drop ALL events when the user is opted out
        // — including SDK auto-captures (UncaughtExceptionHandler crashes,
        // ActivityLifecycle, etc.) that don't pass through our [track] /
        // [captureException] gates. Without this, an opted-out user whose
        // app crashes would still ship the crash to GlitchTip.
        sentryClient.init(
            dsn,
            environment,
            release,
            redactor,
            isDebug,
            effectiveHost,
            effectivePort,
            runtimeOptInProvider,
        )
        log.d {
            if (effectiveHost != null) {
                "Sentry initialized with SOCKS proxy at $effectiveHost:$effectivePort"
            } else {
                "Sentry initialized (no proxy — dev / SSH-tunnel mode only)"
            }
        }
    }

    override fun track(event: AnalyticsEvent) {
        if (!isReadyToEmit(event.name)) return
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
        if (!isReadyToEmit("exception:${throwable::class.simpleName}")) return
        log.w { "Sentry: Tracking exception ${throwable.message}" }
        sentryClient.captureException(throwable)
    }

    override fun captureExceptionImmediate(throwable: Throwable) = captureException(throwable)

    /**
     * Both gates must be true. On a `false`, log WHY — silent drops here used
     * to be a debugging nightmare (the upstream [BufferedAnalyticsService]
     * sees no exception and happily logs "forwarded direct", but the event
     * never actually reaches the SDK).
     */
    private fun isReadyToEmit(reason: String): Boolean {
        val init = initialized.value
        val optedIn = runtimeOptInProvider()
        if (!init || !optedIn) {
            log.d {
                "Sentry: dropping '$reason' — initialized=$init, runtimeOptInProvider=$optedIn"
            }
            return false
        }
        return true
    }
}
