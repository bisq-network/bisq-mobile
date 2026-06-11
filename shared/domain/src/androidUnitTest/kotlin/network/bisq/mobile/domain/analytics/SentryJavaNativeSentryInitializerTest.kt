package network.bisq.mobile.domain.analytics

import io.sentry.SentryEvent
import io.sentry.SentryOptions
import io.sentry.android.core.SentryAndroidOptions
import io.sentry.protocol.Message
import io.sentry.protocol.SentryException
import java.net.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Pins the [SentryJavaNativeSentryInitializer] privacy contract by exercising
 * the same `setupPrivacy` + `setupTransport` mutations the production code
 * applies to `SentryAndroidOptions` — without actually calling into the SDK's
 * global `Sentry.initWithPlatformOptions` (which holds process-wide state
 * across tests and would try to start the real transport).
 *
 * The production methods are `internal` so this test calls them DIRECTLY: the
 * SDK boot is sidestepped but every options setter, integration killswitch,
 * and `beforeSend` scrub path is real production code. A regression at any
 * point — a dropped killswitch, a weakened default, an unredacted field —
 * fails an assertion below.
 */
class SentryJavaNativeSentryInitializerTest {
    /**
     * Calls the real production [SentryJavaNativeSentryInitializer.setupPrivacy]
     * and [SentryJavaNativeSentryInitializer.setupTransport] methods so the
     * assertions below pin the actual production behaviour (not a test mirror
     * that could drift). DSN/env/release/isDebug mirror what the production
     * `init` lambda sets — they live outside `setupPrivacy` because they're
     * the boring identity bits that don't need explicit coverage.
     */
    private fun applyProductionOptionsContract(
        options: SentryAndroidOptions,
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

        val initializer = SentryJavaNativeSentryInitializer()
        initializer.setupPrivacy(options, redactor)
        initializer.setupTransport(options, socksProxyHost, socksProxyPort)
    }

    @Test
    fun `init configures DSN environment release and isDebug on the platform options`() {
        val options = SentryAndroidOptions()
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
        val options = SentryAndroidOptions()
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
        val options = SentryAndroidOptions()
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
        val options = SentryAndroidOptions()
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
        val options = SentryAndroidOptions()
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
        // The injected onion is a real Tor v3 (DuckDuckGo's, public, 56 base32
        // chars + .onion). Asserting against the EXACT injected string forces
        // the test to verify actual redaction — using any other string would
        // pass trivially regardless of whether beforeSend ran.
        assertEquals(
            false,
            scrubbedValue.contains("2gzyxa5ihm7nsggfxnu52rck2vv4rvmdlkiu3zzui5du4xyclen53wid.onion"),
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
        val options = SentryAndroidOptions()
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
        val options = SentryAndroidOptions()
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
        val hostOnlyOptions = SentryAndroidOptions()
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

        val portOnlyOptions = SentryAndroidOptions()
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
        val options = SentryAndroidOptions()
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

    // ============ MINIMAL PAYLOAD CONTRACT (privacy scrub) ============
    //
    // These tests exercise `applyMinimalPayloadContract` against synthetic
    // events populated with everything Sentry-Android auto-attaches by default.
    // After the scrub, ONLY the privacy-contract-approved fields survive.
    // See class kdoc for the contract.

    @Test
    fun `scrub clears user - no user id ever leaves the device`() {
        // `user.id` is auto-populated by Sentry's session tracking even with
        // sendDefaultPii=false. Disabling the integration is the primary lock;
        // this scrub is defence in depth — a refactor that flips
        // enableAutoSessionTracking back on would still NOT leak the id.
        val event =
            SentryEvent().apply {
                user =
                    io.sentry.protocol
                        .User()
                        .apply { id = "7B9A3BC1-01C7-4AF8-A932" }
            }
        SentryJavaNativeSentryInitializer().applyMinimalPayloadContract(event, AnalyticsRedactor())
        assertNull(event.user, "user MUST be cleared — `user.id` violates the no-user-ids contract")
    }

    @Test
    fun `scrub clears breadcrumbs - no trusted-node onion leakage`() {
        // The CRITICAL scrub: Sentry-Android's network event breadcrumbs would
        // attach every HTTP request to subsequent events, including the URL.
        // We talk to *.onion trusted nodes — without this clear, every Sentry
        // event would ship the user's chosen trusted node onion URL.
        val event =
            SentryEvent().apply {
                breadcrumbs =
                    mutableListOf(
                        io.sentry.Breadcrumb().apply {
                            category = "http"
                            setData("url", "http://b4teju6q...trusted-node.onion/api/v1/session")
                        },
                    )
            }
        SentryJavaNativeSentryInitializer().applyMinimalPayloadContract(event, AnalyticsRedactor())
        assertNull(event.breadcrumbs, "breadcrumbs MUST be cleared to avoid trusted-node URL leakage")
    }

    @Test
    fun `scrub clears auto-attached threads - track events do not ship thread stacks`() {
        // Sentry-Android attaches every thread stack to non-exception events
        // when attachThreads=true. The dashboard-opened test event was carrying
        // 60+ frames including K/N runtime internals and absolute file paths.
        // Disabling attachThreads is the primary lock; this scrub is the catch.
        //
        // Note: Sentry's SentryEvent wraps the threads list in `SentryValues`
        // whose constructor normalises null to an empty ArrayList — so
        // setThreads(null) results in event.threads == empty list, NOT null.
        // Empty list is functionally equivalent for the wire (no thread data
        // attached), which is what we care about for the privacy contract.
        val event =
            SentryEvent().apply {
                threads =
                    mutableListOf(
                        io.sentry.protocol
                            .SentryThread()
                            .apply { name = "main" },
                    )
            }
        SentryJavaNativeSentryInitializer().applyMinimalPayloadContract(event, AnalyticsRedactor())
        assertEquals(
            true,
            event.threads.isNullOrEmpty(),
            "auto-attached threads MUST be cleared on track events (null or empty list — got ${event.threads})",
        )
    }

    @Test
    fun `scrub clears debugMeta - no user home paths in symbol file references`() {
        // debugMeta carries absolute paths to debug symbol files — on a dev
        // build these point inside `/Users/<name>/Library/Developer/...` which
        // de-anonymises the developer/user. Real users get sandbox paths, but
        // those still fingerprint the device.
        val event = SentryEvent().apply { debugMeta = io.sentry.protocol.DebugMeta() }
        SentryJavaNativeSentryInitializer().applyMinimalPayloadContract(event, AnalyticsRedactor())
        assertNull(event.debugMeta, "debugMeta MUST be cleared (paths fingerprint device/user)")
    }

    @Test
    fun `scrub keeps OS name and version - useful for triage`() {
        val event =
            SentryEvent().apply {
                contexts.setOperatingSystem(
                    io.sentry.protocol.OperatingSystem().apply {
                        name = "Android"
                        version = "15"
                        build = "AE3A.240806.043" // fingerprint-adjacent — should be dropped
                        kernelVersion = "6.6.30-android15" // should be dropped
                    },
                )
            }
        SentryJavaNativeSentryInitializer().applyMinimalPayloadContract(event, AnalyticsRedactor())

        val os = assertNotNull(event.contexts.operatingSystem)
        assertEquals("Android", os.name)
        assertEquals("15", os.version)
        assertNull(os.build, "OS build hash is fingerprint-adjacent — must be stripped")
        assertNull(os.kernelVersion, "kernel version is fingerprint-adjacent — must be stripped")
    }

    @Test
    fun `scrub keeps device volatile diagnostic fields and drops the fingerprint ones`() {
        // Keep-list per the privacy contract: volatile fields only (rotate
        // with reboot / charge / OS pressure), useful for triage but no
        // device fingerprint. Crucially, the STATIC counterparts of the
        // memory/storage pair (memorySize, storageSize) are NOT kept — those
        // fingerprint the device tier (e.g. 256GB iPhone).
        val event =
            SentryEvent().apply {
                contexts.setDevice(
                    io.sentry.protocol.Device().apply {
                        // Keep these — volatile diagnostics
                        bootTime = java.util.Date(1_700_000_000_000L)
                        batteryLevel = 72.5f
                        isCharging = false
                        freeMemory = 1_302_515_712L
                        freeStorage = 3_030_827_008L
                        isLowMemory = false
                        // Drop these — fingerprints
                        id = "f71bd5bdf6a54251bbb3acb286623025"
                        manufacturer = "Google"
                        brand = "google"
                        family = "sdk_gphone64_arm64"
                        model = "sdk_gphone64_arm64"
                        modelId = "AE3A.240806.043"
                        archs = arrayOf("arm64-v8a")
                        memorySize = 2_592_759_808L // total RAM = device tier fingerprint
                        storageSize = 6_228_115_456L // total disk = device tier fingerprint
                        timezone = java.util.TimeZone.getTimeZone("Asia/Kuala_Lumpur") // location leak
                        screenDensity = 2.75f
                        screenDpi = 440
                    },
                )
            }
        SentryJavaNativeSentryInitializer().applyMinimalPayloadContract(event, AnalyticsRedactor())

        val device = assertNotNull(event.contexts.device)
        // Kept (volatile diagnostics)
        assertEquals(java.util.Date(1_700_000_000_000L), device.bootTime)
        assertEquals(72.5f, device.batteryLevel)
        assertEquals(false, device.isCharging)
        assertEquals(1_302_515_712L, device.freeMemory)
        assertEquals(3_030_827_008L, device.freeStorage)
        assertEquals(false, device.isLowMemory)
        // Stripped (the load-bearing assertions for privacy)
        assertNull(device.id, "device.id is the FIRST contract violation we hit on iOS — must be stripped")
        assertNull(device.manufacturer)
        assertNull(device.brand)
        assertNull(device.family)
        assertNull(device.model)
        assertNull(device.modelId)
        assertNull(device.archs)
        assertNull(device.memorySize, "total RAM fingerprints the device model — strip")
        assertNull(device.storageSize, "total disk capacity fingerprints the device tier — strip")
        assertNull(device.timezone, "timezone leaks user region — strip")
        assertNull(device.screenDensity)
        assertNull(device.screenDpi)
    }

    @Test
    fun `scrub drops every context type other than os and device`() {
        // App context carries app_identifier, app_id, view_names, device_app_hash,
        // permissions — all device/user fingerprint. Drop wholesale. Same for
        // every other context Sentry-Java might attach.
        val event =
            SentryEvent().apply {
                contexts.setApp(
                    io.sentry.protocol
                        .App()
                        .apply { appName = "Bisq Easy" },
                )
                contexts.setBrowser(io.sentry.protocol.Browser())
                contexts.setRuntime(io.sentry.protocol.SentryRuntime())
                contexts.setGpu(io.sentry.protocol.Gpu())
                contexts.setOperatingSystem(
                    io.sentry.protocol
                        .OperatingSystem()
                        .apply { name = "Android" },
                )
                contexts.setDevice(
                    io.sentry.protocol
                        .Device()
                        .apply { batteryLevel = 50f },
                )
            }
        SentryJavaNativeSentryInitializer().applyMinimalPayloadContract(event, AnalyticsRedactor())

        assertNotNull(event.contexts.operatingSystem, "OS context is on the keep list")
        assertNotNull(event.contexts.device, "Device context is on the keep list")
        assertNull(event.contexts.app, "App context MUST be dropped (carries app_identifier, permissions, etc.)")
        assertNull(event.contexts.browser)
        assertNull(event.contexts.runtime)
        assertNull(event.contexts.gpu)
    }

    // ============ beforeSend OPT-IN GATE (Option B) ============

    @Test
    fun `beforeSend returns null when runtimeOptInProvider returns false - drops SDK auto-captures`() {
        // The load-bearing privacy invariant from Option B (issue #525):
        // when the user is opted out, the SDK's own pipelines (Uncaught-
        // ExceptionHandler crashes, ActivityLifecycle, etc.) ship envelopes
        // through beforeSend — NOT through SentryAnalyticsService.track().
        // Without this gate, an opted-out user whose app crashes would still
        // leak the crash report.
        val options = SentryAndroidOptions()
        SentryJavaNativeSentryInitializer().setupPrivacy(
            options = options,
            redactor = AnalyticsRedactor(),
            runtimeOptInProvider = { false }, // user opted OUT
        )
        val callback = assertNotNull(options.beforeSend)

        val event = SentryEvent()
        val result = callback.execute(event, io.sentry.Hint())

        assertNull(
            result,
            "beforeSend MUST return null when opted-out — Sentry drops the envelope at SDK level, no wire traffic",
        )
    }

    @Test
    fun `beforeSend forwards the event when runtimeOptInProvider returns true`() {
        // Symmetric to the opted-out test: when opted in, the gate is open
        // and beforeSend returns the event (after scrubbing).
        val options = SentryAndroidOptions()
        SentryJavaNativeSentryInitializer().setupPrivacy(
            options = options,
            redactor = AnalyticsRedactor(),
            runtimeOptInProvider = { true }, // user opted IN
        )
        val callback = assertNotNull(options.beforeSend)

        val event = SentryEvent()
        val result = callback.execute(event, io.sentry.Hint())

        assertEquals(event, result, "beforeSend MUST return the event when opted-in — SDK ships it")
    }

    @Test
    fun `beforeSend consults the provider on EVERY call - flipping opt-in mid-session takes effect immediately`() {
        // Captures-by-reference contract: the provider lambda is invoked fresh
        // on every event, not snapshotted at init time. This is what makes
        // toggle-off-after-init work — Sentry stays loaded but stops emitting
        // the instant the user flips the switch in Settings.
        var consented = true
        val options = SentryAndroidOptions()
        SentryJavaNativeSentryInitializer().setupPrivacy(
            options = options,
            redactor = AnalyticsRedactor(),
            runtimeOptInProvider = { consented },
        )
        val callback = assertNotNull(options.beforeSend)

        // First event: opted in → shipped.
        assertNotNull(callback.execute(SentryEvent(), io.sentry.Hint()))

        // User flips toggle OFF mid-session.
        consented = false
        assertNull(
            callback.execute(SentryEvent(), io.sentry.Hint()),
            "beforeSend must re-read the provider on every call, not cache",
        )

        // User flips back ON.
        consented = true
        assertNotNull(callback.execute(SentryEvent(), io.sentry.Hint()))
    }
}
