package network.bisq.mobile.domain.analytics

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import network.bisq.mobile.domain.repository.SettingsRepository

/**
 * Builds the [BufferedAnalyticsService] wrapping a [SentryAnalyticsService] — the single
 * analytics wiring shared by the client and node DI modules; see those modules for the
 * app-specific rationale (DSN selection, SOCKS port source).
 *
 * Two gates AND'd together on every emit:
 *  1. [analyticsDevEnabled] — dev-only build flag; release builds const-fold it to true
 *     at BuildConfig generation time.
 *  2. User-settings toggle — flipped from the Settings UI, no rebuild required.
 */
fun createBufferedAnalyticsService(
    settingsRepository: SettingsRepository,
    nativeInitializer: NativeSentryInitializer,
    analyticsDevEnabled: Boolean,
): BufferedAnalyticsService {
    // Independent scope so the buffer's periodic flusher survives any
    // individual feature-scope cancellation. SupervisorJob so a single
    // failed enqueue doesn't kill the flusher. Reused for the
    // settings-derived analyticsEnabled StateFlow so its hot-share
    // lives exactly as long as the BufferedAnalyticsService instance.
    val analyticsScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    // Hot StateFlow view of `Settings.analyticsEnabled`. Backed by
    // DataStore so a settings flip from any UI surface propagates
    // immediately to this `.value` read in the runtime gate.
    val analyticsEnabledFlow = settingsRepository.analyticsEnabledIn(analyticsScope)
    return BufferedAnalyticsService(
        downstream =
            SentryAnalyticsService(
                nativeInitializer = nativeInitializer,
                runtimeOptInProvider = {
                    analyticsDevEnabled && analyticsEnabledFlow.value
                },
            ),
        scope = analyticsScope,
    )
}
