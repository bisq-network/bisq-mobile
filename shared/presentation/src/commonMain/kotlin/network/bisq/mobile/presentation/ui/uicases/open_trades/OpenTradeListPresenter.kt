package network.bisq.mobile.presentation.ui.uicases.open_trades

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.formatters.NumberFormatter
import network.bisq.mobile.domain.formatters.PriceSpecFormatter
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

class OpenTradeListPresenter(
    mainPresenter: MainPresenter,
    private val tradesServiceFacade: TradesServiceFacade,
    private val settingsServiceFacade: SettingsServiceFacade
) : BasePresenter(mainPresenter) {

    private val _openTradeItems: MutableStateFlow<List<TradeItemPresentationModel>> = MutableStateFlow(emptyList())
    val openTradeItems: StateFlow<List<TradeItemPresentationModel>> = _openTradeItems

    val tradeRulesConfirmed: StateFlow<Boolean> = settingsServiceFacade.tradeRulesConfirmed

    private val _tradeGuideVisible = MutableStateFlow(false)
    val tradeGuideVisible: StateFlow<Boolean> get() = _tradeGuideVisible

    init {
        presenterScope.launch {
            mainPresenter.languageCode.collect {
                _openTradeItems.value = tradesServiceFacade.openTradeItems.value.map {
                    it.apply {
                        quoteAmountWithCode = "${NumberFormatter.format(it.quoteAmount.toDouble() / 10000.0)} ${it.quoteCurrencyCode}"
                        formattedPrice = PriceSpecFormatter.getFormattedPriceSpec(it.bisqEasyOffer.priceSpec, true)
                        formattedBaseAmount = NumberFormatter.btcFormat(it.baseAmount)
                    }
                }
            }
        }
    }

    override fun onViewAttached() {
        super.onViewAttached()
    }

    fun onOpenTradeGuide() {
        // _tradeGuideVisible.value = true
        navigateTo(Routes.TradeGuideOverview)
    }

    fun onCloseTradeGuide() {
        _tradeGuideVisible.value = false
    }

    fun onConfirmTradeRules(value: Boolean) {
        _tradeGuideVisible.value = false
        this.presenterScope.launch {
            settingsServiceFacade.confirmTradeRules(value)
        }
    }

    fun onSelect(openTradeItem: TradeItemPresentationModel) {
        if (tradeRulesConfirmed.value) {
            tradesServiceFacade.selectOpenTrade(openTradeItem.tradeId)
            navigateTo(Routes.OpenTrade)
        } else {
            log.w { "User hasn't accepted trade rules yet, showing dialog" }
            // TODO show dialogue to open trade guide
            _tradeGuideVisible.value = true
        }
    }

    fun onNavigateToOfferbook() {
        navigateToTab(Routes.TabOfferbook)
    }
}