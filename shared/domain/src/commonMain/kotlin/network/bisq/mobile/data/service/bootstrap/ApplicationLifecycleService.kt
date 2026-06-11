package network.bisq.mobile.data.service.bootstrap

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import network.bisq.mobile.data.service.BaseService
import network.bisq.mobile.data.service.network.KmpTorService
import network.bisq.mobile.data.utils.getPlatformInfo
import network.bisq.mobile.domain.analytics.AnalyticsBootstrapConfig
import network.bisq.mobile.domain.analytics.AnalyticsService
import network.bisq.mobile.domain.analytics.AnalyticsSocksPortProvider
import network.bisq.mobile.domain.analytics.BufferedAnalyticsService
import network.bisq.mobile.domain.model.PlatformType
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.domain.utils.killProcess
import network.bisq.mobile.domain.utils.restartProcess

abstract class ApplicationLifecycleService(
    private val applicationBootstrapFacade: ApplicationBootstrapFacade,
    private val kmpTorService: KmpTorService,
    private val analyticsService: AnalyticsService,
    private val analyticsBootstrapConfig: AnalyticsBootstrapConfig,
    // Optional — always bound now in production DI; null only in test fixtures
    // that don't care about the readiness signal (see TestApplicationLifecycleService).
    private val bufferedAnalyticsService: BufferedAnalyticsService? = null,
    // Optional — always bound now in production DI (per-app: ClientApp wraps
    // KmpTorService, NodeApp polls bisq2's NetworkService). null only in test
    // fixtures; `bootstrapAnalytics()` then skips entirely.
    private val analyticsSocksPortProvider: AnalyticsSocksPortProvider? = null,
    // Optional — always bound in production DI. Pre-warmed in
    // [bootstrapAnalytics] before flipping onSentryReady so the DI's
    // hot StateFlow gate (`analyticsEnabledIn`) has its real value before
    // any track() call runs through it. Without this, a fast first-frame
    // dashboard view could race the StateFlow's initial emission and the
    // event would be silently dropped at the runtime gate. See PR #1473
    // discussion + diagnostic logs in [SentryAnalyticsService.isReadyToEmit].
    private val settingsRepository: SettingsRepository? = null,
) : BaseService() {
    private val isTerminating = atomic<Boolean>(false)

    /**
     * Marks the app as terminating if not already started, returns true if this is the first call.
     * Use this as a one-shot guard.
     */
    fun compareAndSetIsTerminating(
        expect: Boolean,
        update: Boolean,
    ): Boolean {
        // this function is to avoid adding atomicfu dependency to node app for now
        return isTerminating.compareAndSet(expect, update)
    }

    fun initialize() {
        // Defer analytics SDK init until the kmp-tor SOCKS port is up — Phase 1
        // contract is "Tor or nothing", so clearnet users get no analytics on
        // the wire (events still hit the BufferedAnalyticsService buffer and
        // are evicted by the bounded-buffer policy without ever leaving the
        // device). The service implementation is itself gated (build-time +
        // runtime) so a build with analytics disabled never enters this path.
        log.i { "Maybe initialize analytics" }
        bootstrapAnalytics()

        log.i { "Initialize core services and Tor" }
        serviceScope.launch {
            try {
                activateServiceFacades()
            } catch (e: Exception) {
                onUnrecoverableError(e)
            }
        }
    }

    private fun bootstrapAnalytics() {
        // Skip entirely when analytics is disabled at build time. With the
        // build-time gate off, the [AnalyticsSocksPortProvider] binding is
        // absent and the runtime gate on [SentryAnalyticsService] denies
        // emission anyway — but bailing out here keeps the log noise honest
        // and avoids spinning a coroutine that has nothing to do.
        val provider = analyticsSocksPortProvider
        if (provider == null) {
            log.d { "Analytics: SocksPortProvider not bound — analytics disabled" }
            return
        }
        // Launch instead of running inline so the lifecycle's initialize() does
        // not block on Tor bootstrap (which can take 30–60s on first run, much
        // longer for users who pair their onion trusted node well after launch).
        // All service facade activation proceeds in parallel — the SDK is not
        // on the critical path.
        serviceScope.launch {
            try {
                // (1) Wait until the user opts in. NEVER load Sentry's SDK
                //     into the process until the user explicitly agrees —
                //     prevents the auto-installed integrations (Uncaught-
                //     ExceptionHandler, ActivityLifecycle, Watchdog, etc.)
                //     from being registered at all. If the user never opts
                //     in, this suspends forever and Sentry is never loaded.
                //
                //     Note: SettingsRepository is typed nullable for test
                //     fixtures. Without it we can't honour the opt-in
                //     contract — fail loudly via early return rather than
                //     silently behaving as opted-in.
                val settingsRepo = settingsRepository
                if (settingsRepo == null) {
                    log.w { "Analytics: SettingsRepository not bound — refusing to init Sentry (opt-in cannot be verified)" }
                    return@launch
                }
                log.i { "Analytics: waiting for the user opt-in toggle" }
                settingsRepo.data
                    .map { it.analyticsEnabled }
                    .filter { it }
                    .first()
                log.i { "Analytics: opt-in confirmed; waiting for Tor SOCKS port" }
                // (2) Wait for Tor. No timeout — if Tor never comes up
                //     (clearnet trusted node on the client, or a node app
                //     that fails Tor bootstrap), this suspends forever. The
                //     bounded BufferedAnalyticsService FIFO eviction
                //     guarantees memory safety; the privacy contract
                //     guarantees we never leak via clearnet because Sentry
                //     is simply never initialized.
                val socksPort = provider.awaitSocksPort()
                analyticsService.init(
                    dsn = analyticsBootstrapConfig.dsn,
                    environment = analyticsBootstrapConfig.environment,
                    release = analyticsBootstrapConfig.release,
                    isDebug = analyticsBootstrapConfig.isDebug,
                    socksProxyHost = LOOPBACK_SOCKS_HOST,
                    socksProxyPort = socksPort,
                )
                // Flip the buffer's readiness flag and trigger an immediate
                // drain of any events that fired between presenter wiring and
                // now. Null only in test fixtures. Safe to call multiple
                // times — the method is idempotent (atomic CAS internally).
                //
                // No pre-warm needed here: the `filter { it }.first()` above
                // already proved DataStore has emitted at least one value
                // where `analyticsEnabled == true`, so the DI module's hot
                // StateFlow gate reflects reality before any track call.
                bufferedAnalyticsService?.onSentryReady()
                log.i { "Analytics: Sentry initialized over Tor (SOCKS5 $LOOPBACK_SOCKS_HOST:$socksPort)" }
            } catch (e: Exception) {
                // Never let analytics setup take the app down — if Sentry-KMP's
                // init throws on a malformed DSN or platform quirk, log and
                // proceed. We intentionally do NOT flip readiness here —
                // events stay in the buffer (subject to the periodic flush's
                // later retry, which will keep tryDirect failing safely).
                log.w(e) { "Analytics init failed; continuing without analytics" }
            }
        }
    }

    private companion object {
        /** kmp-tor and bisq2 both bind their SOCKS5 listener on loopback. */
        const val LOOPBACK_SOCKS_HOST = "127.0.0.1"
    }

    /**
     * Public suspend method to deactivate all service facades.
     * This can be called to trigger a full lifecycle restart.
     * Waits for deactivation to complete.
     */
    suspend fun deactivate() {
        if (isTerminating.value) {
            log.w { "Cannot deactivate: app is terminating" }
            return
        }
        try {
            deactivateServiceFacades()
        } catch (e: Exception) {
            log.e(e) { "Error during deactivateServiceFacades" }
        }
    }

    /**
     * Public suspend method to activate all service facades.
     * This can be called to trigger a full lifecycle restart.
     * Waits for activation to complete.
     */
    suspend fun activate() {
        if (isTerminating.value) {
            log.w { "Cannot activate: app is terminating" }
            return
        }
        try {
            activateServiceFacades()
        } catch (e: Exception) {
            log.e { "Service activate error: $e" }
            onUnrecoverableError(e)
        }
    }

    protected open fun onUnrecoverableError(e: Throwable) {
        log.e(e) { "Unrecoverable error detected. Application must be restarted. Stopping services." }
        serviceScope.launch {
            try {
                deactivateServiceFacades()
            } catch (e: Exception) {
                log.w(e) { "Error while calling deactivateServiceFacades at onUnrecoverableError. This should not happen." }
            } finally {
                applicationBootstrapFacade.handleBootstrapFailure(e)
            }
        }
    }

    protected abstract suspend fun activateServiceFacades()

    protected abstract suspend fun deactivateServiceFacades()

    fun terminateApp(view: Any?) {
        if (getPlatformInfo().type != PlatformType.ANDROID) return

        if (!compareAndSetIsTerminating(expect = false, update = true)) {
            log.w { "App has already been scheduled for termination; ignoring call to terminateApp." }
            return
        }

        serviceScope.launch {
            try {
                deactivateServiceFacades()
            } catch (e: Exception) {
                log.e("Error at shutdownServicesAndTor", e)
            } finally {
                killProcess(view)
            }
        }
    }

    fun restartApp(
        view: Any?,
        purgeTorDir: Boolean = true,
    ) {
        if (getPlatformInfo().type != PlatformType.ANDROID) return

        if (!compareAndSetIsTerminating(expect = false, update = true)) {
            log.w { "App has already been scheduled for termination; ignoring call to restartApp." }
            return
        }

        serviceScope.launch {
            try {
                // Perform shutdown off the UI thread
                deactivateServiceFacades()
            } catch (e: Exception) {
                log.e("Error at deactivateServiceFacades", e)
            } finally {
                if (purgeTorDir) {
                    // Ensure Tor is fully stopped, wait for control port to close, then purge the Tor dir
                    try {
                        kmpTorService.stopAndPurgeWorkingDir()
                    } catch (e: Exception) {
                        log.w(e) { "Failed to fully stop and purge Tor before restart" }
                    }
                }
                restartProcess(view)
            }
        }
    }
}
