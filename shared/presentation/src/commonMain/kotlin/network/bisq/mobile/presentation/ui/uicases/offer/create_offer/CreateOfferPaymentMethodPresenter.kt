package network.bisq.mobile.presentation.ui.uicases.offer.create_offer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import network.bisq.mobile.domain.replicated.offer.isBuy
import network.bisq.mobile.i18n.AppStrings
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

class CreateOfferPaymentMethodPresenter(
    mainPresenter: MainPresenter,
    private val createOfferPresenter: CreateOfferPresenter
) : BasePresenter(mainPresenter) {

    lateinit var quoteSideHeadline: String
    lateinit var baseSideHeadline: String
    lateinit var availableQuoteSidePaymentMethods: List<String>
    lateinit var availableBaseSidePaymentMethods: List<String>
    val selectedQuoteSidePaymentMethods: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())
    val selectedBaseSidePaymentMethods: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())

    private lateinit var createOfferModel: CreateOfferPresenter.CreateOfferModel
    lateinit var appStrings: AppStrings

    override fun onViewAttached() {
        createOfferModel = createOfferPresenter.createOfferModel

        val quoteCurrencyCode = createOfferModel.market!!.quoteCurrencyCode
        val isBuy = createOfferModel.direction.isBuy
        val bisqEasyStrings = appStrings.bisqEasy
        quoteSideHeadline = if (isBuy)
            bisqEasyStrings.bisqEasy_takeOffer_paymentMethods_subtitle_fiat_buyer(quoteCurrencyCode)
        else
            bisqEasyStrings.bisqEasy_takeOffer_paymentMethods_subtitle_fiat_seller(quoteCurrencyCode)

        baseSideHeadline = if (isBuy)
            bisqEasyStrings.bisqEasy_takeOffer_paymentMethods_subtitle_bitcoin_buyer
        else
            bisqEasyStrings.bisqEasy_takeOffer_paymentMethods_subtitle_bitcoin_seller

        // availableQuoteSidePaymentMethods = createOfferModel.availableQuoteSidePaymentMethods.subList(0, 3)  // for dev testing to avoid scroll
        availableQuoteSidePaymentMethods = createOfferModel.availableQuoteSidePaymentMethods
        availableBaseSidePaymentMethods = createOfferModel.availableBaseSidePaymentMethods
    }

    fun getQuoteSidePaymentMethodsImagePaths(): List<String> {
        return getPaymentMethodsImagePaths(availableQuoteSidePaymentMethods, "fiat")
    }

    fun getBaseSidePaymentMethodsImagePaths(): List<String> {
        return getPaymentMethodsImagePaths(availableBaseSidePaymentMethods, "bitcoin")
    }

    fun onToggleQuoteSidePaymentMethod(value: String) {
        if (selectedQuoteSidePaymentMethods.value.contains(value)) {
            selectedQuoteSidePaymentMethods.update { it - value }
        } else {
            selectedQuoteSidePaymentMethods.update { it + value }
        }
    }

    fun onToggleBaseSidePaymentMethod(value: String) {
        if (selectedBaseSidePaymentMethods.value.contains(value)) {
            selectedBaseSidePaymentMethods.update { it - value }
        } else {
            selectedBaseSidePaymentMethods.update { it + value }
        }
    }

    fun onBack() {
        commitToModel()
        navigateBack()
    }

    fun onNext() {
        if (isValid()) {
            commitToModel()
            navigateTo(Routes.CreateOfferReviewOffer)
        }
    }

    private fun commitToModel() {
        if (isValid()) {
            createOfferPresenter.commitPaymentMethod(selectedQuoteSidePaymentMethods.value, selectedBaseSidePaymentMethods.value)
        }
    }

    private fun isValid() = selectedQuoteSidePaymentMethods.value.isNotEmpty() && selectedBaseSidePaymentMethods.value.isNotEmpty()

    private fun getPaymentMethodsImagePaths(list: List<String>, directory: String) = list
        .map { paymentMethod ->
            val fileName = paymentMethod.lowercase().replace("-", "_")
            "drawable/payment/$directory/$fileName.png"
        }
}
