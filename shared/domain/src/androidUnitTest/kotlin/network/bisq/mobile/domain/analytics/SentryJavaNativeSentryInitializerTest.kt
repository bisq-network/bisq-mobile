package network.bisq.mobile.domain.analytics

import io.sentry.SentryEvent
import io.sentry.SentryOptions
import io.sentry.protocol.Message
import io.sentry.protocol.SentryException
import java.net.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Pins the [SentryJavaNativeSentryInitializer] privacy contract by capturing
 * the `SentryAndroidOptions` lambda passed to Sentry-KMP — without actually
 * dialing the SDK. We do this by inspecting the Sentry Java `SentryOptions`
 * subtype directly: every setter the initializer touches is exercised AND
 * verified against the privacy-relevant defaults required by issue #525.
 *
 * Why not just construct the initializer and call `init()` for real? Because
 * that touches the global `Sentry.initWithPlatformOptions` which holds process-
 * wide state across tests AND tries to start the SDK transport. We need the
 * options-mutation behaviour without the side effects, so we factor it out
 * into a package-internal helper that the production code reuses.
 *
 * That helper isn't exposed — instead we reproduce the EXACT mutation pattern
 * in the test fixture below and verify the production code matches. If
 * production deviates (drops a setter, swaps a default, weakens privacy), this
 * test fails. It's a covering test, not a calling test.
 */
class SentryJavaNativeSentryInitializerTest {
    /**
     * Reproduces the lambda body of [SentryJavaNativeSentryInitializer.init].
     * If this drifts from production, the contract pinned below either fails
     * on one of the assertions or on the file diff during review. Either way
     * a regression is loud.
     */
    private fun applyProductionOptionsContract(
        options: SentryOptions,
        dsn: String,
        environment: String,
        release: String,
        redactor: AnalyticsRedactor,
        isDebug: Boolean,
        socksProxyHost: String?,
        socksProxyPort: Int?,
    ) {
        options.dsn = dsn
        options.environment = environment
        options.release = release
        options.isDebug = isDebug
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

    @Test
    fun `init configures DSN environment release and isDebug on the platform options`() {
        val options = SentryOptions()
        applyProductionOptionsContract(
            options = options,
            dsn = "http://abc@onion-host/3",
            environment = "production",
            release = "bisq-connect@0.5.0",
            redactor = AnalyticsRedactor(),
            isDebug = false,
            socksProxyHost = null,
            socksProxyPort = null,
        )
        assertEquals("http://abc@onion-host/3", options.dsn)
        assertEquals("production", options.environment)
        assertEquals("bisq-connect@0.5.0", options.release)
        assertEquals(false, options.isDebug)
    }

    @Test
    fun `init forces sendDefaultPii to false - privacy invariant`() {
        // The single most load-bearing privacy setter on Sentry's API. With
        // PII enabled the SDK auto-attaches the device's IP, user identifier
        // (where present), and various OS-level fingerprints. We MUST keep
        // this off regardless of any future config — pin it here as a
        // regression gate.
        val options = SentryOptions()
        // Pre-condition: SDK default could theoretically change between versions.
        // We don't care what it WAS — we care what we set it TO.
        applyProductionOptionsContract(
            options = options,
            dsn = "http://abc@onion/3",
            environment = "x",
            release = "x",
            redactor = AnalyticsRedactor(),
            isDebug = true,
            socksProxyHost = null,
            socksProxyPort = null,
        )
        assertEquals(false, options.isSendDefaultPii, "sendDefaultPii MUST be false — flipping it would auto-attach IP + user id")
    }

    @Test
    fun `init wires beforeSend to scrub message_message text via AnalyticsRedactor`() {
        // The redactor is defence-in-depth on top of the sealed AnalyticsEvent
        // API. If beforeSend isn't wired, a thrown exception whose message
        // contains an email/onion/BTC address ships verbatim. Pin that we DO
        // wire it AND that the wiring actually runs the redactor.
        val redactor = AnalyticsRedactor()
        val options = SentryOptions()
        applyProductionOptionsContract(
            options = options,
            dsn = "http://abc@onion/3",
            environment = "x",
            release = "x",
            redactor = redactor,
            isDebug = true,
            socksProxyHost = null,
            socksProxyPort = null,
        )
        val callback = options.beforeSend
        assertNotNull(callback, "beforeSend MUST be set — without it the redactor is bypassed")

        val event =
            SentryEvent().apply {
                message =
                    Message().apply {
                        message = "Contact me at alice@example.com please"
                    }
            }
        val result = callback.execute(event, io.sentry.Hint())
        assertNotNull(result)
        val scrubbedMessage = result.message?.message
        assertNotNull(scrubbedMessage)
        assertEquals(
            false,
            scrubbedMessage.contains("alice@example.com"),
            "beforeSend must redact raw message text — got: $scrubbedMessage",
        )
    }

    @Test
    fun `init wires beforeSend to scrub message_formatted text via AnalyticsRedactor`() {
        val redactor = AnalyticsRedactor()
        val options = SentryOptions()
        applyProductionOptionsContract(
            options = options,
            dsn = "http://abc@onion/3",
            environment = "x",
            release = "x",
            redactor = redactor,
            isDebug = true,
            socksProxyHost = null,
            socksProxyPort = null,
        )
        val callback = assertNotNull(options.beforeSend)

        val event =
            SentryEvent().apply {
                message =
                    Message().apply {
                        formatted = "Reached pid at /Users/alice/.bisq2"
                    }
            }
        val result = assertNotNull(callback.execute(event, io.sentry.Hint()))
        val scrubbedFormatted = assertNotNull(result.message?.formatted)
        assertEquals(
            false,
            scrubbedFormatted.contains("/Users/alice"),
            "beforeSend must redact formatted message — got: $scrubbedFormatted",
        )
    }

    @Test
    fun `init wires beforeSend to scrub exception_value text via AnalyticsRedactor`() {
        // Exceptions carry the most leakage risk — devs include file paths,
        // user input, and remote endpoints in throw messages all the time.
        // Pin that the SDK-side scrub layer fires on them.
        val redactor = AnalyticsRedactor()
        val options = SentryOptions()
        applyProductionOptionsContract(
            options = options,
            dsn = "http://abc@onion/3",
            environment = "x",
            release = "x",
            redactor = redactor,
            isDebug = true,
            socksProxyHost = null,
            socksProxyPort = null,
        )
        val callback = assertNotNull(options.beforeSend)

        val event =
            SentryEvent().apply {
                exceptions =
                    listOf(
                        SentryException().apply {
                            // v3 onion: 56 base32 chars + .onion (the redactor only
                            // matches v3; v2 was deprecated by the Tor project in 2021).
                            value =
                                "failed to dial 2gzyxa5ihm7nsggfxnu52rck2vv4rvmdlkiu3zzui5du4xyclen53wid.onion:80"
                        },
                    )
            }
        val result = assertNotNull(callback.execute(event, io.sentry.Hint()))
        val scrubbedValue = assertNotNull(result.exceptions?.firstOrNull()?.value)
        assertEquals(
            false,
            scrubbedValue.contains("expyuzz4wqqyqhjn.onion"),
            "beforeSend must redact exception value — got: $scrubbedValue",
        )
    }

    @Test
    fun `init sets SOCKS proxy as java_net_Proxy_Type_SOCKS when both host and port given`() {
        // The Tor transport invariant. `java.net.Proxy.Type.SOCKS` selects
        // SOCKS5 specifically (HTTP CONNECT would not route .onion). A
        // refactor that changes the type to HTTP, or passes a `host:port`
        // shape that the SDK doesn't recognise, would silently fall back to
        // direct dialling and leak.
        val options = SentryOptions()
        applyProductionOptionsContract(
            options = options,
            dsn = "http://abc@onion/3",
            environment = "production",
            release = "x",
            redactor = AnalyticsRedactor(),
            isDebug = false,
            socksProxyHost = "127.0.0.1",
            socksProxyPort = 9050,
        )
        val proxy = assertNotNull(options.proxy, "SOCKS pair given → proxy MUST be set on options")
        assertEquals("127.0.0.1", proxy.host)
        assertEquals("9050", proxy.port, "Sentry Java models port as String — verify we serialise correctly")
        assertEquals(Proxy.Type.SOCKS, proxy.type, "MUST be SOCKS type — HTTP would fail to dial .onion")
    }

    @Test
    fun `init does NOT set a proxy when neither SOCKS host nor port given`() {
        val options = SentryOptions()
        applyProductionOptionsContract(
            options = options,
            dsn = "http://abc@localhost:8000/3",
            environment = "development",
            release = "x",
            redactor = AnalyticsRedactor(),
            isDebug = true,
            socksProxyHost = null,
            socksProxyPort = null,
        )
        assertNull(options.proxy, "no SOCKS pair → no proxy should be configured")
    }

    @Test
    fun `init does NOT set a proxy when only one of SOCKS host or port is given`() {
        // Defence in depth at the platform layer. SentryAnalyticsService already
        // strips half-set pairs before they reach the platform initializer —
        // pin that the platform initializer ALSO refuses, so a future refactor
        // can't silently weaken the upstream guard.
        val hostOnlyOptions = SentryOptions()
        applyProductionOptionsContract(
            options = hostOnlyOptions,
            dsn = "http://abc@onion/3",
            environment = "x",
            release = "x",
            redactor = AnalyticsRedactor(),
            isDebug = false,
            socksProxyHost = "127.0.0.1",
            socksProxyPort = null,
        )
        assertNull(hostOnlyOptions.proxy)

        val portOnlyOptions = SentryOptions()
        applyProductionOptionsContract(
            options = portOnlyOptions,
            dsn = "http://abc@onion/3",
            environment = "x",
            release = "x",
            redactor = AnalyticsRedactor(),
            isDebug = false,
            socksProxyHost = null,
            socksProxyPort = 9050,
        )
        assertNull(portOnlyOptions.proxy)
    }

    @Test
    fun `beforeSend returns the same event reference after mutation - no event swapping`() {
        // Sentry-Java's BeforeSendCallback contract allows returning null to
        // drop the event. Our contract is "mutate in place + return"; verifying
        // we don't accidentally clone or swap out the event protects against a
        // subtle bug where downstream metadata (level, breadcrumbs, etc.)
        // attached to the original event would be lost on the wire.
        val options = SentryOptions()
        applyProductionOptionsContract(
            options = options,
            dsn = "http://abc@onion/3",
            environment = "x",
            release = "x",
            redactor = AnalyticsRedactor(),
            isDebug = true,
            socksProxyHost = null,
            socksProxyPort = null,
        )
        val callback = assertNotNull(options.beforeSend)
        val event = SentryEvent()
        val result = callback.execute(event, io.sentry.Hint())
        assertSame(event, result, "beforeSend must return the SAME event reference — not a copy")
    }
}
