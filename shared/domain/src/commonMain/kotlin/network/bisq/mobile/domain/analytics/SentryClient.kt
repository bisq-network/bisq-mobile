package network.bisq.mobile.domain.analytics

import io.sentry.kotlin.multiplatform.Sentry

/**
 * Thin abstraction over the Sentry-KMP top-level [Sentry] object. The only
 * reason this exists is unit-testability: the production code path goes through
 * the global `Sentry` singleton which cannot be reset between tests, but our
 * own [SentryAnalyticsService] should be exercisable in isolation with a fake
 * collaborator. Production code wires [DefaultSentryClient]; tests inject a
 * fake.
 *
 * Methods are 1:1 with the [Sentry] API surface we actually use — kept narrow
 * on purpose so the privacy review surface is small.
 */
internal interface SentryClient {
    /**
     * Initialise the underlying SDK with the privacy posture required by
     * bisq-network/bisq-mobile#525:
     *  - `sendDefaultPii = false` (no IP, no user, no device id auto-attached).
     *  - `beforeSend` runs the [AnalyticsRedactor] over message + exception
     *    text as a defence-in-depth layer for whatever made it past the
     *    sealed-event API.
     *  - `beforeSend` ALSO consults [runtimeOptInProvider] and drops the event
     *    when it returns false — this is the only gate that catches the SDK's
     *    auto-installed pipelines (UncaughtExceptionHandler, ActivityLifecycle,
     *    etc.) when a user opts out AFTER init has happened.
     *  - SOCKS5 proxy when [socksProxyHost] + [socksProxyPort] are non-null —
     *    required for production where the GlitchTip server lives on a Tor
     *    hidden service.
     *
     * Implementations MUST be safe to call only once per process — Sentry-KMP
     * keeps internal init state and re-init is undefined.
     *
     * [isDebug] gates the SDK's own verbose internal logging — see the
     * matching field on [AnalyticsBootstrapConfig] for the rationale.
     */
    fun init(
        dsn: String,
        environment: String,
        release: String,
        redactor: AnalyticsRedactor,
        isDebug: Boolean,
        socksProxyHost: String? = null,
        socksProxyPort: Int? = null,
        runtimeOptInProvider: () -> Boolean,
    )

    /**
     * Emit a discrete event. Backs [AnalyticsService.track]. Implementations
     * MUST drop the call silently if the SDK isn't initialised.
     */
    fun captureMessage(message: String)

    /**
     * Capture an exception. Backs [AnalyticsService.captureException]. The
     * SDK auto-installs an `UncaughtExceptionHandler` once initialised, so
     * this is for *handled* exceptions you explicitly want to ship.
     */
    fun captureException(throwable: Throwable)
}

/**
 * Production implementation. Delegates the platform-touching `Sentry.initWithPlatformOptions`
 * call to an injected [NativeSentryInitializer] (provided per app/platform by
 * the DI module) — this keeps the cocoapods-dependent iOS init code out of
 * `:shared:domain`, which doesn't apply the cocoapods plugin.
 *
 * The captureMessage / captureException paths still go through the cross-
 * platform [Sentry] object — they don't touch native types so no platform
 * indirection is needed there.
 */
internal class DefaultSentryClient(
    private val nativeInitializer: NativeSentryInitializer,
) : SentryClient {
    override fun init(
        dsn: String,
        environment: String,
        release: String,
        redactor: AnalyticsRedactor,
        isDebug: Boolean,
        socksProxyHost: String?,
        socksProxyPort: Int?,
        runtimeOptInProvider: () -> Boolean,
    ) {
        nativeInitializer.init(
            dsn = dsn,
            environment = environment,
            release = release,
            redactor = redactor,
            isDebug = isDebug,
            socksProxyHost = socksProxyHost,
            socksProxyPort = socksProxyPort,
            runtimeOptInProvider = runtimeOptInProvider,
        )
    }

    override fun captureMessage(message: String) {
        Sentry.captureMessage(message)
    }

    override fun captureException(throwable: Throwable) {
        Sentry.captureException(throwable)
    }
}
