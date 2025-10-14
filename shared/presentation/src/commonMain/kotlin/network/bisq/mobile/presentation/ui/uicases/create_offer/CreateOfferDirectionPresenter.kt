package network.bisq.mobile.presentation.ui.uicases.create_offer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.service.reputation.ReputationServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.CurrencyUtils
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.NavRoute

class CreateOfferDirectionPresenter(
    mainPresenter: MainPresenter,
    private val createOfferPresenter: CreateOfferPresenter,
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val reputationServiceFacade: ReputationServiceFacade
) : BasePresenter(mainPresenter) {

    var direction: DirectionEnum = createOfferPresenter.createOfferModel.direction
    val marketName: String?
        get() = createOfferPresenter.createOfferModel.market?.let { market ->
            CurrencyUtils.getLocaleFiatCurrencyName(
                market.quoteCurrencyCode,
                market.quoteCurrencyName
            )
        }
    val headline: String
        get() {
            val market = createOfferPresenter.createOfferModel.market
            return if (market != null) {
                val fiatName = CurrencyUtils.getLocaleFiatCurrencyName(
                    market.quoteCurrencyCode,
                    market.quoteCurrencyName
                )
                "mobile.bisqEasy.tradeWizard.directionAndMarket.headlineWithMarket".i18n(fiatName)
            } else {
                "mobile.bisqEasy.tradeWizard.directionAndMarket.headlineNoMarket".i18n()
            }
        }
    private val _reputation = MutableStateFlow<ReputationScoreVO?>(null)

    private val _showSellerReputationWarning = MutableStateFlow(false)
    val showSellerReputationWarning: StateFlow<Boolean> get() = _showSellerReputationWarning.asStateFlow()
    fun setShowSellerReputationWarning(value: Boolean) {
        _showSellerReputationWarning.value = value
    }

    override fun onViewAttached() {
        super.onViewAttached()
        launchIO {
            val profile = userProfileServiceFacade.getSelectedUserProfile() ?: return@launchIO
            val reputation = reputationServiceFacade.getReputation(profile.id).getOrNull()

            withContext(Dispatchers.Main) {
                _reputation.value = reputation
            }
        }
    }

    fun onBuySelected() {
        direction = DirectionEnum.BUY
        navigateNext()
    }

    fun onSellSelected() {
        val userReputation = _reputation.value?.totalScore ?: 0L
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
