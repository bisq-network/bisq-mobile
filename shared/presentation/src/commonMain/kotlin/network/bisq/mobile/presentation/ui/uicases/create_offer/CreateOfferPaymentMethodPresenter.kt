package network.bisq.mobile.presentation.ui.uicases.create_offer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnumExtensions.isBuy
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

class CreateOfferPaymentMethodPresenter(
    mainPresenter: MainPresenter, private val createOfferPresenter: CreateOfferPresenter
) : BasePresenter(mainPresenter) {

    lateinit var quoteSideHeadline: String
    lateinit var baseSideHeadline: String
    lateinit var availableQuoteSidePaymentMethods: List<String>
    lateinit var availableBaseSidePaymentMethods: List<String>
    val selectedQuoteSidePaymentMethods: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())
    val selectedBaseSidePaymentMethods: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())

    private lateinit var createOfferModel: CreateOfferPresenter.CreateOfferModel

    init {
        createOfferModel = createOfferPresenter.createOfferModel

        val quoteCurrencyCode = createOfferModel.market?.quoteCurrencyCode
            ?: throw IllegalStateException("Market must be initialized before creating payment method presenter")

        val isBuy = createOfferModel.direction.isBuy
        quoteSideHeadline = if (isBuy) "bisqEasy.takeOffer.paymentMethods.subtitle.fiat.buyer".i18n(quoteCurrencyCode)
        else "bisqEasy.takeOffer.paymentMethods.subtitle.fiat.seller".i18n(quoteCurrencyCode)

        baseSideHeadline = if (isBuy) "bisqEasy.takeOffer.paymentMethods.subtitle.bitcoin.buyer".i18n()
        else "bisqEasy.takeOffer.paymentMethods.subtitle.bitcoin.seller".i18n()

        // availableQuoteSidePaymentMethods = createOfferModel.availableQuoteSidePaymentMethods.subList(0, 3)  // for dev testing to avoid scroll
        availableQuoteSidePaymentMethods = createOfferModel.availableQuoteSidePaymentMethods
        availableBaseSidePaymentMethods = createOfferModel.availableBaseSidePaymentMethods

        selectedQuoteSidePaymentMethods.value = createOfferModel.selectedQuoteSidePaymentMethods.toMutableSet()
        selectedBaseSidePaymentMethods.value = createOfferModel.selectedBaseSidePaymentMethods.toMutableSet()
    }

    override fun onViewUnattaching() {
        dismissSnackbar()
        super.onViewUnattaching()
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
            if (selectedQuoteSidePaymentMethods.value.size == 4) {
                showSnackbar("bisqEasy.tradeWizard.paymentMethods.warn.maxMethodsReached".i18n())
            } else {
                selectedQuoteSidePaymentMethods.update { it + value }
            }
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
        commitPaymentToModel()
        commitSettlementToModel()
        navigateBack()
    }

    fun onClose() {
        commitPaymentToModel()
        commitSettlementToModel()
        navigateToOfferList()
    }

    fun onQuoteSideNext() {
        if (isQuoteSideValid()) {
            commitPaymentToModel()
            navigateTo(Routes.CreateOfferBaseSidePaymentMethod)
        } else {
            showSnackbar("bisqEasy.tradeWizard.paymentMethods.warn.noFiatPaymentMethodSelected".i18n())
        }
    }

    fun onBaseSideNext() {
        if (isBaseSideValid()) {
            commitSettlementToModel()
            navigateTo(Routes.CreateOfferReviewOffer)
        } else {
            showSnackbar("bisqEasy.tradeWizard.paymentMethods.warn.noBtcSettlementMethodSelected".i18n())
        }
    }

    private fun navigateToOfferList() {
        navigateBackTo(Routes.TabContainer)
        navigateToTab(Routes.TabOfferbook)
    }

    private fun commitPaymentToModel() {
        if (isQuoteSideValid()) {
            createOfferPresenter.commitPaymentMethod(
                selectedQuoteSidePaymentMethods.value
            )
        }
    }

    private fun commitSettlementToModel() {
        if (isBaseSideValid()) {
            createOfferPresenter.commitSettlementMethod(
                selectedBaseSidePaymentMethods.value
            )
        }
    }


    private fun isQuoteSideValid() = selectedQuoteSidePaymentMethods.value.isNotEmpty()
    private fun isBaseSideValid() = selectedBaseSidePaymentMethods.value.isNotEmpty()

    private fun getPaymentMethodsImagePaths(list: List<String>, directory: String) = list.map { paymentMethod ->
            val fileName = paymentMethod.lowercase().replace("-", "_")
            "drawable/payment/$directory/$fileName.png"
        }
}
