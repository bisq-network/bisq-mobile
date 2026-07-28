package network.bisq.mobile.data.replicated.trade.bisq_easy

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.data.replicated.contract.BisqEasyContractVO
import network.bisq.mobile.data.replicated.contract.RoleEnum
import network.bisq.mobile.data.replicated.identity.IdentityVO
import network.bisq.mobile.data.replicated.trade.TradeRoleEnum
import network.bisq.mobile.data.replicated.trade.TradeRoleEnumExtensions.isMaker
import network.bisq.mobile.data.replicated.trade.TradeRoleEnumExtensions.isSeller
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum

class BisqEasyTradeModel(
    bisqEasyTradeDto: BisqEasyTradeDto,
) {
    // Delegates of bisqEasyTradeVO
    val taker = bisqEasyTradeDto.taker
    val maker = bisqEasyTradeDto.maker
    val contract: BisqEasyContractVO = bisqEasyTradeDto.contract
    val id: String = bisqEasyTradeDto.id
    val tradeRole: TradeRoleEnum = bisqEasyTradeDto.tradeRole
    val myIdentity: IdentityVO = bisqEasyTradeDto.myIdentity

    // Delegates of BisqEasyContractVO
    val offer = contract.offer
    val takeOfferDate = contract.takeOfferDate

    // Delegates of TradeRoleEnum
    val isBuyer = tradeRole.isBuyer
    val isSeller = tradeRole.isSeller
    val isMaker = tradeRole.isMaker
    val isTaker = tradeRole.isTaker

    // Utils
    val peer: BisqEasyTradePartyVO get() = if (tradeRole.isTaker) maker else taker
    val myself: BisqEasyTradePartyVO get() = if (tradeRole.isTaker) taker else maker

    val buyer: BisqEasyTradePartyVO
        get() =
            when (tradeRole) {
                TradeRoleEnum.BUYER_AS_TAKER -> taker
                TradeRoleEnum.BUYER_AS_MAKER -> maker
                TradeRoleEnum.SELLER_AS_TAKER -> maker
                TradeRoleEnum.SELLER_AS_MAKER -> taker
            }

    val seller: BisqEasyTradePartyVO
        get() =
            when (tradeRole) {
                TradeRoleEnum.BUYER_AS_TAKER -> maker
                TradeRoleEnum.BUYER_AS_MAKER -> taker
                TradeRoleEnum.SELLER_AS_TAKER -> taker
                TradeRoleEnum.SELLER_AS_MAKER -> maker
            }

    val shortId: String
        get() {
            return id.substring(0, 8)
        }

    // Mutable trade data. Backed by private MutableStateFlow and exposed as read-only StateFlow;
    // updates go through the setters below so state can only be mutated via the owning model.
    // Kept as per-field flows (rather than a single snapshot) to preserve fine-grained observation:
    // a consumer watching only tradeState is not notified when e.g. errorStackTrace changes.
    private val _tradeState: MutableStateFlow<BisqEasyTradeStateEnum> = MutableStateFlow(bisqEasyTradeDto.tradeState)
    val tradeState: StateFlow<BisqEasyTradeStateEnum> = _tradeState.asStateFlow()

    // The role who cancelled or rejected the trade
    private val _interruptTradeInitiator: MutableStateFlow<RoleEnum?> = MutableStateFlow(bisqEasyTradeDto.interruptTradeInitiator)
    val interruptTradeInitiator: StateFlow<RoleEnum?> = _interruptTradeInitiator.asStateFlow()

    private val _paymentAccountData: MutableStateFlow<String?> = MutableStateFlow(bisqEasyTradeDto.paymentAccountData)
    val paymentAccountData: StateFlow<String?> = _paymentAccountData.asStateFlow()

    // btc address in case of mainChain, or LN invoice if LN is used
    private val _bitcoinPaymentData: MutableStateFlow<String?> = MutableStateFlow(bisqEasyTradeDto.bitcoinPaymentData)
    val bitcoinPaymentData: StateFlow<String?> = _bitcoinPaymentData.asStateFlow()

    // txId in case of mainChain, or preimage if LN is used
    private val _paymentProof: MutableStateFlow<String?> = MutableStateFlow(bisqEasyTradeDto.paymentProof)
    val paymentProof: StateFlow<String?> = _paymentProof.asStateFlow()

    private val _errorMessage: MutableStateFlow<String?> = MutableStateFlow(bisqEasyTradeDto.errorMessage)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _errorStackTrace: MutableStateFlow<String?> = MutableStateFlow(bisqEasyTradeDto.errorStackTrace)
    val errorStackTrace: StateFlow<String?> = _errorStackTrace.asStateFlow()

    private val _peersErrorMessage: MutableStateFlow<String?> = MutableStateFlow(bisqEasyTradeDto.peersErrorMessage)
    val peersErrorMessage: StateFlow<String?> = _peersErrorMessage.asStateFlow()

    private val _peersErrorStackTrace: MutableStateFlow<String?> = MutableStateFlow(bisqEasyTradeDto.peersErrorStackTrace)
    val peersErrorStackTrace: StateFlow<String?> = _peersErrorStackTrace.asStateFlow()

    private val _tradeCompletedDate: MutableStateFlow<Long?> = MutableStateFlow(bisqEasyTradeDto.tradeCompletedDate)
    val tradeCompletedDate: StateFlow<Long?> = _tradeCompletedDate.asStateFlow()

    fun setTradeState(value: BisqEasyTradeStateEnum) {
        _tradeState.value = value
    }

    fun setInterruptTradeInitiator(value: RoleEnum?) {
        _interruptTradeInitiator.value = value
    }

    fun setPaymentAccountData(value: String?) {
        _paymentAccountData.value = value
    }

    fun setBitcoinPaymentData(value: String?) {
        _bitcoinPaymentData.value = value
    }

    fun setPaymentProof(value: String?) {
        _paymentProof.value = value
    }

    fun setErrorMessage(value: String?) {
        _errorMessage.value = value
    }

    fun setErrorStackTrace(value: String?) {
        _errorStackTrace.value = value
    }

    fun setPeersErrorMessage(value: String?) {
        _peersErrorMessage.value = value
    }

    fun setPeersErrorStackTrace(value: String?) {
        _peersErrorStackTrace.value = value
    }

    fun setTradeCompletedDate(value: Long?) {
        _tradeCompletedDate.value = value
    }
}
