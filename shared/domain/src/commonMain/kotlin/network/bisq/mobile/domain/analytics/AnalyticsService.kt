package network.bisq.mobile.domain.analytics

/**
 * Opt-in analytics surface for Bisq Mobile (issue #525).
 *
 * Two layers of gating must BOTH be true for any event to leave the device:
 *
 *  1. **Dev-only build gate** — `BuildConfig.ANALYTICS_DEV_ENABLED`. In RELEASE
 *     builds this is hardcoded to true at BuildConfig generation time. In DEBUG
 *     builds it reads `feature.analyticsDevEnabled` from gradle/local.properties
 *     (default false), letting contributors keep their dev builds silent even
 *     if they toggle the user setting ON for testing. Protects the production
 *     GlitchTip from being polluted by dev test traffic.
 *  2. **User-facing runtime gate** — `SettingsRepository.analyticsEnabled`.
 *     The user-controlled switch in the Settings UI. Default OFF. Checked on
 *     every emit; flipping it ON in a release build starts emission
 *     immediately, no rebuild required.
 *
 * The API surface is deliberately narrow:
 *  - [init] is called once during app bootstrap.
 *  - [track] / [trackImmediate] take a sealed [AnalyticsEvent] — there is no
 *    free-form `trackEvent(name, props)` overload. This makes it structurally
 *    impossible for trade data, user inputs, or other PII to slip in via untyped
 *    maps. Every analytics event is enumerated in [AnalyticsEvent]; adding one
 *    is a single-line addition reviewable in diff.
 *  - [captureException] / [captureExceptionImmediate] are for crash reports;
 *    the redactor scrubs message and stack frames before send.
 *
 * The `*Immediate` variants exist to handle critical signals (e.g. a fatal
 * crash about to take the process down) that should not sit in any local
 * buffer if the transport is ready right now. See [BufferedAnalyticsService]
 * for the buffering wrapper that makes the distinction meaningful — the
 * underlying Sentry impl treats both pairs identically.
 */
interface AnalyticsService {
    /**
     * Initialise the underlying SDK. Implementations MUST be safe to call
     * before the user has opted in — they typically configure the SDK but
     * don't send anything until the runtime gate flips.
     *
     * @param dsn The per-app DSN identifying the GlitchTip project to receive
     * events. Empty / blank means analytics is effectively disabled regardless
     * of build/runtime gates — implementations MUST refuse to init with a
     * blank DSN rather than send to a wrong destination.
     * @param environment "development" for opted-in debug builds, "production"
     * for shipped builds.
     * @param release Release identifier used by GlitchTip to group events and
     * match against uploaded proguard/dSYM mappings.
     * @param isDebug When true the underlying SDK logs verbose internal
     * diagnostics (envelope POSTs, transport failures, etc.) to logcat — useful
     * for debugging ingestion locally. When false only ERROR-level SDK problems
     * are logged, keeping shipped builds quiet even when a user opts in.
     * @param socksProxyHost Optional SOCKS5 proxy host (typically `127.0.0.1`
     * pointing at the local kmp-tor instance). Required for production: the
     * GlitchTip server lives behind a Tor hidden service and `.onion`
     * addresses are unresolvable without a proxy. When null, the SDK uses
     * the platform default network stack — only safe for SSH-tunnel dev
     * setups where the DSN points at localhost.
     * @param socksProxyPort Paired with [socksProxyHost]. Must both be
     * non-null or both null — implementations interpret a half-set pair as
     * misconfiguration and ignore the proxy entirely.
     */
    fun init(
        dsn: String,
        environment: String,
        release: String,
        isDebug: Boolean,
        socksProxyHost: String? = null,
        socksProxyPort: Int? = null,
    )

    /**
     * Emit a typed analytics event. No-op when either gate is closed.
     *
     * The redactor (see `AnalyticsRedactor`) is applied to the event's name
     * and any attached scope data as a defense-in-depth measure — but the
     * sealed-class API is the primary guarantee that no PII can be attached.
     *
     * When wrapped by [BufferedAnalyticsService] (the default in the DI graph)
     * the call enqueues to an in-memory buffer if Sentry isn't yet ready
     * (Tor still bootstrapping); periodic + ready-trigger flush drains it.
     * Callers do not need to know whether the transport is up.
     */
    fun track(event: AnalyticsEvent)

    /**
     * Same as [track], but signals "send this NOW if possible". When the
     * underlying transport is ready it goes directly through the SDK; when
     * not, it jumps to the HEAD of the buffer so it leaves before any
     * lower-priority events still queued.
     *
     * Intended for signals close to the wire — e.g. a final marker emitted
     * from an unhandled-exception handler just before the process dies, or
     * an explicit "user opted in" event that we want to land before any
     * incidental screen-views buffered behind it.
     *
     * Reach for normal [track] by default. The Sentry impl itself does not
     * differentiate; the priority semantics live in the buffering wrapper.
     */
    fun trackImmediate(event: AnalyticsEvent)

    /**
     * Capture an unhandled exception. The redactor scrubs the message and
     * any in-stack-frame strings before send. No-op when either gate is closed.
     */
    fun captureException(throwable: Throwable)

    /**
     * Same priority semantics as [trackImmediate] but for crash captures.
     * Use when the exception is the reason the app is about to die and the
     * buffer drain may never get to run on a subsequent app launch.
     */
    fun captureExceptionImmediate(throwable: Throwable)
}
