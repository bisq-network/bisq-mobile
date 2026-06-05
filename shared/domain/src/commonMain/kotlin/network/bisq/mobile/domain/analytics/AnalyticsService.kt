package network.bisq.mobile.domain.analytics

/**
 * Opt-in analytics surface for Bisq Mobile (issue #525).
 *
 * Two layers of gating must both be true for any event to leave the device:
 *
 *  1. **Build-time gate** — `BuildConfig.ANALYTICS_ENABLED`. When false the DI
 *     graph binds [NoOpAnalyticsService] and Sentry-KMP isn't even loaded.
 *     Production builds ship with this OFF. A contributor's fresh clone sends
 *     nothing.
 *  2. **Runtime gate** — the user's explicit opt-in. The DI module wires the
 *     provider to `{ BuildConfig.ANALYTICS_ENABLED }` initially (same source
 *     as the build-time gate, doubly-locked); a follow-up swaps this for
 *     `{ settingsRepository.analyticsEnabled.value }` once the Settings UI
 *     toggle ships. Default at the user level is OFF.
 *
 * The API surface is deliberately narrow:
 *  - [init] is called once during app bootstrap.
 *  - [track] takes a sealed [AnalyticsEvent] — there is no free-form
 *    `trackEvent(name, props)` overload. This makes it structurally impossible
 *    for trade data, user inputs, or other PII to slip in via untyped maps.
 *    Every analytics event is enumerated in [AnalyticsEvent]; adding one is a
 *    single-line addition reviewable in diff.
 *  - [captureException] is for crash reports; the redactor scrubs message and
 *    stack frames before send.
 *
 * Transport in first tests is plain HTTP to localhost:8000 via an SSH tunnel
 * (developer machines only). TODO Tor routing for complete feature impl .
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
     */
    fun init(
        dsn: String,
        environment: String,
        release: String,
        isDebug: Boolean,
    )

    /**
     * Emit a typed analytics event. No-op when either gate is closed.
     *
     * The redactor (see `AnalyticsRedactor`) is applied to the event's name
     * and any attached scope data as a defense-in-depth measure — but the
     * sealed-class API is the primary guarantee that no PII can be attached.
     */
    fun track(event: AnalyticsEvent)

    /**
     * Capture an unhandled exception. The redactor scrubs the message and
     * any in-stack-frame strings before send. No-op when either gate is closed.
     */
    fun captureException(throwable: Throwable)
}
