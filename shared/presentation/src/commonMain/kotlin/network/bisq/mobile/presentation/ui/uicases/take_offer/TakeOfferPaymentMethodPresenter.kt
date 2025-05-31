package network.bisq.mobile.presentation.ui.uicases.take_offer

import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.i18n.i18n
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
    val quoteSidePaymentMethod: MutableStateFlow<String?> = MutableStateFlow(null)
    val baseSidePaymentMethod: MutableStateFlow<String?> = MutableStateFlow(null)
    lateinit var quoteCurrencyCode: String

    private lateinit var takeOfferModel: TakeOfferPresenter.TakeOfferModel

    init {
        takeOfferModel = takeOfferPresenter.takeOfferModel
        hasMultipleQuoteSidePaymentMethods = takeOfferModel.hasMultipleQuoteSidePaymentMethods
        hasMultipleBaseSidePaymentMethods = takeOfferModel.hasMultipleBaseSidePaymentMethods

        val offerListItem = takeOfferModel.offerItemPresentationVO
        quoteSidePaymentMethods = offerListItem.quoteSidePaymentMethods
        if (takeOfferModel.quoteSidePaymentMethod.isNotEmpty()) {
            quoteSidePaymentMethod.value = takeOfferModel.quoteSidePaymentMethod
        } else {
            if (quoteSidePaymentMethods.size == 1) {
                quoteSidePaymentMethod.value = quoteSidePaymentMethods[0]
            }
        }

        baseSidePaymentMethods = offerListItem.baseSidePaymentMethods
        if (takeOfferModel.baseSidePaymentMethod.isNotEmpty()) {
            baseSidePaymentMethod.value = takeOfferModel.baseSidePaymentMethod
        } else {
            if (offerListItem.baseSidePaymentMethods.size == 1) {
                baseSidePaymentMethod.value = offerListItem.baseSidePaymentMethods[0]
            }
        }
        quoteCurrencyCode = offerListItem.bisqEasyOffer.market.quoteCurrencyCode
    }

    override fun onViewAttached() {
        super.onViewAttached()
    }

    override fun onViewUnattaching() {
        dismissSnackbar()
        super.onViewUnattaching()
    }

    fun onQuoteSidePaymentMethodSelected(paymentMethod: String) {
        quoteSidePaymentMethod.value = paymentMethod
    }

    fun onBaseSidePaymentMethodSelected(paymentMethod: String) {
        baseSidePaymentMethod.value = paymentMethod
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
            if (quoteSidePaymentMethod.value == null) {
                showSnackbar("bisqEasy.tradeWizard.review.paymentMethodDescriptions.fiat.taker".i18n())
            } else if (baseSidePaymentMethod.value == null) {
                showSnackbar("bisqEasy.tradeWizard.review.paymentMethodDescriptions.btc.taker".i18n())
            }
            // Note the data is set at the service layer, so if there is only one payment method we
            // have it set at the service. We do not need to check here if we have the multiple options.
        }
    }

    private fun commitToModel() {
        if (isValid()) {
            takeOfferPresenter.commitPaymentMethod(quoteSidePaymentMethod.value!!, baseSidePaymentMethod.value!!)
        }
    }

    private fun isValid() = quoteSidePaymentMethod.value != null && baseSidePaymentMethod.value != null

    fun getQuoteSidePaymentMethodsImagePaths(): List<String> {
        return getPaymentMethodsImagePaths(quoteSidePaymentMethods, "fiat")
    }

    fun getBaseSidePaymentMethodsImagePaths(): List<String> {
        return getPaymentMethodsImagePaths(baseSidePaymentMethods, "bitcoin")
    }

    private fun getPaymentMethodsImagePaths(list: List<String>, directory: String) = list
        .map { paymentMethod ->
            val fileName = paymentMethod.lowercase().replace("-", "_")
            "drawable/payment/$directory/$fileName.png"
        }
}
