package network.bisq.mobile.presentation.ui.uicases.offers.createOffer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bisq.mobile.client.replicated_model.offer.Direction
import network.bisq.mobile.client.replicated_model.user.reputation.ReputationScore
import network.bisq.mobile.domain.data.model.MarketListItem
import network.bisq.mobile.domain.data.model.OfferListItem
import network.bisq.mobile.domain.data.model.OfferbookMarket
import network.bisq.mobile.domain.service.offerbook.OfferbookServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

enum class AmountType {
    FIXED_AMOUNT,
    RANGE_AMOUNT,
}

enum class PriceType {
    PERCENTAGE,
    FIXED,
}


interface ICreateOfferPresenter : ViewPresenter {
    var marketListItemWithNumOffers: List<MarketListItem>

    val offerListItems: StateFlow<List<OfferListItem>>

    val state: StateFlow<CreateOffer>

    val direction: StateFlow<Direction>

    val fixedAmount: StateFlow<Float>

    fun onSelectMarket(marketListItem: MarketListItem)

    fun onSelectAmountType(amountType: AmountType)

    fun navigateToCurrencySelector()

    fun buyBitcoinClicked()

    fun sellBitcoinClicked()

    fun navigateToAmountSelector()

    fun navigateToTradePriceSelector()

    fun navigateToPaymentMethod()

    fun navigateToReviewOffer()

    fun createOffer()

    fun onSelectPriceType(priceType: PriceType)

    fun onSelectPayment(paymentMethod: String)

    fun onFixedAmountChange(amount: Float)

}

data class CreateOffer(
    val selectedAmountType: AmountType = AmountType.FIXED_AMOUNT,
    val selectedPriceType: PriceType = PriceType.PERCENTAGE,
    val selectedOfferbookMarket: OfferbookMarket = OfferbookMarket.EMPTY,
    val paymentMethod: String = ""
)

open class CreateOfferPresenter(
    mainPresenter: MainPresenter,
    private val offerbookServiceFacade: OfferbookServiceFacade,
) : BasePresenter(mainPresenter), ICreateOfferPresenter {

    override val offerListItems: StateFlow<List<OfferListItem>> = MutableStateFlow(
        listOf(
            OfferListItem(
                messageId = "12345",
                offerId = "abcde",
                isMyMessage = true,
                direction = Direction.BUY,
                offerTitle = "Sample Offer",
                date = 1638316800000,  // Example timestamp
                formattedDate = "Dec 5, 2024",
                nym = "user123",
                userName = "John Doe",
                reputationScore = ReputationScore.NONE,
                formattedQuoteAmount = "500 USD",
                formattedPrice = "1000 USD",
                quoteSidePaymentMethods = listOf("Amazon-Gift-Card", "ACH-Transfer", "Cash-App"),
                baseSidePaymentMethods = listOf("Main-Chain", "Ln"),
                supportedLanguageCodes = "en,es,de",
                quoteCurrencyCode = "USD",
            )
        )
    )
    private val _state = MutableStateFlow(CreateOffer())
    override val state = _state.asStateFlow()

    private val _direction = MutableStateFlow(Direction.BUY)
    override val direction: StateFlow<Direction> = _direction.asStateFlow()

    private val _fixedAmount = MutableStateFlow(0.0f)
    override val fixedAmount: StateFlow<Float> = _fixedAmount.asStateFlow()

    private var mainCurrencies = OfferbookServiceFacade.mainCurrencies

    override var marketListItemWithNumOffers: List<MarketListItem> =
        offerbookServiceFacade.offerbookMarketItems
            .sortedWith(
                compareByDescending<MarketListItem> { it.numOffers.value }
                    .thenByDescending { mainCurrencies.contains(it.market.quoteCurrencyCode.lowercase()) } // [1]
                    .thenBy { item ->
                        if (!mainCurrencies.contains(item.market.quoteCurrencyCode.lowercase())) item.market.quoteCurrencyName
                        else null // Null values will naturally be sorted together
                    }
            )

    // [1] thenBy doesn’t work as expected for boolean expressions because true and false are
    // sorted alphabetically (false before true), thus we use thenByDescending

    override fun onSelectMarket(marketListItem: MarketListItem) {
        _state.update {
            it.copy(selectedOfferbookMarket = OfferbookMarket(marketListItem.market))
        }
        log.i { state.value.selectedOfferbookMarket.market.quoteCurrencyCode }
    }

    override fun onSelectAmountType(amountType: AmountType) {
        _state.update {
            it.copy(selectedAmountType = amountType)
        }
    }

    override fun onViewAttached() {
    }

    override fun onViewUnattaching() {
    }

    override fun buyBitcoinClicked() {
        _direction.value = Direction.BUY
        navigateToCurrencySelector()
    }

    override fun sellBitcoinClicked() {
        _direction.value = Direction.SELL
        navigateToCurrencySelector()
    }

    override fun navigateToCurrencySelector() {
        log.i { "navigateToCurrencySelector" }

        CoroutineScope(Dispatchers.Main).launch {
            rootNavigator.navigate(Routes.CreateOfferCurrency.name)
        }
    }

    override fun navigateToAmountSelector() {
        log.i { "navigateToAmountSelector" }

        if (state.value.selectedOfferbookMarket == OfferbookMarket.EMPTY) {
            log.e { "A currency must be selected!" }
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            rootNavigator.navigate(Routes.CreateOfferAmount.name)
        }
    }

    override fun navigateToTradePriceSelector() {
        log.i { "navigateToTradePriceSelector" }

        CoroutineScope(Dispatchers.Main).launch {
            rootNavigator.navigate(Routes.CreateOfferTradePrice.name)
        }
    }

    override fun navigateToPaymentMethod() {
        log.i { "navigateToPaymentMethod" }

        CoroutineScope(Dispatchers.Main).launch {
            rootNavigator.navigate(Routes.CreateOfferPaymentMethod.name)
        }
    }

    override fun navigateToReviewOffer() {
        log.i { "navigateToReviewOffer" }

        CoroutineScope(Dispatchers.Main).launch {
            rootNavigator.navigate(Routes.CreateOfferReviewOffer.name)
        }
    }

    override fun createOffer() {
        log.i { "createOffer" }

        CoroutineScope(Dispatchers.Main).launch {
            rootNavigator.popBackStack(Routes.TabContainer.name, inclusive = false, saveState = false )
        }
    }

    override fun onSelectPriceType(priceType: PriceType) {
        _state.update {
            it.copy(selectedPriceType = priceType)
        }
        log.i { "priceType" }
    }

    override fun onSelectPayment(paymentMethod: String) {
        _state.update {
            it.copy(paymentMethod = paymentMethod)
        }
    }

    override fun onFixedAmountChange(amount: Float) {
        _fixedAmount.value = amount
        log.i { "Change fixed amount: ${amount.toString()}" }
    }
}
