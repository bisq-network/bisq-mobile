package network.bisq.mobile.presentation.ui.uicases.take_offer

import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

class TakeOfferPaymentMethodPresenter(
    mainPresenter: MainPresenter,
    private val takeOfferPresenter: TakeOfferPresenter
) : BasePresenter(mainPresenter) {

    var hasMultipleQuoteSidePaymentMethods: Boolean = false
    var hasMultipleBaseSidePaymentMethods: Boolean = false
    lateinit var quoteSidePaymentMethods: List<String>
    lateinit var baseSidePaymentMethods: List<String>
    var quoteSidePaymentMethod: String? = null
    var baseSidePaymentMethod: String? = null

    private lateinit var takeOfferModel: TakeOfferPresenter.TakeOfferModel

    override fun onViewAttached() {
        takeOfferModel = takeOfferPresenter.takeOfferModel
        hasMultipleQuoteSidePaymentMethods = takeOfferModel.hasMultipleQuoteSidePaymentMethods
        hasMultipleBaseSidePaymentMethods = takeOfferModel.hasMultipleBaseSidePaymentMethods

        val offerListItem = takeOfferModel.offerItemPresentationVO
        quoteSidePaymentMethods = offerListItem.quoteSidePaymentMethods
        baseSidePaymentMethods = offerListItem.baseSidePaymentMethods
        if (quoteSidePaymentMethods.size == 1) {
            quoteSidePaymentMethod = quoteSidePaymentMethods[0]
        }
        if (offerListItem.baseSidePaymentMethods.size == 1) {
            baseSidePaymentMethod = offerListItem.baseSidePaymentMethods[0]
        }
    }

    fun onQuoteSidePaymentMethodSelected(paymentMethod: String) {
        quoteSidePaymentMethod = paymentMethod
    }

    fun onBaseSidePaymentMethodSelected(paymentMethod: String) {
        baseSidePaymentMethod = paymentMethod
    }

    fun onBack() {
        commitToModel()
        navigateBack()
    }

    fun onNext() {
        if (isValid()) {
            commitToModel()
            navigateTo(Routes.TakeOfferReviewTrade)
        } else {
            //TODO show user feedback if one or both are not selected.
            // Note the data is set at the service layer, so if there is only one payment method we
            // have it set at the service. We do not need to check here if we have the multiple options.
        }
    }

    private fun commitToModel() {
        if (isValid()) {
            takeOfferPresenter.commitPaymentMethod(quoteSidePaymentMethod!!, baseSidePaymentMethod!!)
        }
    }

    private fun isValid() = quoteSidePaymentMethod != null && baseSidePaymentMethod != null
}
