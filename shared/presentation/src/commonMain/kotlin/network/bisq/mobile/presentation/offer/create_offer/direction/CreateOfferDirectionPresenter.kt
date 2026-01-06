package network.bisq.mobile.presentation.offer.create_offer.direction

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.domain.service.reputation.ReputationServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.CurrencyUtils
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.offer.create_offer.CreateOfferPresenter

class CreateOfferDirectionPresenter(
    mainPresenter: MainPresenter,
    private val createOfferPresenter: CreateOfferPresenter,
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val reputationServiceFacade: ReputationServiceFacade,
) : BasePresenter(mainPresenter) {
    var direction: DirectionEnum = createOfferPresenter.createOfferModel.direction
    val marketName: String?
        get() =
            createOfferPresenter.createOfferModel.market?.let { market ->
                CurrencyUtils.getLocaleFiatCurrencyName(
                    market.quoteCurrencyCode,
                    market.quoteCurrencyName,
                )
            }
    val headline: String
        get() {
            val market = createOfferPresenter.createOfferModel.market
            return if (market != null) {
                val fiatName =
                    CurrencyUtils.getLocaleFiatCurrencyName(
                        market.quoteCurrencyCode,
                        market.quoteCurrencyName,
                    )
                "mobile.bisqEasy.tradeWizard.directionAndMarket.headlineWithMarket".i18n(fiatName)
            } else {
                "mobile.bisqEasy.tradeWizard.directionAndMarket.headlineNoMarket".i18n()
            }
        }

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
        if (createOfferPresenter.skipCurrency) {
            navigateTo(NavRoute.CreateOfferAmount)
        } else {
            navigateTo(NavRoute.CreateOfferMarket)
        }
    }

    private fun commitToModel() {
        createOfferPresenter.commitDirection(direction)
    }
}
