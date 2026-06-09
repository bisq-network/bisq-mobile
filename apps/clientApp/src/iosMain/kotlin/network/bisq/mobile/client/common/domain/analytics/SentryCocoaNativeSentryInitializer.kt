@file:OptIn(ExperimentalForeignApi::class)

package network.bisq.mobile.client.common.domain.analytics

import io.sentry.kotlin.multiplatform.Sentry
import kotlinx.cinterop.ExperimentalForeignApi
import network.bisq.mobile.domain.analytics.AnalyticsRedactor
import network.bisq.mobile.domain.analytics.NativeSentryInitializer
import network.bisq.mobile.domain.utils.Logging
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import cocoapods.Sentry.SentryEvent as CocoaSentryEvent
import cocoapods.Sentry.SentryMessage as CocoaSentryMessage

/**
 * iOS implementation of [NativeSentryInitializer]. Two responsibilities:
 *
 *  1. Configure Sentry-Cocoa's `SentryOptions` to route all transport through
 *     a SOCKS5-configured `NSURLSession` pointing at kmp-tor's local SOCKS
 *     port (so envelopes travel over Tor's onion routing).
 *  2. Hold the privacy line. Sentry-Cocoa's defaults are LOUD — 17 default
 *     integrations including session replay, view hierarchy capture, network
 *     breadcrumbs, swizzling-based view-controller tracing, auto session
 *     tracking, MetricKit kernel telemetry, etc. Even with `sendDefaultPii=false`
 *     the auto-generated `user.id`, full `culture`/`device` contexts, debug
 *     symbol paths from `/Users/...`, and HTTP breadcrumbs leaking the user's
 *     trusted-node onion URL all shipped on the wire — verified on a clean
 *     iOS simulator run 2026-06-09. Every integration listed in
 *     [setupPrivacy] is explicitly disabled, AND the [beforeSend] scrubber
 *     reduces every event to the minimal payload contract.
 *
 * Disabled integrations rationale: same as the Android cousin (see
 * [SentryJavaNativeSentryInitializer] kdoc); the integration names just
 * differ between platforms. The two killswitches that are extra-load-bearing
 * on iOS:
 *
 *  - **enableNetworkBreadcrumbs / enableAutoBreadcrumbTracking**: Sentry's
 *    iOS swizzles NSURLSession to drop a breadcrumb on every HTTP call.
 *    Without disabling, every Sentry event ships a list of the user's
 *    recent trusted-node onion requests — direct violation of the no-trade-
 *    data contract, and effectively de-anonymises which trusted node a user
 *    is paired with.
 *  - **enableAutoSessionTracking**: generates a per-launch `user.id` UUID
 *    even with `sendDefaultPii=false`, attached to every event. Violates the
 *    "no user/device IDs" contract on its own.
 *
 * Kept:
 *  - **enableCrashHandler** (default true): we want uncaught NSException /
 *    Mach exception capture, just like Android keeps the Java uncaught
 *    handler. Stack frames on real exception events ARE useful; [beforeSend]
 *    leaves them alone (it only kills the [SentryEvent.threads] auto-attached
 *    on `track()` events).
 *
 * ## SOCKS5 routing
 *
 * NSURLSession honours these keys on its `connectionProxyDictionary` to send
 * traffic over a SOCKS5 proxy:
 *  - `SOCKSEnable = 1`
 *  - `SOCKSProxy = <host>`
 *  - `SOCKSPort = <port>`
 *
 * This is the SAME pattern Ktor's Darwin engine uses in production (see
 * `io.ktor.client.engine.darwin.ProxySupportCommon.setupSocksProxy` in
 * ktor-client-darwin sources) — it's the WebSocket transport that connects
 * to a Tor onion-bound trusted node in this same app, so we know empirically
 * that iOS honours the keys.
 *
 * ## KNOWN LIMITATIONS (deferred to follow-up tasks)
 *
 *  - **Stale port after restart**: SentrySDK.start is one-shot per process;
 *    the urlSession is captured at init time. If kmp-tor restarts mid-session
 *    (e.g. lifecycle service hits the "activate called while already
 *    activated" path and tears everything down), the SOCKS port changes and
 *    Sentry's NSURLSession is permanently stale. Analytics dies for the rest
 *    of the session. See [KmpTorSocksPortProvider] kdoc.
 *  - **SentryReachability `sentry.io` probe**: Sentry-Cocoa hardcodes
 *    `SCNetworkReachability` against `sentry.io` to detect "Internet
 *    connection back" for flushing cached envelopes. The check fires DNS
 *    lookups against `sentry.io` over clearnet — independent of our proxy
 *    config. Worth patching upstream or via cocoapods source override; for
 *    Phase 1 it's a minor leak (DNS only, no payload) compared to what we've
 *    already fixed.
 */
class SentryCocoaNativeSentryInitializer :
    NativeSentryInitializer,
    Logging {
    override fun init(
        dsn: String,
        environment: String,
        release: String,
        redactor: AnalyticsRedactor,
        isDebug: Boolean,
        socksProxyHost: String?,
        socksProxyPort: Int?,
    ) {
        Sentry.initWithPlatformOptions { options ->
            // Identity / build-time config
            options.dsn = dsn
            options.environment = environment
            options.releaseName = release
            options.debug = isDebug

            setupPrivacy(options, redactor)
            setupTransport(options, socksProxyHost, socksProxyPort)
        }
        log.d {
            if (socksProxyHost != null) {
                "Sentry-Cocoa initialized (SOCKS5 $socksProxyHost:$socksProxyPort)"
            } else {
                "Sentry-Cocoa initialized (direct — no proxy)"
            }
        }
    }

    private fun setupPrivacy(
        options: cocoapods.Sentry.SentryOptions,
        redactor: AnalyticsRedactor,
    ) {
        options.sendDefaultPii = false

        // Killswitches — see class kdoc for rationale.
        options.enableAutoSessionTracking = false
        options.enableNetworkBreadcrumbs = false
        options.enableAutoBreadcrumbTracking = false
        options.enableCaptureFailedRequests = false
        options.enableNetworkTracking = false
        options.enableSwizzling = false
        options.enableWatchdogTerminationTracking = false
        options.enableAppHangTracking = false
        options.enableUIViewControllerTracing = false
        options.enableUserInteractionTracing = false
        options.enableAutoPerformanceTracing = false
        options.enableFileIOTracing = false
        options.enableCoreDataTracing = false
        options.attachStacktrace = false

        // beforeSend is defence in depth — see [applyMinimalPayloadContract].
        options.beforeSend = { event ->
            event?.let { applyMinimalPayloadContract(it, redactor) }
            event
        }
    }

    /**
     * Strips every event down to the privacy contract:
     *  - `message` + `level` + `timestamp` + `release` + `environment`
     *  - `contexts.os.{name, version}` (useful iOS version split)
     *  - `contexts.device.{boot_time, battery_level, free_memory, storage_size}`
     *    (volatile diagnostic info — useful for OOM/storage debugging,
     *    no device fingerprint)
     *
     * Everything else — `user`, `breadcrumbs`, `threads`, `debugMeta`,
     * `tags`, `extras`, `contexts.app`, `contexts.culture`, every other
     * device field — is cleared.
     */
    private fun applyMinimalPayloadContract(
        event: CocoaSentryEvent,
        redactor: AnalyticsRedactor,
    ) {
        scrubMessage(event, redactor)
        scrubExceptions(event, redactor)

        // Things Sentry auto-fills that violate the contract.
        event.user = null
        event.breadcrumbs = null
        event.threads = null
        event.debugMeta = null
        event.serverName = null
        event.dist = null
        event.tags = null
        event.extra = null

        // Rebuild contexts dict to only carry the minimum.
        event.context = minimalContextsFrom(event.context)
    }

    private fun scrubMessage(
        event: CocoaSentryEvent,
        redactor: AnalyticsRedactor,
    ) {
        // Cocoa SentryMessage.formatted is readonly (set via initWithFormatted:),
        // so to redact a message that already has a formatted value we swap in
        // a new SentryMessage built around the redacted text. The raw `message`
        // template is mutable so we redact that field directly when present.
        event.message?.let { msg ->
            val redactedFormatted = redactor.redact(msg.formatted)
            val replacement = CocoaSentryMessage(formatted = redactedFormatted)
            msg.message?.let { replacement.message = redactor.redact(it) }
            event.message = replacement
        }
    }

    private fun scrubExceptions(
        event: CocoaSentryEvent,
        redactor: AnalyticsRedactor,
    ) {
        // SentryException.value is a settable copy-typed NSString.
        event.exceptions?.forEach { obj ->
            val ex = obj as? cocoapods.Sentry.SentryException ?: return@forEach
            ex.value = redactor.redact(ex.value)
        }
    }

    /**
     * Rebuilds the contexts NSDictionary with only allowlisted keys.
     * Source `os` keeps `name`/`version`; source `device` keeps the volatile
     * diagnostic quartet; everything else is dropped (app, culture, trace,
     * runtime, gpu, etc.). If the source dict is null or doesn't carry the
     * `os`/`device` sub-dicts, we still return a non-null empty dict so the
     * shape stays consistent and Sentry doesn't auto-repopulate.
     */
    @Suppress("UNCHECKED_CAST")
    private fun minimalContextsFrom(
        source: Map<Any?, *>?,
    ): Map<Any?, Map<Any?, *>>? {
        if (source == null) return null
        val rebuilt = mutableMapOf<Any?, Map<Any?, *>>()

        (source["os"] as? Map<Any?, *>)?.let { os ->
            val kept: Map<Any?, Any?> =
                OS_KEEP_KEYS
                    .mapNotNull { key -> os[key]?.let { key to it } }
                    .toMap()
            if (kept.isNotEmpty()) rebuilt["os"] = kept
        }

        (source["device"] as? Map<Any?, *>)?.let { device ->
            val kept: Map<Any?, Any?> =
                DEVICE_KEEP_KEYS
                    .mapNotNull { key -> device[key]?.let { key to it } }
                    .toMap()
            if (kept.isNotEmpty()) rebuilt["device"] = kept
        }

        // Everything else — app, culture, trace, runtime, gpu, etc. — is
        // intentionally dropped. If you add a key here, justify it against
        // the "By design, not by promise" wiki contract.
        return rebuilt
    }

    private fun setupTransport(
        options: cocoapods.Sentry.SentryOptions,
        socksProxyHost: String?,
        socksProxyPort: Int?,
    ) {
        if (socksProxyHost != null && socksProxyPort != null) {
            options.urlSession = buildSocksRoutedSession(socksProxyHost, socksProxyPort)
        }
    }

    private fun buildSocksRoutedSession(
        host: String,
        port: Int,
    ): NSURLSession {
        val cfg = NSURLSessionConfiguration.ephemeralSessionConfiguration
        cfg.connectionProxyDictionary =
            mapOf<Any?, Any?>(
                SOCKS_ENABLE_KEY to 1,
                SOCKS_PROXY_KEY to host,
                SOCKS_PORT_KEY to port,
            )
        return NSURLSession.sessionWithConfiguration(cfg)
    }

    private companion object {
        // String literal keys for NSURLSessionConfiguration.connectionProxyDictionary,
        // matching Ktor's Darwin engine. Equivalent to the CFNetwork constants
        // `kCFNetworkProxiesSOCKSEnable` / `…Proxy` / `…Port` but avoids needing
        // a platform.CFNetwork cinterop binding.
        const val SOCKS_ENABLE_KEY = "SOCKSEnable"
        const val SOCKS_PROXY_KEY = "SOCKSProxy"
        const val SOCKS_PORT_KEY = "SOCKSPort"

        // OS context keep-list. `build` (e.g. simulator build hash) and
        // `kernel_version` are fingerprint-adjacent; `rooted` is jailbreak
        // detection irrelevant to our use case.
        val OS_KEEP_KEYS = listOf("name", "version")

        // Device context keep-list. Volatile (rotates with reboot / charge /
        // OS pressure) so doesn't fingerprint, but useful for triage:
        //  - boot_time     → "did this start happening after a reboot?"
        //  - battery_level → low-battery-throttling diagnostic
        //  - charging      → "happened while plugged in?" correlation
        //  - free_memory   → OOM correlation
        //  - free_storage  → "out of disk" correlation
        //  - low_memory    → OS-level memory-pressure flag
        // The static counterparts (`memory_size` = total RAM, `storage_size`
        // = total disk capacity) are deliberately NOT kept: they fingerprint
        // the device tier (e.g. 256GB vs 128GB iPhone).
        // EVERY other field (id, manufacturer, brand, family, model, model_id,
        // arch, locale, timezone, screen_*, processor_*, thermal_state,
        // simulator, …) is dropped.
        val DEVICE_KEEP_KEYS =
            listOf(
                "boot_time",
                "battery_level",
                "charging",
                "free_memory",
                "free_storage",
                "low_memory",
            )
    }
}
