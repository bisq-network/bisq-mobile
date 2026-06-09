package network.bisq.mobile.domain.analytics

import io.sentry.SentryOptions
import io.sentry.kotlin.multiplatform.Sentry
import network.bisq.mobile.domain.utils.Logging
import java.net.Proxy

/**
 * Android implementation of [NativeSentryInitializer]. Uses the Sentry-KMP
 * `initWithPlatformOptions` API to reach the underlying Sentry Java
 * `SentryAndroidOptions` (a subtype of `SentryOptions`) — the only path that
 * exposes the `setProxy(...)` call we need for SOCKS5 routing through Tor.
 *
 * Lives in `:shared:domain` androidMain (NOT in `apps/clientApp` or
 * `apps/nodeApp`) because both Android apps need the exact same Android init
 * behaviour. Shared code, single source of truth, single test surface.
 *
 * The class deliberately has no Android-platform dependencies beyond Sentry
 * itself — no `Context`, no Android lifecycle hooks. This keeps it cheap to
 * test on the JVM unit-test surface that `:shared:domain` already has.
 *
 * Privacy guarantees enforced in init (in addition to whatever the calling DI
 * module already gates):
 *  - `sendDefaultPii = false` — no IP, no user, no device id auto-attached.
 *  - `beforeSend` runs the injected [AnalyticsRedactor] over `message.message`,
 *    `message.formatted`, and each `SentryException.value`. Sentry does not
 *    auto-capture stack frames with source variables in release builds, but
 *    a thrown `IllegalArgumentException("user typed: foo@bar")` would still
 *    ship the message text — the redactor scrubs that.
 *  - When a SOCKS host/port pair is provided, the SDK is wired with
 *    `Proxy.Type.SOCKS` pointing at it. Sentry Java's HTTP transport then
 *    routes ALL outbound traffic (envelopes + session updates + transaction
 *    samples) through that proxy — verified empirically in Phase 0 by
 *    pointing at localhost:9050 and confirming traffic on the Tor side.
 */
class SentryJavaNativeSentryInitializer :
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
            options.release = release
            options.isDebug = isDebug
            // The privacy-relevant defaults — these are repeated here AS WELL AS
            // documented in the interface contract on purpose. Anyone touching
            // this file is looking at exactly what gets shipped on the wire.
            options.isSendDefaultPii = false
            options.beforeSend =
                SentryOptions.BeforeSendCallback { event, _ ->
                    event.message?.let { msg ->
                        msg.message?.let { msg.message = redactor.redact(it) }
                        msg.formatted?.let { msg.formatted = redactor.redact(it) }
                    }
                    event.exceptions?.forEach { ex ->
                        ex.value?.let { ex.value = redactor.redact(it) }
                    }
                    event
                }
            if (socksProxyHost != null && socksProxyPort != null) {
                options.setProxy(
                    SentryOptions.Proxy(
                        socksProxyHost,
                        socksProxyPort.toString(),
                        Proxy.Type.SOCKS,
                    ),
                )
            }
        }
        log.d {
            if (socksProxyHost != null) {
                "Sentry-Android initialized (SOCKS5 $socksProxyHost:$socksProxyPort)"
            } else {
                "Sentry-Android initialized (direct — no proxy)"
            }
        }
    }
}
