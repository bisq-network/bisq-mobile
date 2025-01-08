package network.bisq.mobile.presentation.ui.uicases.offer.create_offer

import network.bisq.mobile.domain.replicated.offer.DirectionEnum
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
        direction = DirectionEnum.SELL
        navigateNext()
    }

    fun onBack() {
        commitToModel()
        rootNavigator.popBackStack()
    }

    fun onNext() {
        navigateNext()
    }

    private fun navigateNext() {
        commitToModel()
        if (createOfferPresenter.createOfferModel.market == null) {
            rootNavigator.navigate(Routes.CreateOfferMarket.name)
        } else {
            rootNavigator.navigate(Routes.CreateOfferAmount.name)
        }
    }

    private fun commitToModel() {
        createOfferPresenter.commitDirection(direction)
    }
}
