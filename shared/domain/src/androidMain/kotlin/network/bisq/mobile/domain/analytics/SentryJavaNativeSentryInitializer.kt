package network.bisq.mobile.domain.analytics

import io.sentry.SentryEvent
import io.sentry.SentryOptions
import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.protocol.Contexts
import io.sentry.protocol.Device
import io.sentry.protocol.OperatingSystem
import network.bisq.mobile.domain.utils.Logging
import java.net.Proxy

/**
 * Android implementation of [NativeSentryInitializer]. Two responsibilities:
 *
 *  1. Configure Sentry-Java's `SentryAndroidOptions` to route all transport
 *     through the kmp-tor / bisq2 SOCKS5 proxy. See [setupTransport].
 *  2. Hold the privacy line. Sentry-Android's defaults are loud — auto
 *     activity-lifecycle breadcrumbs, network breadcrumbs (which would leak
 *     the user's trusted-node onion URL in every event), auto session
 *     tracking (which generates a session/user id even with sendDefaultPii
 *     off), attached threads, etc. We disable everything we don't need AND
 *     scrub anything that leaks through in [beforeSend]. See [setupPrivacy].
 *
 * Disabled integrations rationale:
 *  - **enableAutoSessionTracking**: generates per-install user IDs that violate
 *    the "no user/device IDs" contract from issue #525.
 *  - **enableNetworkEventBreadcrumbs**: silently attaches every HTTP request
 *    the app makes to subsequent events. We talk to `*.onion` trusted nodes —
 *    this would leak the user's chosen trusted node onion URL in every Sentry
 *    event. Critical to disable.
 *  - **enableActivityLifecycleBreadcrumbs / enableAppLifecycleBreadcrumbs /
 *    enableSystemEventBreadcrumbs / enableAppComponentBreadcrumbs**: noise +
 *    auto context attached to every event (activity names, configuration
 *    changes, battery / low-memory broadcasts, etc.).
 *  - **enableUserInteractionBreadcrumbs / enableUserInteractionTracing**:
 *    captures touch events on resolved view IDs — could leak screen names /
 *    UI hierarchy state that aren't in our sealed AnalyticsEvent contract.
 *  - **anrEnabled / setReportHistoricalAnrs**: ANR tracking is a CPU sampler
 *    that attaches full thread stacks to events. Useful for performance, off
 *    by our explicit ask + minimal payload contract.
 *  - **enableFramesTracking / enableAutoActivityLifecycleTracing**: pulls in
 *    performance traces we never look at.
 *  - **enableNdk**: native crash handler. We're a Compose-only app, no JNI
 *    surface that warrants the binary size + symbols upload complexity.
 *  - **attachStacktrace**: auto-attaches the current thread's stack to
 *    *non-exception* events (i.e. our `track()` calls). The dashboard-opened
 *    event was carrying 60+ frame stacks including K/N runtime internals.
 *  - **attachThreads**: same idea — all thread stacks dumped onto events.
 *  - **attachScreenshot / attachViewHierarchy**: never want UI snapshots.
 *  - **enableRootCheck**: fingerprints rooted devices into the event tags.
 *
 * Kept:
 *  - **enableUncaughtExceptionHandler** (default true): the whole point of
 *    error reporting. Stack frames on exception events ARE useful; the
 *    [beforeSend] scrubber leaves them alone.
 *  - **enableDeduplication** (default true): silently drops duplicate event
 *    sends — saves bandwidth on the slow Tor transport.
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
            // Identity / build-time config
            options.dsn = dsn
            options.environment = environment
            options.release = release
            options.isDebug = isDebug

            setupPrivacy(options, redactor)
            setupTransport(options, socksProxyHost, socksProxyPort)
        }
        log.d {
            if (socksProxyHost != null) {
                "Sentry-Android initialized (SOCKS5 $socksProxyHost:$socksProxyPort)"
            } else {
                "Sentry-Android initialized (direct — no proxy)"
            }
        }
    }

    /**
     * Visible to androidUnitTest so the privacy contract on `SentryOptions`
     * can be pinned without spinning up the global SDK. The production
     * callsite ([init]) is the only intended caller.
     */
    internal fun setupPrivacy(
        options: io.sentry.android.core.SentryAndroidOptions,
        redactor: AnalyticsRedactor,
    ) {
        // Defaults that block whole categories of auto-attached data.
        options.isSendDefaultPii = false

        // Cross-platform options (live on SentryOptions, not just Android).
        options.isEnableAutoSessionTracking = false
        options.isEnableUserInteractionTracing = false
        options.isEnableUserInteractionBreadcrumbs = false
        options.isAttachStacktrace = false
        options.isAttachThreads = false
        options.isAttachServerName = false

        // Android-specific integration killswitches.
        options.isEnableActivityLifecycleBreadcrumbs = false
        options.isEnableAppLifecycleBreadcrumbs = false
        options.isEnableSystemEventBreadcrumbs = false
        options.isEnableAppComponentBreadcrumbs = false
        options.isEnableNetworkEventBreadcrumbs = false
        options.isEnableAutoActivityLifecycleTracing = false
        options.isEnableFramesTracking = false
        options.isAttachScreenshot = false
        options.isAttachViewHierarchy = false
        options.isEnableRootCheck = false
        options.isEnableNdk = false
        options.isAnrEnabled = false

        // beforeSend is defence in depth on top of the killswitches above.
        // Each killswitch can be a no-op on a future Sentry version (e.g. a
        // new integration that attaches similar data) — the scrubber is the
        // last gate before the wire.
        options.beforeSend =
            SentryOptions.BeforeSendCallback { event, _ ->
                applyMinimalPayloadContract(event, redactor)
                event
            }
    }

    /**
     * Strips every event down to the privacy contract:
     *  - `message` + `level` + `timestamp` + `release` + `environment`
     *  - `contexts.os.{name, version}` (useful Android version split)
     *  - `contexts.device.{bootTime, batteryLevel, charging, freeMemory,
     *    freeStorage, lowMemory}` — volatile diagnostic info only. The
     *    static counterparts (`memorySize`, `storageSize`) are deliberately
     *    NOT kept; they fingerprint the device tier.
     *
     * Everything else — `user`, `breadcrumbs`, `threads`, `debugMeta`,
     * `tags`, `extras`, `contexts.app`, `contexts.runtime`, every other
     * device field — is cleared. See PR description for the wire example.
     *
     * Visible to androidUnitTest so the scrubber can be exercised directly
     * against a synthetic [SentryEvent] without spinning up the SDK. Kept
     * `internal` because the production callsite (`setupPrivacy`) is the
     * only intended caller.
     */
    internal fun applyMinimalPayloadContract(
        event: SentryEvent,
        redactor: AnalyticsRedactor,
    ) {
        // Free text — run the redactor as defence in depth on top of the
        // sealed AnalyticsEvent API. Cheap; survives a sealed-class refactor
        // that accidentally widens.
        event.message?.let { msg ->
            msg.message?.let { msg.message = redactor.redact(it) }
            msg.formatted?.let { msg.formatted = redactor.redact(it) }
        }
        event.exceptions?.forEach { ex ->
            ex.value?.let { ex.value = redactor.redact(it) }
        }

        // Things Sentry auto-fills that violate the "no user/device IDs +
        // no user-state correlation" contract.
        event.user = null
        event.breadcrumbs = null
        event.threads = null
        event.debugMeta = null
        event.serverName = null
        event.dist = null

        // Tag/extra maps are auto-grown by some integrations — clear.
        event.tags?.clear()
        event.extras?.clear()

        scrubContexts(event.contexts)
    }

    private fun scrubContexts(contexts: Contexts) {
        // Replace OS + Device with stripped copies (keep narrow allowlists).
        contexts.operatingSystem?.let { contexts.setOperatingSystem(strippedOs(it)) }
        contexts.device?.let { contexts.setDevice(strippedDevice(it)) }

        // Drop every OTHER context key (app, browser, runtime, gpu, response,
        // trace, profile, feedback, spring, flags, …). We iterate + remove
        // rather than calling the typed setters because Sentry-Java's setters
        // are @NotNull-annotated (no `setApp(null)`); `Contexts` is map-backed
        // so removal by key works for every type without enumeration. This
        // also future-proofs against new context types Sentry adds — they get
        // dropped automatically until we explicitly allowlist them here.
        //
        // `Contexts.keys()` returns a java.util.Enumeration (legacy Java API);
        // snapshot it to a list before mutating, otherwise removal during
        // iteration would throw ConcurrentModificationException.
        val allKeys = mutableListOf<String>()
        val keysEnum = contexts.keys()
        while (keysEnum.hasMoreElements()) {
            allKeys += keysEnum.nextElement()
        }
        allKeys.filter { it !in CONTEXT_KEEP_KEYS }.forEach { contexts.remove(it) }
    }

    private fun strippedOs(source: OperatingSystem): OperatingSystem =
        OperatingSystem().apply {
            name = source.name
            version = source.version
        }

    private fun strippedDevice(source: Device): Device =
        Device().apply {
            // Volatile diagnostics — useful for triage, no fingerprint value.
            // The static counterparts (memorySize = total RAM, storageSize =
            // total disk capacity) are deliberately NOT kept: they fingerprint
            // the device tier (e.g. 256GB vs 128GB iPhone).
            bootTime = source.bootTime // "did the issue start after a reboot?"
            batteryLevel = source.batteryLevel // low-battery throttling correlation
            // `charging` and `lowMemory` use Java-style `isXxx()/setXxx()` —
            // Kotlin synthetic property names them `isCharging` / `isLowMemory`.
            isCharging = source.isCharging // "happened while plugged in?" correlation
            freeMemory = source.freeMemory // OOM correlation
            freeStorage = source.freeStorage // out-of-disk correlation
            isLowMemory = source.isLowMemory // OS-level memory-pressure flag (Android)
            // EVERY other field (id, manufacturer, brand, family, model,
            // modelId, archs, memorySize, storageSize, screen*, locale,
            // timezone, processor*, …) is intentionally left null. If you
            // need to add one, justify it against the "By design, not by
            // promise" wiki contract.
        }

    /** Visible to androidUnitTest — see [setupPrivacy] kdoc for the rationale. */
    internal fun setupTransport(
        options: SentryOptions,
        socksProxyHost: String?,
        socksProxyPort: Int?,
    ) {
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

    private companion object {
        // Context map keys we keep. Anything not in this set gets removed by
        // [scrubContexts]. The string values are Sentry-Java's well-known
        // context type keys — `OperatingSystem.TYPE` and `Device.TYPE` — but
        // hardcoded as constants here to keep the privacy contract readable
        // at a glance without chasing through the SDK classes.
        val CONTEXT_KEEP_KEYS = setOf("os", "device")
    }
}
