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
 * iOS implementation of [NativeSentryInitializer]. Uses the Sentry-KMP
 * `initWithPlatformOptions` API to reach the underlying Cocoa
 * `cocoapods.Sentry.SentryOptions` — the only path that exposes the
 * `urlSession` setter we need to route ALL outbound Sentry traffic through
 * a SOCKS5-configured `NSURLSession` for Tor hidden-service delivery.
 *
 * Lives in `:apps:clientApp` iosMain (NOT in `:shared:domain`) because the
 * cocoapods Gradle plugin is only applied to clientApp — propagating it to
 * `:shared:domain` would add iOS-specific build complexity to the Android
 * nodeApp which doesn't ship iOS.
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
 * that the iOS SDK honours the keys in our deployment configuration.
 *
 * The string literal keys are used (matching Ktor's choice) rather than the
 * `kCFNetworkProxies*` CFNetwork constants to avoid a `platform.CFNetwork`
 * cinterop binding for a trivial saving.
 *
 * ## Privacy guarantees enforced
 *
 *  - `sendDefaultPii = false` — no IP, no user, no device id auto-attached.
 *  - `beforeSend` runs the injected [AnalyticsRedactor] over `event.message`
 *    (replacing the entire `SentryMessage` since `formatted` is readonly on
 *    Cocoa, set only via the initialiser) and each `SentryException.value`.
 *  - `urlSession` is the SOCKS-routed session when [socksProxyHost] and
 *    [socksProxyPort] are both non-null; otherwise unset (SDK builds its own
 *    default ephemeral session). The DI module is responsible for never
 *    handing in a half-set pair — [SentryAnalyticsService] enforces that.
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
            options.dsn = dsn
            options.environment = environment
            options.releaseName = release
            options.debug = isDebug
            options.sendDefaultPii = false
            options.beforeSend = { event ->
                event?.let { redactInPlace(it, redactor) }
                event
            }
            if (socksProxyHost != null && socksProxyPort != null) {
                options.urlSession = buildSocksRoutedSession(socksProxyHost, socksProxyPort)
            }
        }
        log.d {
            if (socksProxyHost != null) {
                "Sentry-Cocoa initialized (SOCKS5 $socksProxyHost:$socksProxyPort)"
            } else {
                "Sentry-Cocoa initialized (direct — no proxy)"
            }
        }
    }

    private fun redactInPlace(
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
        // SentryException.value is a settable copy-typed NSString.
        event.exceptions?.forEach { obj ->
            val ex = obj as? cocoapods.Sentry.SentryException ?: return@forEach
            ex.value = redactor.redact(ex.value)
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
    }
}
