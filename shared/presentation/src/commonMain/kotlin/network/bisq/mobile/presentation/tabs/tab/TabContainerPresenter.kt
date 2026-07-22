package network.bisq.mobile.presentation.tabs.tab

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.data.service.alert.TradeRestrictingAlertServiceFacade
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.data.utils.AppUpdateLinker
import network.bisq.mobile.presentation.common.ui.alert.AlertNotificationUiAction
import network.bisq.mobile.presentation.common.ui.alert.AlertNotificationUiState
import network.bisq.mobile.presentation.common.ui.alert.toAlertNotificationUiState
import network.bisq.mobile.presentation.common.ui.animation.AnimationSettings
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.offer.create_offer.CreateOfferCoordinator

/**
 * Main presenter for the display when landing the user on the app ready to be used.
 */
class TabContainerPresenter(
    private val mainPresenter: MainPresenter,
    private val createOfferCoordinator: CreateOfferCoordinator,
    private val settingsServiceFacade: SettingsServiceFacade,
    private val tradeRestrictingAlertServiceFacade: TradeRestrictingAlertServiceFacade,
    private val appUpdateLinker: AppUpdateLinker,
    private val animationSettings: AnimationSettings,
) : BasePresenter(mainPresenter),
    ITabContainerPresenter {
    // Effective flag (user setting AND device not low-spec) so the avatar animation honours the
    // device lock too, not just the raw setting. See #1293.
    override val showAnimation: StateFlow<Boolean> get() = animationSettings.enabled
    override val tradesWithUnreadMessages: StateFlow<Map<String, Int>> get() = mainPresenter.tradesWithUnreadMessages

    private val _showTradeRestrictedDialog = MutableStateFlow<AlertNotificationUiState?>(null)
    override val showTradeRestrictedDialog: StateFlow<AlertNotificationUiState?> = _showTradeRestrictedDialog.asStateFlow()

    private val _isCreateOfferEnabled = MutableStateFlow(true)
    override val isCreateOfferEnabled: StateFlow<Boolean> = _isCreateOfferEnabled.asStateFlow()

    override fun createOffer() {
        val activeAlert = tradeRestrictingAlertServiceFacade.alert.value
        if (activeAlert != null) {
            _showTradeRestrictedDialog.value = activeAlert.toAlertNotificationUiState()
            return
        }
        guardedSuspendAction(
            _isCreateOfferEnabled,
            "createOffer",
            showLoadingOverlay = false,
            reEnableGuardOnComplete = false,
        ) {
            try {
                createOfferCoordinator.onStartCreateOffer()
                createOfferCoordinator.skipCurrency = false
                navigateTo(NavRoute.CreateOfferDirection)
            } catch (e: Exception) {
                _isCreateOfferEnabled.value = true
                log.e(e) { "Failed to create offer: ${e.message}" }
            }
        }
    }

    override fun onTradeRestrictingAlertAction(action: AlertNotificationUiAction) {
        when (action) {
            AlertNotificationUiAction.OnUpdateNow -> {
                _showTradeRestrictedDialog.value = null
                navigateToUrl(appUpdateLinker.getUpdateUrl())
            }
            AlertNotificationUiAction.OnCloseDialog -> _showTradeRestrictedDialog.value = null
            else -> Unit
        }
    }
}
