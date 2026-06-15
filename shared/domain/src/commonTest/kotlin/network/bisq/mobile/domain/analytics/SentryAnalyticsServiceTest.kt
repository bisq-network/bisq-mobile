package network.bisq.mobile.domain.analytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SentryAnalyticsServiceTest {
    // Records every call into the SDK so tests can assert on what would have
    // been sent. Keeps SentryEvent + the redactor wiring out of the test
    // surface (they're covered by DefaultSentryClient + AnalyticsRedactorTest
    // respectively).
    private class FakeSentryClient : SentryClient {
        var initCalls = 0
            private set
        var lastDsn: String? = null
        var lastEnv: String? = null
        var lastRelease: String? = null
        var lastIsDebug: Boolean? = null
        var lastSocksHost: String? = null
        var lastSocksPort: Int? = null
        var lastRuntimeOptInProvider: (() -> Boolean)? = null
        val capturedMessages = mutableListOf<String>()
        val capturedExceptions = mutableListOf<Throwable>()

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
            initCalls++
            lastDsn = dsn
            lastEnv = environment
            lastRelease = release
            lastIsDebug = isDebug
            lastSocksHost = socksProxyHost
            lastSocksPort = socksProxyPort
            lastRuntimeOptInProvider = runtimeOptInProvider
        }

        override fun captureMessage(message: String) {
            capturedMessages += message
        }

        override fun captureException(throwable: Throwable) {
            capturedExceptions += throwable
        }
    }

    private fun newService(
        client: FakeSentryClient = FakeSentryClient(),
        optedIn: Boolean = true,
    ): Pair<SentryAnalyticsService, FakeSentryClient> {
        val service =
            SentryAnalyticsService(
                sentryClient = client,
                runtimeOptInProvider = { optedIn },
            )
        return service to client
    }

    // ============ INIT GUARDS ============

    @Test
    fun `init dials the SDK with the configured DSN - environment and release`() {
        val (service, client) = newService()
        service.init(dsn = "http://abc@localhost:8000/3", environment = "development", release = "0.4.1", isDebug = true)
        assertEquals(1, client.initCalls)
        assertEquals("http://abc@localhost:8000/3", client.lastDsn)
        assertEquals("development", client.lastEnv)
        assertEquals("0.4.1", client.lastRelease)
    }

    @Test
    fun `init forwards isDebug to the SDK client - controls verbose SDK logging`() {
        // Pins the contract that prevents the SDK from spamming logcat with
        // envelope POSTs / transport diagnostics in shipped builds. If a
        // refactor ever drops the isDebug threading, this test fails before
        // the noise reaches a user's logs.
        val (debugService, debugClient) = newService()
        debugService.init("http://abc@localhost:8000/3", "development", "0.4.1", isDebug = true)
        assertEquals(true, debugClient.lastIsDebug, "isDebug=true must reach the SDK in debug builds")

        val (releaseService, releaseClient) = newService()
        releaseService.init("http://abc@localhost:8000/3", "production", "0.5.0", isDebug = false)
        assertEquals(false, releaseClient.lastIsDebug, "isDebug=false must reach the SDK in release builds")
    }

    @Test
    fun `init is idempotent - second call is a silent no-op`() {
        val (service, client) = newService()
        service.init("http://abc@localhost:8000/3", "development", "0.4.1", isDebug = true)
        service.init("http://different@elsewhere/4", "production", "0.5.0", isDebug = false)
        assertEquals(1, client.initCalls)
        // First-call config is preserved; second call's args are dropped.
        assertEquals("http://abc@localhost:8000/3", client.lastDsn)
    }

    @Test
    fun `init with blank DSN refuses to dial`() {
        val (service, client) = newService()
        service.init(dsn = "", environment = "development", release = "0.4.1", isDebug = true)
        assertEquals(0, client.initCalls)
        assertNull(client.lastDsn)
    }

    @Test
    fun `init with whitespace-only DSN refuses to dial`() {
        val (service, client) = newService()
        service.init(dsn = "   ", environment = "development", release = "0.4.1", isDebug = true)
        assertEquals(0, client.initCalls)
    }

    // ============ RUNTIME OPT-IN GATE ============

    @Test
    fun `track is a no-op before init`() {
        val (service, client) = newService()
        service.track(AnalyticsEvent.ScreenOpened.Dashboard)
        assertTrue(client.capturedMessages.isEmpty())
    }

    @Test
    fun `track emits when initialized AND user is opted in`() {
        val (service, client) = newService(optedIn = true)
        service.init("http://abc@localhost:8000/3", "development", "0.4.1", isDebug = true)
        service.track(AnalyticsEvent.ScreenOpened.Dashboard)
        assertEquals(listOf("screen.dashboard_opened"), client.capturedMessages)
    }

    @Test
    fun `track is a no-op when runtime opt-in is false even after init`() {
        val (service, client) = newService(optedIn = false)
        service.init("http://abc@localhost:8000/3", "development", "0.4.1", isDebug = true)
        service.track(AnalyticsEvent.ScreenOpened.Dashboard)
        assertTrue(client.capturedMessages.isEmpty())
    }

    @Test
    fun `runtime opt-in is checked PER call - not just at init`() {
        // The provider returns a different value each invocation — proves we
        // re-query each time rather than caching the value from init.
        var consented = false
        val client = FakeSentryClient()
        val service = SentryAnalyticsService(client, runtimeOptInProvider = { consented })
        service.init("http://abc@localhost:8000/3", "development", "0.4.1", isDebug = true)

        service.track(AnalyticsEvent.ScreenOpened.Dashboard)
        assertTrue(client.capturedMessages.isEmpty())

        consented = true
        service.track(AnalyticsEvent.ScreenOpened.Dashboard)
        assertEquals(1, client.capturedMessages.size)

        consented = false
        service.track(AnalyticsEvent.ScreenOpened.Dashboard)
        assertEquals(1, client.capturedMessages.size)
    }

    // ============ EXCEPTION CAPTURE ============

    @Test
    fun `captureException is a no-op before init`() {
        val (service, client) = newService()
        service.captureException(RuntimeException("boom"))
        assertTrue(client.capturedExceptions.isEmpty())
    }

    @Test
    fun `captureException ships throwable when initialized AND opted in`() {
        val (service, client) = newService(optedIn = true)
        service.init("http://abc@localhost:8000/3", "development", "0.4.1", isDebug = true)
        val boom = RuntimeException("boom")
        service.captureException(boom)
        assertEquals(listOf<Throwable>(boom), client.capturedExceptions)
    }

    @Test
    fun `captureException is a no-op when opted out`() {
        val (service, client) = newService(optedIn = false)
        service.init("http://abc@localhost:8000/3", "development", "0.4.1", isDebug = true)
        service.captureException(RuntimeException("boom"))
        assertTrue(client.capturedExceptions.isEmpty())
    }

    // ============ IMMEDIATE-PRIORITY PASS-THROUGH ============
    //
    // At the SDK level "immediate" is identical to the normal variant — the
    // priority semantics are enforced upstream by BufferedAnalyticsService.
    // These tests pin that the Sentry impl forwards to the same path, so
    // future refactors that try to differentiate at the SDK level (and
    // accidentally re-introduce the early-event-drop bug) fail loudly.

    @Test
    fun `trackImmediate emits via the same path as track when ready and opted in`() {
        val (service, client) = newService(optedIn = true)
        service.init("http://abc@localhost:8000/3", "development", "0.4.1", isDebug = true)
        service.trackImmediate(AnalyticsEvent.ScreenOpened.Dashboard)
        assertEquals(listOf("screen.dashboard_opened"), client.capturedMessages)
    }

    @Test
    fun `trackImmediate is a no-op when opted out`() {
        val (service, client) = newService(optedIn = false)
        service.init("http://abc@localhost:8000/3", "development", "0.4.1", isDebug = true)
        service.trackImmediate(AnalyticsEvent.ScreenOpened.Dashboard)
        assertTrue(client.capturedMessages.isEmpty(), "runtime opt-in must gate the immediate variant the same way")
    }

    @Test
    fun `trackImmediate is a no-op before init`() {
        val (service, client) = newService()
        service.trackImmediate(AnalyticsEvent.ScreenOpened.Dashboard)
        assertTrue(client.capturedMessages.isEmpty())
    }

    @Test
    fun `captureExceptionImmediate ships throwable via the same path when ready and opted in`() {
        val (service, client) = newService(optedIn = true)
        service.init("http://abc@localhost:8000/3", "development", "0.4.1", isDebug = true)
        val boom = RuntimeException("boom")
        service.captureExceptionImmediate(boom)
        assertEquals(listOf<Throwable>(boom), client.capturedExceptions)
    }

    @Test
    fun `captureExceptionImmediate is a no-op when opted out`() {
        val (service, client) = newService(optedIn = false)
        service.init("http://abc@localhost:8000/3", "development", "0.4.1", isDebug = true)
        service.captureExceptionImmediate(RuntimeException("boom"))
        assertTrue(client.capturedExceptions.isEmpty())
    }

    @Test
    fun `captureExceptionImmediate is a no-op before init`() {
        val (service, client) = newService()
        service.captureExceptionImmediate(RuntimeException("boom"))
        assertTrue(client.capturedExceptions.isEmpty())
    }

    // ============ SOCKS PROXY PASS-THROUGH ============

    @Test
    fun `init forwards both SOCKS host and port to the SDK client`() {
        val (service, client) = newService()
        service.init(
            dsn = "http://abc@localhost:8000/3",
            environment = "development",
            release = "0.4.1",
            isDebug = true,
            socksProxyHost = "127.0.0.1",
            socksProxyPort = 9050,
        )
        assertEquals("127.0.0.1", client.lastSocksHost)
        assertEquals(9050, client.lastSocksPort)
    }

    @Test
    fun `init with no SOCKS args passes null through to the SDK client`() {
        // Dev / SSH-tunnel mode: no proxy configured. The SDK must be wired
        // for direct egress so devs can hit a localhost-tunnelled GlitchTip.
        val (service, client) = newService()
        service.init("http://abc@localhost:8000/3", "development", "0.4.1", isDebug = true)
        assertNull(client.lastSocksHost)
        assertNull(client.lastSocksPort)
    }

    @Test
    fun `init refuses to dial when only SOCKS host is set - half-config is rejected`() {
        // Defence in depth against a refactor that wires only one half of the
        // SOCKS pair. Without this guard we'd silently dial direct (clearnet)
        // when the caller intended Tor — leaking onion-bound traffic.
        val (service, client) = newService()
        service.init(
            dsn = "http://abc@localhost:8000/3",
            environment = "development",
            release = "0.4.1",
            isDebug = true,
            socksProxyHost = "127.0.0.1",
            socksProxyPort = null,
        )
        assertEquals(1, client.initCalls, "init must still happen — degrade to no-proxy rather than refusing entirely")
        assertNull(client.lastSocksHost, "half-config must be downgraded to no proxy, not partially applied")
        assertNull(client.lastSocksPort)
    }

    @Test
    fun `init refuses to dial when only SOCKS port is set - half-config is rejected`() {
        val (service, client) = newService()
        service.init(
            dsn = "http://abc@localhost:8000/3",
            environment = "development",
            release = "0.4.1",
            isDebug = true,
            socksProxyHost = null,
            socksProxyPort = 9050,
        )
        assertEquals(1, client.initCalls)
        assertNull(client.lastSocksHost)
        assertNull(client.lastSocksPort)
    }

    // ============ DEFAULT WIRING ============

    @Test
    fun `DI-friendly public constructor accepts a NativeSentryInitializer plus runtimeOptInProvider`() {
        // Pins the contract for the constructor used by every DI module
        // (ClientDomainModule + NodeDomainModule pass `get<NativeSentryInitializer>()`
        // plus `{ ANALYTICS_DEV_ENABLED && settingsRepository.analyticsEnabled.value }`).
        // Construction must not throw and must NOT actually invoke the native
        // initializer (init is lazy — only fires when init() is called). This
        // catches any refactor that eagerly calls into the SDK at construction time.
        var consented = false
        var nativeInitCalls = 0
        val recordingNative =
            object : NativeSentryInitializer {
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
                    nativeInitCalls++
                }
            }
        val instance =
            SentryAnalyticsService(
                nativeInitializer = recordingNative,
                runtimeOptInProvider = { consented },
            )
        assertNotNull(instance)
        assertEquals(0, nativeInitCalls, "construction must not touch the SDK — init() is the only entry point")
        // Mutating `consented` after construction proves the provider lambda
        // is captured-by-reference, not snapshotted at construction time.
        consented = true
        assertEquals(true, consented)
    }

    @Test
    fun `default runtimeOptInProvider denies emission - safe by default`() {
        // Defence in depth: if a caller wires up SentryAnalyticsService without
        // passing an explicit provider (internal constructor path), the service
        // must NOT emit. The DI module's build-time gate is the primary lock;
        // this is the secondary. If this default ever drifts back to `{ true }`,
        // a refactor that bypasses the DI gate would silently start sending events.
        val client = FakeSentryClient()
        val service = SentryAnalyticsService(sentryClient = client) // no runtimeOptInProvider passed

        service.init("http://abc@localhost:8000/3", "development", "0.4.1", isDebug = true)
        service.track(AnalyticsEvent.ScreenOpened.Dashboard)
        service.captureException(RuntimeException("boom"))

        assertTrue(client.capturedMessages.isEmpty(), "default provider must deny track()")
        assertTrue(client.capturedExceptions.isEmpty(), "default provider must deny captureException()")
    }

    // ============ OPT-IN GATE FORWARDING ============
    //
    // The init must forward the SAME `runtimeOptInProvider` to the underlying
    // SentryClient so the platform's `beforeSend` can drop SDK-auto-captured
    // events (UncaughtExceptionHandler crashes, ActivityLifecycle events,
    // etc.) when the user is opted out. Without this, the gate only catches
    // OUR manual track() / captureException() calls — auto-captured events
    // bypass it entirely and leak post-opt-out.

    @Test
    fun `init forwards the runtimeOptInProvider to the SentryClient`() {
        // Pins the load-bearing contract for Option B (PR #1474 follow-up):
        // the same lambda the service uses to gate its own emit paths must
        // ALSO be threaded into the native init so beforeSend can refuse
        // events at the SDK level. Identity check (===), not equality.
        var consented = true
        val providerLambda: () -> Boolean = { consented }
        val (service, client) = newService(optedIn = true)
        // Replace with a fresh instance whose provider we control by reference.
        val taggedService =
            SentryAnalyticsService(
                sentryClient = client,
                runtimeOptInProvider = providerLambda,
            )

        taggedService.init("http://abc@localhost:8000/3", "development", "0.4.1", isDebug = true)

        assertEquals(1, client.initCalls)
        assertEquals(
            providerLambda,
            client.lastRuntimeOptInProvider,
            "SentryAnalyticsService.init MUST forward the same runtimeOptInProvider — beforeSend uses it as the SDK-level opt-in gate",
        )
        // Bonus: mutating `consented` via the lambda reaches the forwarded
        // lambda too (i.e. it's captured-by-reference, not snapshotted).
        consented = false
        assertEquals(false, client.lastRuntimeOptInProvider?.invoke())
        // Silence the unused warning on the test-helper service.
        assertEquals(client, client.also { service.track(AnalyticsEvent.ScreenOpened.Dashboard) })
    }
}
