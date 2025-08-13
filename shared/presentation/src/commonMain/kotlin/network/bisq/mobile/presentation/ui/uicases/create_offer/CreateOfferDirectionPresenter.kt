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
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.BisqLinks
import network.bisq.mobile.presentation.ui.navigation.Routes

class CreateOfferDirectionPresenter(
    mainPresenter: MainPresenter,
    private val createOfferPresenter: CreateOfferPresenter,
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val reputationServiceFacade: ReputationServiceFacade
) : BasePresenter(mainPresenter) {

    lateinit var direction: DirectionEnum
    lateinit var headline: String
    private val _reputation = MutableStateFlow<ReputationScoreVO?>(null)

    private val _showSellerReputationWarning = MutableStateFlow(false)
    val showSellerReputationWarning: StateFlow<Boolean> get() = _showSellerReputationWarning.asStateFlow()
    fun setShowSellerReputationWarning(value: Boolean) {
        _showSellerReputationWarning.value = value
    }

    override fun onViewAttached() {
        super.onViewAttached()
        direction = createOfferPresenter.createOfferModel.direction
        headline = "bisqEasy.tradeWizard.directionAndMarket.headline".i18n()

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
        navigateToOfferList()
    }

    fun showLearnReputation() {
        setShowSellerReputationWarning(false)
        navigateToUrl(BisqLinks.REPUTATION_BUILD_WIKI_URL)
    }

    fun onDismissSellerReputationWarning() {
        setShowSellerReputationWarning(false)
    }

    private fun navigateNext() {
        commitToModel()
        if (createOfferPresenter.skipCurrency)
            navigateTo(Routes.CreateOfferAmount)
        else
            navigateTo(Routes.CreateOfferMarket)
    }

    private fun navigateToOfferList() {
        navigateBackTo(Routes.TabContainer)
        navigateToTab(Routes.TabOfferbook)
    }

    private fun commitToModel() {
        createOfferPresenter.commitDirection(direction)
    }
}
