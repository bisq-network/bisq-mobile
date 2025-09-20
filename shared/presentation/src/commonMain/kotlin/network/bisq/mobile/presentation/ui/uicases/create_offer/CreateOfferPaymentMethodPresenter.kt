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

    var quoteSideHeadline: String
    var baseSideHeadline: String
    val availableQuoteSidePaymentMethods: MutableStateFlow<Set<String>> = MutableStateFlow((emptySet()))
    val availableBaseSidePaymentMethods: MutableStateFlow<Set<String>> = MutableStateFlow((emptySet()))
    val selectedQuoteSidePaymentMethods: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())
    val selectedBaseSidePaymentMethods: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())

    private val createOfferModel: CreateOfferPresenter.CreateOfferModel = createOfferPresenter.createOfferModel

    init {

        val quoteCurrencyCode = createOfferModel.market?.quoteCurrencyCode
            ?: throw IllegalStateException("Market must be initialized before creating payment method presenter")

        val isBuy = createOfferModel.direction.isBuy
        quoteSideHeadline = if (isBuy) "bisqEasy.takeOffer.paymentMethods.subtitle.fiat.buyer".i18n(quoteCurrencyCode)
        else "bisqEasy.takeOffer.paymentMethods.subtitle.fiat.seller".i18n(quoteCurrencyCode)

        baseSideHeadline = if (isBuy) "bisqEasy.takeOffer.paymentMethods.subtitle.bitcoin.buyer".i18n()
        else "bisqEasy.takeOffer.paymentMethods.subtitle.bitcoin.seller".i18n()

        // availableQuoteSidePaymentMethods.value = createOfferModel.availableQuoteSidePaymentMethods.subList(0, 3).toSet()  // for dev testing to avoid scroll
        availableQuoteSidePaymentMethods.value = createOfferModel.availableQuoteSidePaymentMethods.toSet()
        availableBaseSidePaymentMethods.value = createOfferModel.availableBaseSidePaymentMethods.toSet()

        selectedQuoteSidePaymentMethods.value = createOfferModel.selectedQuoteSidePaymentMethods.toSet()
        selectedBaseSidePaymentMethods.value = createOfferModel.selectedBaseSidePaymentMethods.toSet()

        availableQuoteSidePaymentMethods.update { it + selectedQuoteSidePaymentMethods.value }
    }

    override fun onViewUnattaching() {
        dismissSnackbar()
        super.onViewUnattaching()
    }

    fun getQuoteSidePaymentMethodsImagePaths(): List<String> {
        return getPaymentMethodsImagePaths(availableQuoteSidePaymentMethods.value.toList(), "fiat")
    }

    fun getBaseSidePaymentMethodsImagePaths(): List<String> {
        return getPaymentMethodsImagePaths(availableBaseSidePaymentMethods.value.toList(), "bitcoin")
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

    fun addCustomPayment(value: String) {
        val normalized = value.trim()
        if (normalized.isEmpty()) return
        val available = availableQuoteSidePaymentMethods.value
        val existsIgnoringCase = available.any { it.equals(normalized, ignoreCase = true) }

        if (!existsIgnoringCase && selectedQuoteSidePaymentMethods.value.size >= 4) return
        if (!existsIgnoringCase) {
            availableQuoteSidePaymentMethods.update { it + normalized }
        }
    }

    fun removeCustomPayment(value: String) {
        val normalized = value.trim()
        val target = availableQuoteSidePaymentMethods.value
            .firstOrNull { it.equals(normalized, ignoreCase = true) }
            ?: return
        availableQuoteSidePaymentMethods.update { it - target }
        if (selectedQuoteSidePaymentMethods.value.contains(target)) {
            selectedQuoteSidePaymentMethods.update { it - target }
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
        navigateToOfferbookTab()
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
