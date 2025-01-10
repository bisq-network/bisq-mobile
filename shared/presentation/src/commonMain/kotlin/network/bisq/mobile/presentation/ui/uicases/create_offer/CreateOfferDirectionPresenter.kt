package network.bisq.mobile.presentation.ui.uicases.create_offer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.i18n.AppStrings
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

class CreateOfferDirectionPresenter(
    mainPresenter: MainPresenter,
    private val createOfferPresenter: CreateOfferPresenter
) : BasePresenter(mainPresenter) {

    lateinit var direction: DirectionEnum
    lateinit var headline: String

    lateinit var appStrings: AppStrings

    private val _showSellerReputationWarning = MutableStateFlow(false)
    val showSellerReputationWarning : StateFlow<Boolean> get() = _showSellerReputationWarning
    fun setShowSellerReputationWarning (value: Boolean) {
       _showSellerReputationWarning.value = value
    }

    override fun onViewAttached() {
        direction = createOfferPresenter.createOfferModel.direction
        headline = appStrings.bisqEasyTradeWizard.bisqEasy_tradeWizard_directionAndMarket_headline
    }

    fun onBuySelected() {
        direction = DirectionEnum.BUY
        navigateNext()
    }

    fun onSellSelected() {
        // TODO show warning if no reputation
        val userReputation = 0
        if (userReputation == 0) {
            setShowSellerReputationWarning(true)
        } else {
            direction = DirectionEnum.SELL
            navigateNext()
        }
    }

    fun onSellWithoutReputation() {
        direction = DirectionEnum.SELL
        navigateNext()
    }

    fun onBack() {
        commitToModel()
        navigateBack()
    }

    fun onNext() {
        navigateNext()
    }

    fun showLearnReputation() {
        log.i {"show learn reputation page in browser"}
        // TODO: Uncomment after #143 merge
//        enableInteractive(false)
//        navigateToUrl("https://bisq.wiki/Reputation")
//        enableInteractive(true)
    }

    private fun navigateNext() {
        commitToModel()
        navigateTo(Routes.CreateOfferMarket)
    }

    private fun commitToModel() {
        createOfferPresenter.commitDirection(direction)
    }
}
