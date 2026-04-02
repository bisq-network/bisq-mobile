package network.bisq.mobile.presentation.offer.create_offer.direction

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.service.reputation.ReputationServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.CurrencyUtils
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.offer.OfferFlowPresenter
import network.bisq.mobile.presentation.offer.create_offer.CreateOfferCoordinator

class CreateOfferDirectionPresenter(
    mainPresenter: MainPresenter,
    private val createOfferCoordinator: CreateOfferCoordinator,
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val reputationServiceFacade: ReputationServiceFacade,
) : OfferFlowPresenter(mainPresenter) {
    var direction: DirectionEnum = createOfferCoordinator.createOfferModel.direction

    @OptIn(ExperimentalCoroutinesApi::class)
    val marketName: StateFlow<String?> =
        mainPresenter.languageCode
            .mapLatest { _ ->
                createOfferCoordinator.createOfferModel.market?.let { market ->
                    CurrencyUtils.getLocaleFiatCurrencyName(
                        market.quoteCurrencyCode,
                        market.quoteCurrencyName,
                    )
                }
            }.stateIn(
                presenterScope,
                SharingStarted.WhileSubscribed(5000),
                null,
            )

    val headline: StateFlow<String> =
        marketName
            .map { localizedMarketName ->
                if (localizedMarketName != null) {
                    "mobile.bisqEasy.tradeWizard.directionAndMarket.headlineWithMarket".i18n(localizedMarketName)
                } else {
                    "mobile.bisqEasy.tradeWizard.directionAndMarket.headlineNoMarket".i18n()
                }
            }.stateIn(
                presenterScope,
                SharingStarted.WhileSubscribed(5000),
                "mobile.bisqEasy.tradeWizard.directionAndMarket.headlineNoMarket".i18n(),
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val reputationTotalScore =
        userProfileServiceFacade.selectedUserProfile
            .mapLatest { profile ->
                profile
                    ?.let {
                        reputationServiceFacade.getReputation(it.id).also { result ->
                            result.onFailure { throwable ->
                                log.w(
                                    throwable,
                                    "CreateOfferDirectionPresenter",
                                ) { "Exception at reputationServiceFacade.getReputation" }
                            }
                        }
                    }?.let { it.getOrNull()?.totalScore } ?: 0L
            }.stateIn(
                presenterScope,
                SharingStarted.WhileSubscribed(5000),
                0L,
            )

    private var reputationCollectorJob: Job? = null

    private val _showSellerReputationWarning = MutableStateFlow(false)
    val showSellerReputationWarning: StateFlow<Boolean> get() = _showSellerReputationWarning.asStateFlow()

    fun setShowSellerReputationWarning(value: Boolean) {
        _showSellerReputationWarning.value = value
    }

    override fun onViewAttached() {
        super.onViewAttached()
        reputationCollectorJob?.cancel()
        reputationCollectorJob = reputationTotalScore.launchIn(presenterScope)
    }

    override fun onViewUnattaching() {
        super.onViewUnattaching()
        reputationCollectorJob?.cancel()
        reputationCollectorJob = null
    }

    fun onBuySelected() {
        direction = DirectionEnum.BUY
        navigateNext()
    }

    fun onSellSelected() {
        val userReputation = reputationTotalScore.value
        if (userReputation == 0L) {
            setShowSellerReputationWarning(true)
        } else {
            direction = DirectionEnum.SELL
            navigateNext()
        }
    }

    fun onClose() {
        commitToModel()
        navigateToOfferbookTab()
    }

    fun onNavigateToReputation() {
        navigateTo(NavRoute.Reputation)
        setShowSellerReputationWarning(false)
    }

    fun onDismissSellerReputationWarning() {
        setShowSellerReputationWarning(false)
    }

    private fun navigateNext() {
        commitToModel()
        if (createOfferCoordinator.skipCurrency) {
            navigateTo(NavRoute.CreateOfferAmount)
        } else {
            navigateTo(NavRoute.CreateOfferMarket)
        }
    }

    private fun commitToModel() {
        createOfferCoordinator.commitDirection(direction)
    }
}
