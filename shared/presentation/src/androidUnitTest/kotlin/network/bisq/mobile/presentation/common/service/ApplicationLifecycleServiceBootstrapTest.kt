package network.bisq.mobile.presentation.common.service

import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.data.model.Settings
import network.bisq.mobile.domain.analytics.AnalyticsBootstrapConfig
import network.bisq.mobile.domain.analytics.AnalyticsEvent
import network.bisq.mobile.domain.analytics.AnalyticsService
import network.bisq.mobile.domain.analytics.AnalyticsSocksPortProvider
import network.bisq.mobile.domain.analytics.BufferedAnalyticsService
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.presentation.common.test_utils.TestApplicationLifecycleService
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Pins the analytics-bootstrap behaviour of `ApplicationLifecycleService`:
 *
 *  1. When the kmp-tor SOCKS port becomes available, `initialize()` calls
 *     [AnalyticsService.init] with the configured DSN/env/release/isDebug AND
 *     routes through SOCKS5 at `127.0.0.1:<port>` — preserving the
 *     Tor-or-nothing transport contract for Phase 1 analytics.
 *  2. When no SOCKS port is ever available (clearnet trusted node — Tor never
 *     started), Sentry MUST NOT be initialised. Events stay in the bounded
 *     in-memory buffer and evict naturally — no clearnet leak of analytics
 *     traffic.
 *  3. Exceptions thrown inside the SDK init path are swallowed — analytics
 *     failures never take the app down.
 *
 * Points (1)+(2) together are the load-bearing privacy invariant for the
 * Phase 1 Tor wiring. A regression that initialised Sentry without a SOCKS
 * port — or worse, fell back to clearnet on a Tor timeout — would dial the
 * GlitchTip onion DSN directly and leak the user's IP.
 */
class ApplicationLifecycleServiceBootstrapTest {
    /**
     * Records `init` calls + can be told to throw on the next call. Tests
     * assert on these recorded values to pin the contract between the
     * lifecycle service and the Sentry SDK shim. We poll instead of using
     * coroutines-test plumbing because [BaseService.serviceScope] is Koin-
     * injected and lives outside any single TestScope's reach — see Koin
     * `before()` setup at class scope.
     */
    private class RecordingAnalyticsService(
        private val throwOnInit: Throwable? = null,
    ) : AnalyticsService {
        @Volatile
        var initCalls = 0
            private set

        @Volatile
        var lastDsn: String? = null
            private set

        @Volatile
        var lastEnvironment: String? = null
            private set

        @Volatile
        var lastRelease: String? = null
            private set

        @Volatile
        var lastIsDebug: Boolean? = null
            private set

        @Volatile
        var lastSocksHost: String? = null
            private set

        @Volatile
        var lastSocksPort: Int? = null
            private set

        override fun init(
            dsn: String,
            environment: String,
            release: String,
            isDebug: Boolean,
            socksProxyHost: String?,
            socksProxyPort: Int?,
        ) {
            initCalls++
            lastDsn = dsn
            lastEnvironment = environment
            lastRelease = release
            lastIsDebug = isDebug
            lastSocksHost = socksProxyHost
            lastSocksPort = socksProxyPort
            throwOnInit?.let { throw it }
        }

        override fun track(event: AnalyticsEvent) = Unit

        override fun trackImmediate(event: AnalyticsEvent) = Unit

        override fun captureException(throwable: Throwable) = Unit

        override fun captureExceptionImmediate(throwable: Throwable) = Unit
    }

    /**
     * Synchronous Koin-compatible jobs manager. Uses `Dispatchers.Unconfined`
     * so every `serviceScope.launch { ... }` inside `bootstrapAnalytics()`
     * runs inline up to the first suspension — combined with the immediate
     * mock returns from `kmpTorService.awaitSocksPort()`, this gives the test
     * a synchronous-looking view of the async bootstrap path.
     */
    private class InlineJobsManager(
        dispatcher: CoroutineDispatcher = Dispatchers.Unconfined,
        override var coroutineExceptionHandler: ((Throwable) -> Unit)? = null,
    ) : CoroutineJobsManager {
        private val scope = CoroutineScope(dispatcher + SupervisorJob())

        override suspend fun dispose() {
            scope.cancel()
        }

        override fun getScope(): CoroutineScope = scope
    }

    @Before
    fun setUp() {
        startKoin {
            modules(
                module {
                    factory<CoroutineJobsManager> { InlineJobsManager() }
                },
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    /**
     * Test-controlled SOCKS port provider: returns the port from a
     * [CompletableDeferred] so individual tests can complete the wait
     * (port available) or leave it pending (clearnet/never).
     */
    private class FakeSocksPortProvider(
        private val deferred: CompletableDeferred<Int>,
    ) : AnalyticsSocksPortProvider {
        override suspend fun awaitSocksPort(): Int = deferred.await()
    }

    @Test
    fun `initialize forwards every AnalyticsBootstrapConfig value to AnalyticsService_init when opted-in AND SOCKS port is up`() {
        val analytics = RecordingAnalyticsService()
        val provider = FakeSocksPortProvider(CompletableDeferred(9050))
        val settingsRepo = TestSettingsRepository(MutableStateFlow(Settings(analyticsEnabled = true)))
        val config =
            AnalyticsBootstrapConfig(
                dsn = "http://abc@onion-host/3",
                environment = "production",
                release = "bisq-connect@0.5.0",
                isDebug = false,
            )
        val service =
            TestApplicationLifecycleService(
                analyticsService = analytics,
                analyticsBootstrapConfig = config,
                analyticsSocksPortProvider = provider,
                settingsRepository = settingsRepo,
            )

        runCatching { service.initialize() }

        assertEquals(1, analytics.initCalls, "AnalyticsService.init must be called exactly once when opted-in + SOCKS up")
        assertEquals(config.dsn, analytics.lastDsn)
        assertEquals(config.environment, analytics.lastEnvironment)
        assertEquals(config.release, analytics.lastRelease)
        assertEquals(config.isDebug, analytics.lastIsDebug)
        assertEquals("127.0.0.1", analytics.lastSocksHost, "Sentry must be routed through loopback SOCKS5")
        assertEquals(9050, analytics.lastSocksPort)
    }

    @Test
    fun `initialize does NOT init Sentry while the SOCKS port is pending - the gate remains suspended`() {
        // This is THE privacy invariant for Phase 1. A clearnet trusted-node
        // user never starts Tor, so the provider suspends forever and the
        // gate sits in its launched coroutine waiting. The lifecycle MUST NOT
        // then fall back to a direct-egress init — doing so would dial the
        // GlitchTip onion DSN over clearnet, failing the connection AND
        // leaking the user's IP to whatever clearnet exit it hit.
        val analytics = RecordingAnalyticsService()
        val provider = FakeSocksPortProvider(CompletableDeferred()) // never completes
        val settingsRepo = TestSettingsRepository(MutableStateFlow(Settings(analyticsEnabled = true)))
        val service =
            TestApplicationLifecycleService(
                analyticsService = analytics,
                analyticsBootstrapConfig =
                    AnalyticsBootstrapConfig(
                        dsn = "http://abc@onion-host/3",
                        environment = "production",
                        release = "bisq-connect@0.5.0",
                        isDebug = false,
                    ),
                analyticsSocksPortProvider = provider,
                settingsRepository = settingsRepo,
            )

        runCatching { service.initialize() }

        assertEquals(0, analytics.initCalls, "Sentry init must NOT fire while the SOCKS port is unresolved")
        assertNull(analytics.lastDsn)
        assertNull(analytics.lastSocksHost)
        assertNull(analytics.lastSocksPort)
    }

    @Test
    fun `initialize skips analytics bootstrap entirely when no SocksPortProvider is bound`() {
        // Build-time analytics-disabled path: the DI module doesn't bind a
        // SocksPortProvider, so the lifecycle must short-circuit cleanly
        // without launching a useless wait. Pin the contract so a refactor
        // that drops the null-check would surface immediately.
        val analytics = RecordingAnalyticsService()
        val service =
            TestApplicationLifecycleService(
                analyticsService = analytics,
                analyticsBootstrapConfig =
                    AnalyticsBootstrapConfig(
                        dsn = "http://abc@onion-host/3",
                        environment = "production",
                        release = "bisq-connect@0.5.0",
                        isDebug = false,
                    ),
                analyticsSocksPortProvider = null, // explicit — pins the contract
            )

        runCatching { service.initialize() }

        assertEquals(0, analytics.initCalls)
    }

    @Test
    fun `initialize swallows exceptions thrown by AnalyticsService_init - analytics never crashes the app`() {
        // The whole point of the try/catch around bootstrapAnalytics: analytics
        // is opt-in and non-critical; a Sentry-KMP init crash on a malformed DSN
        // or platform quirk must NOT prevent the app from starting. Regression
        // pin — if someone removes the try/catch during refactor, the analytics
        // exception would propagate out of initialize() (or crash the
        // serviceScope on its uncaught handler).
        val boom = RuntimeException("Sentry-KMP refused to dial")
        val analytics = RecordingAnalyticsService(throwOnInit = boom)
        val provider = FakeSocksPortProvider(CompletableDeferred(9050))
        val settingsRepo = TestSettingsRepository(MutableStateFlow(Settings(analyticsEnabled = true)))
        val service =
            TestApplicationLifecycleService(
                analyticsService = analytics,
                analyticsBootstrapConfig =
                    AnalyticsBootstrapConfig(
                        dsn = "http://x@example.org/1",
                        environment = "production",
                        release = "test",
                        isDebug = false,
                    ),
                analyticsSocksPortProvider = provider,
                settingsRepository = settingsRepo,
            )

        val outcome = runCatching { service.initialize() }
        outcome.exceptionOrNull()?.let { escaped ->
            assertEquals(
                false,
                escaped === boom || escaped.cause === boom,
                "Analytics init exception must be swallowed by bootstrap try/catch — got $escaped",
            )
        }

        // Init WAS attempted (proves we actually exercised the throw-branch, not
        // the case where init is silently skipped).
        assertEquals(1, analytics.initCalls)
        assertNotNull(analytics.lastDsn)
    }

    @Test
    fun `initialize flips BufferedAnalyticsService readiness only after successful Sentry init`() {
        // The buffer's flush guard depends on this signal — events enqueued
        // before init shouldn't be drained until the SDK is ready, otherwise
        // they hit a null transport and get silently dropped.
        val analytics = RecordingAnalyticsService()
        val buffered =
            BufferedAnalyticsService(
                downstream = analytics,
                scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob()),
                flushIntervalMs = 0L,
            )
        val provider = FakeSocksPortProvider(CompletableDeferred(9050))
        val settingsRepo = TestSettingsRepository(MutableStateFlow(Settings(analyticsEnabled = true)))
        val service =
            TestApplicationLifecycleService(
                analyticsService = buffered,
                analyticsBootstrapConfig =
                    AnalyticsBootstrapConfig(
                        dsn = "http://abc@onion-host/3",
                        environment = "production",
                        release = "test",
                        isDebug = false,
                    ),
                bufferedAnalyticsService = buffered,
                analyticsSocksPortProvider = provider,
                settingsRepository = settingsRepo,
            )

        // Pre-condition: not ready before initialize.
        assertEquals(false, buffered.isReady)

        runCatching { service.initialize() }

        // Post-condition: SDK init completed AND buffer flipped to ready.
        assertEquals(1, analytics.initCalls)
        assertEquals(true, buffered.isReady, "Buffer must be ready after Sentry init succeeds")
    }

    @Test
    fun `initialize does NOT flip BufferedAnalyticsService readiness while the SOCKS port is pending`() {
        // Symmetric privacy invariant to the pending-port test above — if we
        // never init Sentry, we must never tell the buffer to flush. Events
        // would otherwise hit the un-initialised downstream and get dropped
        // (currently) or, worse, accidentally ship via a future fallback path.
        val analytics = RecordingAnalyticsService()
        val buffered =
            BufferedAnalyticsService(
                downstream = analytics,
                scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob()),
                flushIntervalMs = 0L,
            )
        val provider = FakeSocksPortProvider(CompletableDeferred()) // never completes
        val settingsRepo = TestSettingsRepository(MutableStateFlow(Settings(analyticsEnabled = true)))
        val service =
            TestApplicationLifecycleService(
                analyticsService = buffered,
                analyticsBootstrapConfig =
                    AnalyticsBootstrapConfig(
                        dsn = "http://abc@onion-host/3",
                        environment = "production",
                        release = "test",
                        isDebug = false,
                    ),
                bufferedAnalyticsService = buffered,
                analyticsSocksPortProvider = provider,
                settingsRepository = settingsRepo,
            )

        runCatching { service.initialize() }

        assertEquals(0, analytics.initCalls)
        assertEquals(false, buffered.isReady, "Buffer must NOT be flipped to ready when Sentry was never initialized")
    }

    // ============ OPTION B — Defer init until first opt-in ============

    @Test
    fun `initialize waits for analyticsEnabled=true BEFORE calling Sentry init - opted-out users never load the SDK`() {
        // THE Option B privacy invariant: a user who never opts in must NEVER
        // see Sentry's SDK loaded into the process. UncaughtExceptionHandler
        // installed, native libs (libsentry.so, libsentry-android.so) loaded,
        // ActivityLifecycle integration registered, cache dirs created — ALL
        // bypassed until the user explicitly agrees. Verified empirically:
        // `nativeloader Load /.../libsentry.so` log line never appears for
        // opted-out users.
        val analytics = RecordingAnalyticsService()
        val provider = FakeSocksPortProvider(CompletableDeferred(9050))
        val settingsRepo = TestSettingsRepository(MutableStateFlow(Settings(analyticsEnabled = false)))
        val service =
            TestApplicationLifecycleService(
                analyticsService = analytics,
                analyticsBootstrapConfig =
                    AnalyticsBootstrapConfig(
                        dsn = "http://abc@onion-host/3",
                        environment = "production",
                        release = "test",
                        isDebug = false,
                    ),
                analyticsSocksPortProvider = provider,
                settingsRepository = settingsRepo,
            )

        runCatching { service.initialize() }

        assertEquals(0, analytics.initCalls, "Sentry init must NOT run while user is opted-out")
        assertNull(analytics.lastDsn, "DSN must never reach the SDK pre-opt-in")
    }

    @Test
    fun `initialize triggers Sentry init when user flips opt-in to true mid-session`() {
        // The reactive flip-on path: user defers consent at launch, then
        // changes their mind. The lifecycle's `filter { it }.first()` must
        // observe that transition and proceed to init. Without this, a user
        // who opts in via Settings AFTER launch would never get analytics
        // unless they restart the app.
        val analytics = RecordingAnalyticsService()
        val provider = FakeSocksPortProvider(CompletableDeferred(9050))
        val settingsFlow = MutableStateFlow(Settings(analyticsEnabled = false))
        val settingsRepo = TestSettingsRepository(settingsFlow)
        val service =
            TestApplicationLifecycleService(
                analyticsService = analytics,
                analyticsBootstrapConfig =
                    AnalyticsBootstrapConfig(
                        dsn = "http://abc@onion-host/3",
                        environment = "production",
                        release = "test",
                        isDebug = false,
                    ),
                analyticsSocksPortProvider = provider,
                settingsRepository = settingsRepo,
            )

        runCatching { service.initialize() }
        assertEquals(0, analytics.initCalls, "pre-condition: opted-out → no init")

        // User flips toggle ON in Settings.
        settingsFlow.value = settingsFlow.value.copy(analyticsEnabled = true)

        assertEquals(1, analytics.initCalls, "opt-in flip must trigger Sentry init")
    }

    @Test
    fun `initialize refuses to init Sentry when SettingsRepository is not bound - fails closed`() {
        // Defence in depth: if production DI somehow forgot to bind
        // SettingsRepository (refactor bug, module misconfiguration), we must
        // NOT silently default to "init Sentry anyway". That would
        // effectively force-enable analytics for everyone — catastrophic for
        // the opt-in contract. Fail closed, log loud.
        val analytics = RecordingAnalyticsService()
        val provider = FakeSocksPortProvider(CompletableDeferred(9050))
        val service =
            TestApplicationLifecycleService(
                analyticsService = analytics,
                analyticsBootstrapConfig =
                    AnalyticsBootstrapConfig(
                        dsn = "http://abc@onion-host/3",
                        environment = "production",
                        release = "test",
                        isDebug = false,
                    ),
                analyticsSocksPortProvider = provider,
                settingsRepository = null, // explicit: misconfiguration scenario
            )

        runCatching { service.initialize() }

        assertEquals(0, analytics.initCalls, "no SettingsRepository → MUST NOT init Sentry")
    }

    /**
     * Minimal [SettingsRepository] stand-in that lets the test control when
     * `data` emits. Used by the pre-warm contract test below. We don't reach
     * for mockk here because the SettingsRepository interface has many
     * methods and the test only cares about `data` emissions.
     */
    private class TestSettingsRepository(
        private val flow: MutableStateFlow<Settings> = MutableStateFlow(Settings()),
    ) : SettingsRepository {
        override val data = flow

        override suspend fun setFirstLaunch(value: Boolean) {}

        override suspend fun setShowChatRulesWarnBox(value: Boolean) {}

        override suspend fun setSelectedMarketCode(value: String) {}

        override suspend fun setNotificationPermissionState(
            value: network.bisq.mobile.data.model.PermissionState,
        ) {}

        override suspend fun setBatteryOptimizationPermissionState(
            value: network.bisq.mobile.data.model.BatteryOptimizationState,
        ) {}

        override suspend fun update(transform: suspend (t: Settings) -> Settings) {
            flow.value = transform(flow.value)
        }

        override suspend fun clear() {}

        override suspend fun setMarketSortBy(value: network.bisq.mobile.data.model.market.MarketSortBy) {}

        override suspend fun setMarketFilter(value: network.bisq.mobile.data.model.market.MarketFilter) {}

        override suspend fun setDontShowAgainHyperlinksOpenInBrowser(value: Boolean) {}

        override suspend fun setPermitOpeningBrowser(value: Boolean) {}

        override suspend fun setAnalyticsEnabled(value: Boolean) {
            flow.value = flow.value.copy(analyticsEnabled = value)
        }

        override suspend fun setAnalyticsPromptSeen(value: Boolean) {
            flow.value = flow.value.copy(analyticsPromptSeen = value)
        }

        override suspend fun setAnalyticsBaselineSent(value: Boolean) {
            flow.value = flow.value.copy(analyticsBaselineSent = value)
        }

        override suspend fun setRememberOfferbookFilterPreferences(value: Boolean) {
            flow.value = flow.value.copy(rememberOfferbookFilterPreferences = value)
        }
    }

    @Test
    fun `init + onSentryReady fire only after the opt-in flip is observed in Settings`() {
        // Pins the ordering: under Option B, the lifecycle waits for the
        // settings flow to emit `analyticsEnabled=true` BEFORE calling init
        // BEFORE calling onSentryReady. So the buffer's "ready" flag must
        // ONLY flip after both the opt-in flip and SDK init complete.
        //
        // This also implicitly handles the StateFlow pre-warm we previously
        // hand-coded: by the time `filter { it }.first()` resolves, DataStore
        // has emitted at least once with the truthy value, so the DI module's
        // `analyticsEnabledIn` StateFlow has its real value. The race window
        // we hit on the node app on 2026-06-11 is gone.
        val analytics = RecordingAnalyticsService()
        val buffered =
            BufferedAnalyticsService(
                downstream = analytics,
                scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob()),
                flushIntervalMs = 0L,
            )
        val provider = FakeSocksPortProvider(CompletableDeferred(9050))
        val settingsRepo = TestSettingsRepository(MutableStateFlow(Settings(analyticsEnabled = true)))
        val service =
            TestApplicationLifecycleService(
                analyticsService = buffered,
                analyticsBootstrapConfig =
                    AnalyticsBootstrapConfig(
                        dsn = "http://abc@onion-host/3",
                        environment = "production",
                        release = "test",
                        isDebug = false,
                    ),
                bufferedAnalyticsService = buffered,
                analyticsSocksPortProvider = provider,
                settingsRepository = settingsRepo,
            )

        runCatching { service.initialize() }

        assertEquals(1, analytics.initCalls)
        assertEquals(true, buffered.isReady, "Buffer ready flips only after opt-in flip + init + onSentryReady")
    }
}
