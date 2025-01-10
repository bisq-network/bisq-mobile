package network.bisq.mobile.presentation.ui.uicases.trade.take_offer

import kotlinx.coroutines.flow.MutableStateFlow
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
    var quoteCurrencyCode: String = "USD"

    private lateinit var takeOfferModel: TakeOfferPresenter.TakeOfferModel

    override fun onViewAttached() {
        takeOfferModel = takeOfferPresenter.takeOfferModel
        hasMultipleQuoteSidePaymentMethods = takeOfferModel.hasMultipleQuoteSidePaymentMethods
        hasMultipleBaseSidePaymentMethods = takeOfferModel.hasMultipleBaseSidePaymentMethods

        val offerListItem = takeOfferModel.offerListItem
        quoteSidePaymentMethods = offerListItem.quoteSidePaymentMethods
        baseSidePaymentMethods = offerListItem.baseSidePaymentMethods
        if (quoteSidePaymentMethods.size == 1) {
            quoteSidePaymentMethod = quoteSidePaymentMethods[0]
        }
        if (offerListItem.baseSidePaymentMethods.size == 1) {
            baseSidePaymentMethod = offerListItem.baseSidePaymentMethods[0]
        }

        quoteCurrencyCode = takeOfferModel.priceQuote.market.quoteCurrencyCode
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
            var warningMessage = "Please select both Fiat and Bitcoin payment methods"
            if (quoteSidePaymentMethod == null && baseSidePaymentMethod != null) {
                warningMessage = "Please select fiat payment method"
            } else if (quoteSidePaymentMethod != null && baseSidePaymentMethod == null) {
                warningMessage = "Please select settlement method"
            }
            showSnackbar(warningMessage)
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

    fun getPaymentMethodAsSet(paymentMethod: String?): MutableStateFlow<Set<String>> {
        return if (paymentMethod == null)
            MutableStateFlow(emptySet())
        else {
            MutableStateFlow(setOf(paymentMethod))
        }
    }

    fun getQuoteSidePaymentMethodsImagePaths(): List<String> {
        return quoteSidePaymentMethods.map { payment ->
            getPaymentMethodImagePath(payment, "fiat")
        }
    }

    fun getBaseSidePaymentMethodsImagePaths(): List<String> {
        return baseSidePaymentMethods.map { payment ->
            getPaymentMethodImagePath(payment, "bitcoin")
        }
    }

    private fun getPaymentMethodImagePath(paymentMethod: String, directory: String): String {
        val fileName = paymentMethod.lowercase().replace("-", "_")
        return "drawable/payment/$directory/$fileName.png"
    }
}
