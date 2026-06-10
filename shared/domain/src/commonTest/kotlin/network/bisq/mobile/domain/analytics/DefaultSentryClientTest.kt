package network.bisq.mobile.domain.analytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Pins the contract between [DefaultSentryClient] and the platform-specific
 * [NativeSentryInitializer]. The wrapper must forward EVERY argument 1:1 —
 * if it drops or rewrites the SOCKS pair, production builds would dial the
 * GlitchTip onion DSN without a proxy and leak the user's IP.
 *
 * The actual Sentry-KMP `captureMessage` / `captureException` calls happen on
 * a process-global singleton that can't be reset between tests, so we exercise
 * only [DefaultSentryClient.init] here — the contract surface that does any
 * non-trivial work. The two capture methods are intentionally NOT tested in
 * isolation: they're 1-line delegations to `Sentry.captureMessage` /
 * `Sentry.captureException` with no logic to verify, and a test that called
 * them would either mutate global SDK state (test pollution) or be reduced to
 * "asserts that the method exists" (no value).
 */
class DefaultSentryClientTest {
    /**
     * Records every call into the platform initializer so we can assert on
     * argument fidelity. Spy-style.
     */
    private class RecordingNativeInitializer : NativeSentryInitializer {
        var initCalls = 0
            private set
        var lastDsn: String? = null
            private set
        var lastEnvironment: String? = null
            private set
        var lastRelease: String? = null
            private set
        var lastRedactor: AnalyticsRedactor? = null
            private set
        var lastIsDebug: Boolean? = null
            private set
        var lastSocksHost: String? = null
            private set
        var lastSocksPort: Int? = null
            private set

        override fun init(
            dsn: String,
            environment: String,
            release: String,
            redactor: AnalyticsRedactor,
            isDebug: Boolean,
            socksProxyHost: String?,
            socksProxyPort: Int?,
        ) {
            initCalls++
            lastDsn = dsn
            lastEnvironment = environment
            lastRelease = release
            lastRedactor = redactor
            lastIsDebug = isDebug
            lastSocksHost = socksProxyHost
            lastSocksPort = socksProxyPort
        }
    }

    @Test
    fun `init forwards every argument verbatim to the platform initializer`() {
        // Argument fidelity matters because every field is part of the privacy
        // surface: DSN points at a specific GlitchTip project; environment +
        // release shape event grouping; redactor IS the defence-in-depth
        // scrubbing layer; SOCKS pair IS the Tor transport. A silent drop of
        // any of these would either mis-route, mis-tag, or de-anonymise.
        val native = RecordingNativeInitializer()
        val redactor = AnalyticsRedactor()
        val client = DefaultSentryClient(native)

        client.init(
            dsn = "http://abc@onion-host/3",
            environment = "production",
            release = "bisq-connect@0.5.0",
            redactor = redactor,
            isDebug = false,
            socksProxyHost = "127.0.0.1",
            socksProxyPort = 9050,
        )

        assertEquals(1, native.initCalls)
        assertEquals("http://abc@onion-host/3", native.lastDsn)
        assertEquals("production", native.lastEnvironment)
        assertEquals("bisq-connect@0.5.0", native.lastRelease)
        assertEquals(redactor, native.lastRedactor, "must forward the SAME redactor instance — replacing it breaks the scrub layer")
        assertEquals(false, native.lastIsDebug)
        assertEquals("127.0.0.1", native.lastSocksHost)
        assertEquals(9050, native.lastSocksPort)
    }

    @Test
    fun `init with SOCKS args omitted falls back to nulls via default parameters`() {
        // Exercises the Kotlin-generated default-args bridge on the
        // [SentryClient.init] interface method. Without this case, the
        // bridge is dead code from a coverage perspective even though it's
        // used by any caller that opts to skip the optional SOCKS pair.
        // The contract: omitted SOCKS args MUST resolve to nulls (not
        // localhost defaults or "any" sentinels — those would silently
        // route prod traffic without a proxy).
        val native = RecordingNativeInitializer()
        val client = DefaultSentryClient(native)

        client.init(
            dsn = "http://abc@localhost/3",
            environment = "development",
            release = "dev",
            redactor = AnalyticsRedactor(),
            isDebug = true,
            // socksProxyHost + socksProxyPort omitted — exercise the defaults.
        )

        assertEquals(1, native.initCalls)
        assertNull(native.lastSocksHost, "omitted SOCKS host must default to null")
        assertNull(native.lastSocksPort, "omitted SOCKS port must default to null")
    }

    @Test
    fun `init passes null SOCKS arguments through unchanged - no-proxy mode`() {
        // Dev / SSH-tunnel scenarios pass null/null. The wrapper must NOT
        // silently inject a localhost default — that would create a confusing
        // failure mode where dev gets the right behaviour by accident and
        // production breaks when a refactor removes the default.
        val native = RecordingNativeInitializer()
        val client = DefaultSentryClient(native)

        client.init(
            dsn = "http://abc@localhost:8000/3",
            environment = "development",
            release = "dev",
            redactor = AnalyticsRedactor(),
            isDebug = true,
            socksProxyHost = null,
            socksProxyPort = null,
        )

        assertEquals(1, native.initCalls)
        assertNull(native.lastSocksHost)
        assertNull(native.lastSocksPort)
    }
}
