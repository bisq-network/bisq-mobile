package network.bisq.mobile.domain.analytics

/**
 * Platform-specific bootstrap of the Sentry SDK. Exists because the cross-
 * platform `Sentry.init { commonOpts -> … }` path that Phase 0 used does NOT
 * expose proxy / `urlSession` configuration — Phase 1 needs SOCKS5 routing
 * through kmp-tor, which is only reachable via
 * `Sentry.initWithPlatformOptions { platformOpts -> … }` where `platformOpts`
 * is the native Sentry options type (Sentry Android Java's `SentryAndroidOptions`
 * on Android, Cocoa's `SentryOptions` on iOS).
 *
 * `:shared:domain` deliberately does NOT apply the cocoapods plugin (clientApp
 * owns that). Putting the iOS init code in `commonMain` via `expect/actual`
 * would require either propagating cocoapods to `:shared:domain` (build complexity
 * for the nodeApp, which doesn't ship iOS) or moving the iOS actual to a
 * different module than its expect (which KMP doesn't allow).
 *
 * The interface dodges that problem: declare the contract here in commonMain,
 * provide the Android implementation in `:shared:domain` androidMain
 * (`SentryJavaNativeSentryInitializer` — uses Sentry Java types, no cocoapods),
 * provide the iOS implementation in `:apps:clientApp` iosMain
 * (`SentryCocoaNativeSentryInitializer` — uses cocoapods Sentry types) since
 * clientApp is the only iOS-targeted module. Each app's DI module binds the
 * appropriate implementation.
 *
 * Implementations MUST:
 *  - Call `Sentry.initWithPlatformOptions` at most once. Sentry-KMP keeps
 *    internal init state; a second call is undefined behaviour.
 *  - Set `sendDefaultPii = false`.
 *  - Wire the [AnalyticsRedactor] as `beforeSend` to scrub message + exception
 *    text as defence-in-depth on top of the sealed [AnalyticsEvent] API.
 *  - When [socksProxyHost] and [socksProxyPort] are both non-null, configure
 *    the SDK transport to route through that SOCKS5 proxy. When either is
 *    null, configure no proxy (caller is responsible for not handing in a
 *    half-set pair — [SentryAnalyticsService] enforces this before calling
 *    this interface).
 *
 * @see SentryClient
 * @see DefaultSentryClient
 */
interface NativeSentryInitializer {
    /**
     * @param runtimeOptInProvider cheap synchronous lambda the platform's
     *   `beforeSend` MUST consult before letting any event reach the wire.
     *   Returning false drops the event entirely (including auto-captured
     *   crashes from UncaughtExceptionHandler / ActivityLifecycle), giving
     *   the user-settings toggle authority over the SDK's own pipelines —
     *   not just our manual `track()` / `captureException()` callsites.
     */
    fun init(
        dsn: String,
        environment: String,
        release: String,
        redactor: AnalyticsRedactor,
        isDebug: Boolean,
        socksProxyHost: String?,
        socksProxyPort: Int?,
        runtimeOptInProvider: () -> Boolean,
    )
}
