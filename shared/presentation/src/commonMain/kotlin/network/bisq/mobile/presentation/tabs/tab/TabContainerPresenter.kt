package network.bisq.mobile.presentation.tabs.tab

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import network.bisq.mobile.data.model.offerbook.MarketListItem
import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.service.alert.TradeRestrictingAlertServiceFacade
import network.bisq.mobile.data.service.offers.OffersServiceFacade
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.data.utils.AppUpdateLinker
import network.bisq.mobile.domain.service.community.CommunityHubService
import network.bisq.mobile.domain.service.offers.OffersBelowReputationService
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.alert.AlertNotificationUiAction
import network.bisq.mobile.presentation.common.ui.alert.AlertNotificationUiState
import network.bisq.mobile.presentation.common.ui.alert.toAlertNotificationUiState
import network.bisq.mobile.presentation.common.ui.animation.AnimationSettings
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.offer.create_offer.CreateOfferCoordinator
import network.bisq.mobile.presentation.offers_below_reputation.OffersBelowReputationUiAction
import network.bisq.mobile.presentation.offers_below_reputation.OffersBelowReputationUiState

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
    private val communityHubService: CommunityHubService,
    private val offersBelowReputationService: OffersBelowReputationService,
    private val offersServiceFacade: OffersServiceFacade,
) : BasePresenter(mainPresenter),
    ITabContainerPresenter {
    // Effective flag (user setting AND device not low-spec) so the avatar animation honours the
    // device lock too, not just the raw setting. See #1293.
    override val showAnimation: StateFlow<Boolean> get() = animationSettings.enabled
    override val tradesWithUnreadMessages: StateFlow<Map<String, Int>> get() = mainPresenter.tradesWithUnreadMessages

    private val _communityIconVisible = MutableStateFlow(false)
    override val communityIconVisible: StateFlow<Boolean> = _communityIconVisible.asStateFlow()

    private val _communityUnreadCount = MutableStateFlow(0)
    override val communityUnreadCount: StateFlow<Int> = _communityUnreadCount.asStateFlow()

    private val isOffersBelowReputationDialogOpen = MutableStateFlow(false)

    private val _offersBelowReputationUiState = MutableStateFlow(OffersBelowReputationUiState())
    override val offersBelowReputationUiState: StateFlow<OffersBelowReputationUiState> = _offersBelowReputationUiState.asStateFlow()

    override fun onViewAttached() {
        super.onViewAttached()
        communityHubService.liveSegments
            .onEach { live -> _communityIconVisible.value = live.isNotEmpty() }
            .launchIn(presenterScope)
        communityHubService.unreadCount
            .onEach { _communityUnreadCount.value = it }
            .launchIn(presenterScope)
        launchOffersBelowReputationUiState()
        offersBelowReputationService.refresh()
    }

    private fun launchOffersBelowReputationUiState() {
        // A dialog left without offers closes, so offers flagged later do not reopen it by themselves.
        offersBelowReputationService.state
            .onEach { if (it.offendingOffers.isEmpty()) isOffersBelowReputationDialogOpen.value = false }
            .launchIn(presenterScope)
        combine(offersBelowReputationService.state, isOffersBelowReputationDialogOpen) { state, isDialogOpen ->
            OffersBelowReputationUiState(
                offendingOffers = state.offendingOffers,
                isBannerVisible = state.isWarningVisible,
                isDialogVisible = isDialogOpen && state.offendingOffers.isNotEmpty(),
                isRemoving = state.isRemoving,
            )
        }.onEach { _offersBelowReputationUiState.value = it }
            .launchIn(presenterScope)
    }

    override fun openCommunityHub() {
        navigateTo(NavRoute.CommunityHub())
    }

    override fun onOffersBelowReputationAction(action: OffersBelowReputationUiAction) {
        // Nothing else may touch the dialog while its offers are being removed (back and an outside tap dispatch Keep).
        if (offersBelowReputationService.state.value.isRemoving) return
        when (action) {
            OffersBelowReputationUiAction.OpenDialog -> isOffersBelowReputationDialogOpen.value = true
            OffersBelowReputationUiAction.DismissBanner -> offersBelowReputationService.dismiss()
            OffersBelowReputationUiAction.Keep -> {
                offersBelowReputationService.dismiss()
                isOffersBelowReputationDialogOpen.value = false
            }
            OffersBelowReputationUiAction.BuildReputation -> {
                isOffersBelowReputationDialogOpen.value = false
                navigateTo(NavRoute.Reputation)
            }
            OffersBelowReputationUiAction.RemoveOffers -> removeOffersBelowReputation()
            is OffersBelowReputationUiAction.GoToMarket -> goToMarket(action.offer.market)
        }
    }

    // Leaves the warning as it is: going to look at an offer is not deciding to keep it.
    private fun goToMarket(market: MarketVO) {
        offersServiceFacade
            .selectOfferbookMarket(MarketListItem.from(market))
            .onSuccess {
                isOffersBelowReputationDialogOpen.value = false
                navigateTo(NavRoute.Offerbook(onlyMyOffers = true))
            }.onFailure { e ->
                log.e(e) { "Market selection failed" }
                showSnackbar("mobile.error.generic".i18n(), type = SnackbarType.ERROR)
            }
    }

    private fun removeOffersBelowReputation() {
        if (isDemo()) {
            showSnackbar("mobile.demo.action.disabled".i18n(), type = SnackbarType.ERROR)
            return
        }
        presenterScope.launch {
            if (!offersBelowReputationService.removeOffers()) {
                showSnackbar("mobile.bisqEasy.offerbook.offersBelowReputation.removeFailed".i18n(), type = SnackbarType.ERROR)
            }
        }
    }

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
