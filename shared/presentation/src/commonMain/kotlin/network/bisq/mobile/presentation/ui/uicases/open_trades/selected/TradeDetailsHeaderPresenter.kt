package network.bisq.mobile.presentation.ui.uicases.open_trades.selected

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.domain.service.mediation.MediationServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter

@OptIn(ExperimentalCoroutinesApi::class)
class TradeDetailsHeaderPresenter(
    private val mainPresenter: MainPresenter,
    var tradesServiceFacade: TradesServiceFacade,
    var mediationServiceFacade: MediationServiceFacade,
    val userProfileServiceFacade: UserProfileServiceFacade,
) : BasePresenter(mainPresenter) {

    enum class TradeCloseType {
        REJECT,
        CANCEL,
        COMPLETED
    }

    private val _selectedTrade: MutableStateFlow<TradeItemPresentationModel?> =
        MutableStateFlow(tradesServiceFacade.selectedTrade.value)
    val selectedTrade: StateFlow<TradeItemPresentationModel?> = _selectedTrade

    var direction: String = ""
    var directionEnum: DirectionEnum = DirectionEnum.BUY
    var leftAmountDescription: String = ""
    private var _leftAmount: MutableStateFlow<String> = MutableStateFlow("")
    var leftAmount: StateFlow<String> = _leftAmount
    private var _leftCode: MutableStateFlow<String> = MutableStateFlow("")
    var leftCode: StateFlow<String> = _leftCode
    var rightAmountDescription: String = ""
    private var _rightAmount: MutableStateFlow<String> = MutableStateFlow("")
    var rightAmount: StateFlow<String> = _rightAmount
    private var _rightCode: MutableStateFlow<String> = MutableStateFlow("")
    var rightCode: StateFlow<String> = _rightCode

    private var _tradeCloseType: MutableStateFlow<TradeCloseType?> = MutableStateFlow(null)
    val tradeCloseType: StateFlow<TradeCloseType?> = _tradeCloseType

    private var _interruptTradeButtonText: MutableStateFlow<String> = MutableStateFlow("")
    val interruptTradeButtonText: StateFlow<String> = _interruptTradeButtonText

    private var _openMediationButtonText: MutableStateFlow<String> = MutableStateFlow("")
    val openMediationButtonText: StateFlow<String> = _openMediationButtonText

    private val _showInterruptionConfirmationDialog = MutableStateFlow(false)
    val showInterruptionConfirmationDialog: StateFlow<Boolean> get() = _showInterruptionConfirmationDialog

    private val _showMediationConfirmationDialog = MutableStateFlow(false)
    val showMediationConfirmationDialog: StateFlow<Boolean> get() = _showMediationConfirmationDialog

    private val _isInMediation: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isInMediation: StateFlow<Boolean> = this._isInMediation

    private val _peerAvatar: MutableStateFlow<PlatformImage?> = MutableStateFlow(null)
    val peerAvatar: StateFlow<PlatformImage?> = _peerAvatar

    override fun onViewAttached() {
        super.onViewAttached()

        launchUI {
            mainPresenter.languageCode
                .flatMapLatest { tradesServiceFacade.selectedTrade }
                .filterNotNull()
                .collect {
                    val formatted = it.reformat()
                    _selectedTrade.value = formatted

                    if (it.bisqEasyTradeModel.isSeller) {
                        _leftAmount.value = formatted.formattedBaseAmount
                        _leftCode.value = formatted.baseCurrencyCode
                        _rightAmount.value = formatted.formattedQuoteAmount
                        _rightCode.value = formatted.quoteCurrencyCode
                    } else {
                        _leftAmount.value = formatted.formattedQuoteAmount
                        _leftCode.value = formatted.quoteCurrencyCode
                        _rightAmount.value = formatted.formattedBaseAmount
                        _rightCode.value = formatted.baseCurrencyCode
                    }
                }
        }

        require(tradesServiceFacade.selectedTrade.value != null)
        val openTradeItemModel = tradesServiceFacade.selectedTrade.value!!

        if (openTradeItemModel.bisqEasyTradeModel.isSeller) {
            directionEnum = DirectionEnum.SELL
            direction = "SELL" //"offer.sell"
            leftAmountDescription = "Amount to send" //"bisqEasy.tradeState.header.send"
            rightAmountDescription = "Amount to receive" // "bisqEasy.tradeState.header.receive"
        } else {
            directionEnum = DirectionEnum.BUY
            direction = "BUY" //"offer.sell"
            leftAmountDescription = "Amount to pay" //"bisqEasy.tradeState.header.pay"
            rightAmountDescription = "Amount to receive" //"bisqEasy.tradeState.header.receive"
        }

        collectUI(openTradeItemModel.bisqEasyTradeModel.tradeState) {
            tradeStateChanged(it)
        }

        collectUI(openTradeItemModel.bisqEasyOpenTradeChannelModel.isInMediation) {
            this@TradeDetailsHeaderPresenter._isInMediation.value = it
        }

        launchIO {
            _peerAvatar.value = userProfileServiceFacade.getUserAvatar(openTradeItemModel.peersUserProfile)
        }

    }

    override fun onViewUnattaching() {
        reset()
        super.onViewUnattaching()
    }

    private fun tradeStateChanged(state: BisqEasyTradeStateEnum?) {
        _tradeCloseType.value = null
        _interruptTradeButtonText.value = ""
        _openMediationButtonText.value = ""

        if (state == null) {
            return
        }

        when (state) {
            BisqEasyTradeStateEnum.INIT,
            BisqEasyTradeStateEnum.TAKER_SENT_TAKE_OFFER_REQUEST,
            BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,
            BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,
            BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
            BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,
            BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
            BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,
            BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA -> {
                // Before account data are exchange we use `Report to mediator`, after that `Request mediation`
                _openMediationButtonText.value = "bisqEasy.tradeState.reportToMediator".i18n() // Report to mediator
            }

            BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
            BisqEasyTradeStateEnum.TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,
            BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA_,
            BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,

            BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
            BisqEasyTradeStateEnum.MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,
            BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS_,
            BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,

            BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
            BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,
            BisqEasyTradeStateEnum.TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
            BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS_,
            BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,

            BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
            BisqEasyTradeStateEnum.MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
            BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA_,
            BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,

            BisqEasyTradeStateEnum.BUYER_SENT_FIAT_SENT_CONFIRMATION,
            BisqEasyTradeStateEnum.SELLER_RECEIVED_FIAT_SENT_CONFIRMATION,
            BisqEasyTradeStateEnum.SELLER_CONFIRMED_FIAT_RECEIPT,
            BisqEasyTradeStateEnum.BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION,
            BisqEasyTradeStateEnum.SELLER_SENT_BTC_SENT_CONFIRMATION,
            BisqEasyTradeStateEnum.BUYER_RECEIVED_BTC_SENT_CONFIRMATION -> {
                _openMediationButtonText.value = "bisqEasy.tradeState.requestMediation".i18n() //Request mediator
            }

            BisqEasyTradeStateEnum.BTC_CONFIRMED,
            BisqEasyTradeStateEnum.REJECTED,
            BisqEasyTradeStateEnum.PEER_REJECTED,
            BisqEasyTradeStateEnum.CANCELLED,
            BisqEasyTradeStateEnum.PEER_CANCELLED,
            BisqEasyTradeStateEnum.FAILED,
            BisqEasyTradeStateEnum.FAILED_AT_PEER -> {
            }
        }

        when (state) {
            BisqEasyTradeStateEnum.INIT,
            BisqEasyTradeStateEnum.TAKER_SENT_TAKE_OFFER_REQUEST,
            BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,
            BisqEasyTradeStateEnum.MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
            BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
            BisqEasyTradeStateEnum.MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,
            BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
            BisqEasyTradeStateEnum.TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
            BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,
            BisqEasyTradeStateEnum.TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA -> {
                _tradeCloseType.value = TradeCloseType.REJECT
                _interruptTradeButtonText.value = "bisqEasy.openTrades.rejectTrade".i18n() // Reject trade
            }

            BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,

            BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
            BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS_,
            BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,
            BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
            BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS_,
            BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,
            BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,
            BisqEasyTradeStateEnum.SELLER_RECEIVED_FIAT_SENT_CONFIRMATION,
            BisqEasyTradeStateEnum.SELLER_CONFIRMED_FIAT_RECEIPT,
            BisqEasyTradeStateEnum.SELLER_SENT_BTC_SENT_CONFIRMATION,
            BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,
            BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA_,
            BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
            BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
            BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,
            BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
            BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA_,
            BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
            BisqEasyTradeStateEnum.BUYER_SENT_FIAT_SENT_CONFIRMATION,
            BisqEasyTradeStateEnum.BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION,
            BisqEasyTradeStateEnum.BUYER_RECEIVED_BTC_SENT_CONFIRMATION -> {
                _tradeCloseType.value = TradeCloseType.CANCEL
                _interruptTradeButtonText.value = "bisqEasy.openTrades.cancelTrade".i18n()
            }

            BisqEasyTradeStateEnum.BTC_CONFIRMED -> {
                _tradeCloseType.value = TradeCloseType.COMPLETED
            }

            BisqEasyTradeStateEnum.REJECTED,
            BisqEasyTradeStateEnum.PEER_REJECTED,
            BisqEasyTradeStateEnum.CANCELLED,
            BisqEasyTradeStateEnum.PEER_CANCELLED,
            BisqEasyTradeStateEnum.FAILED,
            BisqEasyTradeStateEnum.FAILED_AT_PEER -> {
            }
        }
    }

    fun onOpenInterruptionConfirmationDialog() {
        _showInterruptionConfirmationDialog.value = true
    }

    fun onCloseInterruptionConfirmationDialog() {
        _showInterruptionConfirmationDialog.value = false
    }

    fun onInterruptTrade() {
        require(selectedTrade.value != null)

        launchUI {
            val result: Result<Unit>? = withContext(IODispatcher) {
                when (tradeCloseType.value) {
                    TradeCloseType.REJECT -> {
                        tradesServiceFacade.rejectTrade()
                    }

                    TradeCloseType.CANCEL -> {
                        tradesServiceFacade.cancelTrade()
                    }

                    else -> {
                        null
                    }
                }
            }

            if (result != null) {
                when {
                    result.isFailure -> _showInterruptionConfirmationDialog.value = false
                    result.isSuccess -> _showInterruptionConfirmationDialog.value = false
                }
            }
        }
    }

    fun onOpenMediationConfirmationDialog() {
        _showMediationConfirmationDialog.value = true
    }

    fun onCloseMediationConfirmationDialog() {
        _showMediationConfirmationDialog.value = false
    }

    fun onOpenMediation() {
        if (!isInteractive.value) return
        disableInteractive()
        _showMediationConfirmationDialog.value = false
        launchIO {
            try {
                mediationServiceFacade.reportToMediator(selectedTrade.value!!)
            } catch (e: Exception) {
                // TODO we probably want a UI for this
                showSnackbar("Mediation reporting failed, please reach out to support") // TODO i18n
                log.e(e) { "Failed to proceed to report to mediation - ${e.message}" }
            } finally {
                enableInteractive()
            }
        }
    }

    private fun reset() {

        direction = ""
        leftAmountDescription = ""
        _leftAmount.value = ""
        _leftCode.value = ""
        rightAmountDescription = ""
        _rightAmount.value = ""
        _rightCode.value = ""

        _tradeCloseType.value = null
        _isInMediation.value = false
        _interruptTradeButtonText.value = ""
        _openMediationButtonText.value = ""
        _showInterruptionConfirmationDialog.value = false
        _showMediationConfirmationDialog.value = false
        // Intentionally not resetting _selectedTrade to maintain trade context between view attach/detach cycles
        // _selectedTrade.value = null
    }
}
