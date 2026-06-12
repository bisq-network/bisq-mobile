package network.bisq.mobile.presentation.tabs.dashboard.welcome_carousel

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import network.bisq.mobile.data.model.BatteryOptimizationState
import network.bisq.mobile.data.model.PermissionState
import network.bisq.mobile.data.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.data.service.push_notification.PushNotificationServiceFacade
import network.bisq.mobile.data.utils.getPlatformInfo
import network.bisq.mobile.domain.model.PlatformInfo
import network.bisq.mobile.domain.model.PlatformType
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.utils.BisqLinks
import network.bisq.mobile.presentation.main.MainPresenter

/**
 * Drives the dashboard welcome carousel: which opt-in cards are still pending,
 * and how a "Don't ask again" / "Enable" tap persists.
 *
 * Pending pages are computed live from [SettingsRepository] + the relayed-push
 * opt-in flag. As one card resolves (the user grants/denies the OS prompt or
 * taps a resolution button), the underlying state changes and the live list
 * reflects that — the carousel reactively transitions to the next pending card
 * without any session snapshot.
 *
 * OS-coupled primary actions (asking the OS for notification permission, opening
 * battery settings) need Composable-side launchers and stay in [DashboardScreen].
 * Pure state actions (analytics enable, analytics learn-more, don't-ask-again)
 * are owned here.
 */
open class WelcomeCarouselPresenter(
    mainPresenter: MainPresenter,
    private val settingsRepository: SettingsRepository,
    private val pushNotificationServiceFacade: PushNotificationServiceFacade,
    private val platformInfo: PlatformInfo = getPlatformInfo(),
) : BasePresenter(mainPresenter) {
    private val notificationPermissionState =
        settingsRepository.data
            .map { it.notificationPermissionState }
            .distinctUntilChanged()

    private val batteryOptimizationState =
        settingsRepository.data
            .map { it.batteryOptimizationState }
            .distinctUntilChanged()

    private val analyticsPromptSeen =
        settingsRepository.data
            .map { it.analyticsPromptSeen }
            .distinctUntilChanged()

    val uiState: StateFlow<WelcomeCarouselUiState> =
        combine(
            notificationPermissionState,
            batteryOptimizationState,
            pushNotificationServiceFacade.isPushNotificationsEnabled,
            analyticsPromptSeen,
        ) { notifState, batteryState, pushEnabled, analyticsSeen ->
            // BasePresenter.isDemo() returns false (MainPresenter hardcodes it);
            // the actual demo flag lives on ApplicationBootstrapFacade, set by the
            // pairing flow when the BISQ_DEMO_PAIRING_CODE is entered.
            if (ApplicationBootstrapFacade.isDemo) {
                WelcomeCarouselUiState()
            } else {
                WelcomeCarouselUiState(
                    pages =
                        buildPendingPages(
                            notifState = notifState,
                            batteryState = batteryState,
                            relayedPushEnabled = pushEnabled,
                            analyticsPromptSeen = analyticsSeen,
                        ),
                )
            }
        }.stateIn(
            scope = presenterScope,
            // Eagerly so .value always reflects the computed state. Source flows
            // (settings + relayed-push flag) are cheap; no benefit to gating on
            // subscription, and tests reading .value without an active collector
            // would otherwise see only the initial value.
            started = SharingStarted.Eagerly,
            initialValue = WelcomeCarouselUiState(),
        )

    fun onAction(action: WelcomeCarouselUiAction) {
        when (action) {
            is WelcomeCarouselUiAction.OnDontAskAgain ->
                presenterScope.launch {
                    when (action.type) {
                        CarouselPageType.NOTIFICATIONS ->
                            settingsRepository.setNotificationPermissionState(
                                PermissionState.DONT_ASK_AGAIN,
                            )

                        CarouselPageType.BATTERY ->
                            settingsRepository.setBatteryOptimizationPermissionState(
                                BatteryOptimizationState.DONT_ASK_AGAIN,
                            )

                        CarouselPageType.ANALYTICS ->
                            // Analytics has no DONT_ASK_AGAIN state — the prompt-seen
                            // flag is the only thing that gates the carousel. We never
                            // flip analyticsEnabled here: dismissal must not opt the
                            // user into reporting.
                            settingsRepository.setAnalyticsPromptSeen(true)
                    }
                }

            WelcomeCarouselUiAction.OnEnableAnalytics ->
                presenterScope.launch {
                    // Atomic write of both fields mirrors the Settings screen's
                    // toggle handler, so the runtime opt-in provider (which reads
                    // analyticsEnabled) and the carousel gate (which reads
                    // analyticsPromptSeen) are consistent on the next emission.
                    settingsRepository.update {
                        it.copy(analyticsEnabled = true, analyticsPromptSeen = true)
                    }
                }

            WelcomeCarouselUiAction.OnAnalyticsLearnMore ->
                navigateToUrl(BisqLinks.BISQ_MOBILE_ANALYTICS_WIKI_URL)
        }
    }

    private fun buildPendingPages(
        notifState: PermissionState?,
        batteryState: BatteryOptimizationState?,
        relayedPushEnabled: Boolean,
        analyticsPromptSeen: Boolean,
    ): List<CarouselPageType> {
        val pages = mutableListOf<CarouselPageType>()

        if (isNotificationsPending(notifState)) {
            pages.add(CarouselPageType.NOTIFICATIONS)
        }

        if (isBatteryPending(
                batteryState = batteryState,
                relayedPushEnabled = relayedPushEnabled,
            )
        ) {
            pages.add(CarouselPageType.BATTERY)
        }

        if (isAnalyticsPending(analyticsPromptSeen)) {
            pages.add(CarouselPageType.ANALYTICS)
        }

        return pages
    }

    private fun isNotificationsPending(notifState: PermissionState?): Boolean = notifState == PermissionState.NOT_GRANTED || notifState == PermissionState.DENIED

    private fun isBatteryPending(
        batteryState: BatteryOptimizationState?,
        relayedPushEnabled: Boolean,
    ): Boolean {
        // Battery card is Android-only.
        if (platformInfo.type != PlatformType.ANDROID) return false
        // Suppressed when the user opted into relayed push notifications (settings,
        // not a carousel card): the local foreground service is stopped and FCM/APNs
        // deliver pushes regardless of Doze, so weakening battery defaults yields
        // nothing. Independent of the user's choice on any other carousel card.
        if (relayedPushEnabled) return false
        return batteryState == BatteryOptimizationState.NOT_IGNORED
    }

    // Pending whenever the user hasn't yet engaged with the analytics opt-in —
    // either via this carousel or via Settings → Analytics (which sets the same
    // flag). Existing users on a fresh install of the new version will see the
    // card once; users who already toggled in Settings will not.
    private fun isAnalyticsPending(analyticsPromptSeen: Boolean): Boolean = !analyticsPromptSeen
}
