package network.bisq.mobile.domain.analytics

import kotlinx.coroutines.flow.first
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.domain.utils.Logging

/**
 * Emits a snapshot of the user-controlled settings as analytics events, fired
 * once per app process right after the user opts into analytics (called from
 * `ApplicationLifecycleService.bootstrapAnalytics()` post-`onSentryReady()`).
 *
 * ## Why this exists
 *
 * Without a baseline, settings analytics only see "deltas" — users who never
 * change a setting after opt-in are invisible for that setting. The baseline
 * captures "user has language X / push enabled / keep-connected on" the moment
 * they go live, which is the moment we know they're a real measured user.
 *
 * It also smooths over timing fragility in the per-presenter observers: e.g.
 * the [MainPresenter] language observer can miss the initial value if the
 * platform-specific `SettingsServiceFacade` populates `_languageCode` after
 * the observer subscribes (verified on the Node app, 2026-06-12). The baseline
 * reads `.value` at a known-good moment AFTER the SDK is fully ready.
 *
 * ## Privacy
 *
 * Emits ONLY events from the existing sealed [AnalyticsEvent.Settings]
 * hierarchy — no new wire-name shapes, no payloads beyond what change-events
 * already carry. The privacy review surface stays unchanged. The semantic of
 * each event becomes "user currently has X" (whether arrived at via baseline
 * or change). Aggregation logic on the server side doesn't care.
 *
 * ## Lifecycle
 *
 * Idempotency is the caller's responsibility. `ApplicationLifecycleService`
 * already calls this exactly once per process via the CAS guard inside
 * `BufferedAnalyticsService.onSentryReady()` (which itself runs once). If a
 * future caller wires this differently, they own the dedup.
 */
class AnalyticsSettingsBaseline(
    private val analyticsService: AnalyticsService,
    private val settingsRepository: SettingsRepository,
    private val settingsServiceFacade: SettingsServiceFacade,
) : Logging {
    /**
     * Read the current value of each tracked setting and emit the matching
     * `Settings.*` event — but ONLY if `analyticsBaselineSent` is false. The
     * one-shot-per-opt-in flag is set to true at the end so subsequent cold
     * starts (which would otherwise re-trigger this method via the per-process
     * `onSentryReady`) skip cleanly. The opt-out path in `SettingsPresenter`
     * resets it to false, so the next opt-in re-emits a fresh baseline.
     *
     * Suspends on `settingsRepository.data.first()` since that's exposed as
     * `Flow<Settings>`, not `StateFlow` — the call site
     * (`ApplicationLifecycleService.bootstrapAnalytics`) already runs inside a
     * `serviceScope.launch`, so the suspension is free. Other reads are
     * non-blocking `.value` reads on StateFlow.
     *
     * Each `analyticsService.track` is itself non-blocking — the buffered-vs-
     * direct decision happens inside the service.
     */
    suspend fun emit() {
        // DataStore is already warmed by ApplicationLifecycleService before
        // this point — `first()` returns immediately with the current value.
        val settings = settingsRepository.data.first()

        if (settings.analyticsBaselineSent) {
            log.d { "Analytics: baseline already sent for this opt-in cycle, skipping" }
            return
        }

        // Analytics — always Enabled at this point (the caller is the
        // post-opt-in lifecycle path). Emitted anyway so each opt-in cycle has
        // a clean "this user is on" marker for the analytics dimension,
        // symmetric with the other three.
        analyticsService.track(AnalyticsEvent.Settings.AnalyticsEnabled)

        // Language — normalised via the shared helper so platform-specific
        // code formats (Node's bisq2 raw codes) don't get dropped silently.
        // Skip emission entirely if the code is unknown (defence in depth on
        // top of the per-presenter observer's same filter).
        AnalyticsEvent.Settings
            .normalizeLanguageCode(settingsServiceFacade.languageCode.value)
            ?.let { code ->
                analyticsService.track(AnalyticsEvent.Settings.LanguageChanged(code))
            }

        // Push notifications opt-in — read from the already-collected settings
        // snapshot. The PushNotificationServiceFacade's `isPushNotificationsEnabled`
        // StateFlow is NOT a safe baseline source because:
        //   (a) ClientPushNotificationServiceFacade seeds it to false and only
        //       catches up asynchronously inside an activate-time collector
        //       (documented in ClientApplicationLifecycleService.kt:299-301)
        //   (b) NoOpPushNotificationServiceFacade for the Node app NEVER
        //       updates it, so the facade would always report false regardless
        //       of the user's actual setting.
        // settings.pushNotificationsEnabled is the persisted ground truth in
        // both apps; reading from the already-collected snapshot also keeps
        // the baseline atomic (all 4 events reflect the same point-in-time).
        analyticsService.track(
            if (settings.pushNotificationsEnabled) {
                AnalyticsEvent.Settings.PushNotificationsEnabled
            } else {
                AnalyticsEvent.Settings.PushNotificationsDisabled
            },
        )

        // Keep-connected sub-setting (Android-Connect-only in UI; on Node it
        // stays at the default false and that's an honest signal too).
        analyticsService.track(
            if (settings.keepConnectedInBackground) {
                AnalyticsEvent.Settings.KeepConnectedEnabled
            } else {
                AnalyticsEvent.Settings.KeepConnectedDisabled
            },
        )

        // Pin the "already sent" flag AFTER the events are queued. If track
        // calls throw (defensive — they shouldn't with the buffered service),
        // we leave the flag false and the next opt-in retries. The track
        // operations themselves are fire-and-forget at the analyticsService
        // contract so there's no real chance of failure here, but staying
        // conservative is cheap.
        settingsRepository.setAnalyticsBaselineSent(true)

        log.i { "Analytics: emitted settings baseline snapshot" }
    }
}
