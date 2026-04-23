package network.bisq.mobile.presentation.trade.trade_detail

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.service.mediation.MediationServiceFacade
import network.bisq.mobile.data.service.offers.MediatorNotAvailableException
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.domain.formatters.TradeDurationFormatter
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.main.MainPresenter

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
        COMPLETED,
    }

    val selectedTrade: StateFlow<TradeItemPresentationModel?> get() = tradesServiceFacade.selectedTrade

    var directionEnum: DirectionEnum = DirectionEnum.BUY

    private val _tradeCloseType: MutableStateFlow<TradeCloseType?> = MutableStateFlow(null)
    val tradeCloseType: StateFlow<TradeCloseType?> = _tradeCloseType.asStateFlow()

    private val _interruptTradeButtonText: MutableStateFlow<String> = MutableStateFlow("")
    val interruptTradeButtonText: StateFlow<String> = _interruptTradeButtonText.asStateFlow()

    private val _openMediationButtonText: MutableStateFlow<String> = MutableStateFlow("")
    val openMediationButtonText: StateFlow<String> = _openMediationButtonText.asStateFlow()

    private val _showInterruptionConfirmationDialog = MutableStateFlow(false)
    val showInterruptionConfirmationDialog: StateFlow<Boolean> = _showInterruptionConfirmationDialog.asStateFlow()

    private val _showMediationConfirmationDialog = MutableStateFlow(false)
    val showMediationConfirmationDialog: StateFlow<Boolean> = _showMediationConfirmationDialog.asStateFlow()

    private val _isInMediation: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isInMediation: StateFlow<Boolean> = _isInMediation.asStateFlow()

    private val _mediationError = MutableStateFlow("")
    val mediationError: StateFlow<String> = _mediationError.asStateFlow()

    private val _tradeUiState = MutableStateFlow<TradeDetailsHeaderTradeUiState?>(null)
    val tradeUiState: StateFlow<TradeDetailsHeaderTradeUiState?> = _tradeUiState.asStateFlow()

    private val _sessionUiState = MutableStateFlow(TradeDetailsHeaderSessionUiState())
    val sessionUiState: StateFlow<TradeDetailsHeaderSessionUiState> = _sessionUiState.asStateFlow()

    val userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage get() = userProfileServiceFacade::getUserProfileIcon

    override fun onViewAttached() {
        super.onViewAttached()

        presenterScope.launch {
            combine(
                mainPresenter.languageCode,
                mainPresenter.isSmallScreen,
                tradesServiceFacade.selectedTrade,
            ) { _, isSmall, trade ->
                trade?.toHeaderTradeUiState(isSmall)
            }.distinctUntilChanged()
                .collect { state ->
                    _tradeUiState.value = state
                }
        }

        require(tradesServiceFacade.selectedTrade.value != null)
        val openTradeItemModel = tradesServiceFacade.selectedTrade.value!!

        if (openTradeItemModel.bisqEasyTradeModel.isSeller) {
            directionEnum = DirectionEnum.SELL
        } else {
            directionEnum = DirectionEnum.BUY
        }

        presenterScope.launch {
            openTradeItemModel.bisqEasyTradeModel.tradeState.collect {
                tradeStateChanged(it)
            }
        }

        presenterScope.launch {
            openTradeItemModel.bisqEasyOpenTradeChannelModel.isInMediation.collect {
                this@TradeDetailsHeaderPresenter._isInMediation.value = it
            }
        }

        presenterScope.launch {
            val paymentProofFlow =
                tradesServiceFacade.selectedTrade.flatMapLatest { trade ->
                    trade?.bisqEasyTradeModel?.paymentProof ?: flowOf(null)
                }
            val receiverAddressFlow =
                tradesServiceFacade.selectedTrade.flatMapLatest { trade ->
                    trade?.bisqEasyTradeModel?.bitcoinPaymentData ?: flowOf(null)
                }
            val actionsFlow =
                combine(interruptTradeButtonText, openMediationButtonText, isInMediation, tradeCloseType) { interruptTradeButtonText, openMediationButtonText, isInMediation, tradeCloseType ->
                    Actions(interruptTradeButtonText, openMediationButtonText, isInMediation, tradeCloseType)
                }
            val paymentDataFlow =
                combine(paymentProofFlow, receiverAddressFlow) { paymentProof, receiverAddress ->
                    PaymentData(paymentProof, receiverAddress)
                }
            val formattedTradeDurationFlow =
                tradesServiceFacade.selectedTrade.flatMapLatest { trade ->
                    if (trade == null) {
                        flowOf("")
                    } else {
                        val takeOfferDate = trade.bisqEasyTradeModel.takeOfferDate
                        combine(
                            mainPresenter.languageCode,
                            trade.bisqEasyTradeModel.tradeState,
                            trade.bisqEasyTradeModel.tradeCompletedDate,
                        ) { _: String, _: BisqEasyTradeStateEnum, completedDate: Long? ->
                            TradeDurationFormatter.formatAge(
                                tradeCompletedDate = completedDate,
                                takeOfferDate = takeOfferDate,
                            )
                        }
                    }
                }
            combine(actionsFlow, paymentDataFlow, formattedTradeDurationFlow, isInteractive) { actions, payment, formattedTradeDuration, interactive ->
                _sessionUiState.update { prev ->
                    prev.copy(
                        showDetails = prev.showDetails,
                        isInteractive = interactive,
                        interruptTradeButtonText = actions.interruptTradeButtonText,
                        openMediationButtonText = actions.openMediationButtonText,
                        isInMediation = actions.isInMediation,
                        isCompleted = actions.tradeCloseType == TradeCloseType.COMPLETED,
                        paymentProof = payment.paymentProof,
                        receiverAddress = payment.receiverAddress,
                        formattedTradeDuration = formattedTradeDuration,
                    )
                }
            }.collect { }
        }
    }

    fun onAction(action: TradeDetailsHeaderUiAction) {
        when (action) {
            TradeDetailsHeaderUiAction.ToggleHeader -> onToggleHeader()
            TradeDetailsHeaderUiAction.OpenInterruptionConfirmationDialog -> onOpenInterruptionConfirmationDialog()
            TradeDetailsHeaderUiAction.OpenMediationConfirmationDialog -> onOpenMediationConfirmationDialog()
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
            BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,
            -> {
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
            BisqEasyTradeStateEnum.BUYER_RECEIVED_BTC_SENT_CONFIRMATION,
            -> {
                _openMediationButtonText.value = "bisqEasy.tradeState.requestMediation".i18n() // Request mediator
            }

            BisqEasyTradeStateEnum.BTC_CONFIRMED,
            BisqEasyTradeStateEnum.REJECTED,
            BisqEasyTradeStateEnum.PEER_REJECTED,
            BisqEasyTradeStateEnum.CANCELLED,
            BisqEasyTradeStateEnum.PEER_CANCELLED,
            BisqEasyTradeStateEnum.FAILED,
            BisqEasyTradeStateEnum.FAILED_AT_PEER,
            -> {
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
            BisqEasyTradeStateEnum.TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,
            -> {
                _tradeCloseType.value = TradeCloseType.REJECT
                _interruptTradeButtonText.value = "bisqEasy.openTrades.rejectTrade".i18n() // Reject trade
            }

            BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,

            BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS_,
            BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
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
            BisqEasyTradeStateEnum.BUYER_RECEIVED_BTC_SENT_CONFIRMATION,
            -> {
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
            BisqEasyTradeStateEnum.FAILED_AT_PEER,
            -> {
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
        _showInterruptionConfirmationDialog.value = false
        if (selectedTrade.value == null) {
            return
        }
        presenterScope.launch {
            showLoading()
            when (tradeCloseType.value) {
                TradeCloseType.REJECT -> {
                    tradesServiceFacade
                        .rejectTrade()
                        .onFailure { exception ->
                            handleError(exception)
                        }
                }

                TradeCloseType.CANCEL -> {
                    tradesServiceFacade
                        .cancelTrade()
                        .onFailure { exception ->
                            handleError(exception)
                        }
                }

                else -> Unit
            }
            _showInterruptionConfirmationDialog.value = false
            hideLoading()
        }
    }

    fun onOpenMediationConfirmationDialog() {
        _showMediationConfirmationDialog.value = true
    }

    fun onCloseMediationConfirmationDialog() {
        _showMediationConfirmationDialog.value = false
    }

    fun onOpenMediation() {
        val trade = selectedTrade.value
        if (trade == null) {
            _mediationError.value = "mobile.bisqEasy.tradeState.mediationFailed".i18n()
            return
        }
        _showMediationConfirmationDialog.value = false
        presenterScope.launch {
            showLoading()
            mediationServiceFacade
                .reportToMediator(trade)
                .onFailure { exception ->
                    when (exception) {
                        is MediatorNotAvailableException -> {
                            _mediationError.value =
                                "mobile.takeOffer.noMediatorAvailable.warning".i18n()
                        }

                        else -> {
                            _mediationError.value =
                                "mobile.bisqEasy.tradeState.mediationFailed".i18n()
                        }
                    }
                }
            hideLoading()
        }
    }

    fun onCloseMediationErrorDialog() {
        _mediationError.value = ""
    }

    fun onToggleHeader() {
        disableInteractive()
        _sessionUiState.update { it.copy(showDetails = !it.showDetails) }
        enableInteractive()
    }

    private fun reset() {
        _tradeCloseType.value = null
        _isInMediation.value = false
        _interruptTradeButtonText.value = ""
        _openMediationButtonText.value = ""
        _showInterruptionConfirmationDialog.value = false
        _showMediationConfirmationDialog.value = false
        _mediationError.value = ""
        _tradeUiState.value = null
        _sessionUiState.value = TradeDetailsHeaderSessionUiState()
        // Intentionally not resetting _selectedTrade to maintain trade context between view attach/detach cycles
        // _selectedTrade.value = null
    }

    private data class Actions(
        val interruptTradeButtonText: String,
        val openMediationButtonText: String,
        val isInMediation: Boolean,
        val tradeCloseType: TradeCloseType?,
    )

    private data class PaymentData(
        val paymentProof: String?,
        val receiverAddress: String?,
    )
}
