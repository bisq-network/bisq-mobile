package network.bisq.mobile.domain.data.replicated.trade.bisq_easy

import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.domain.data.replicated.contract.BisqEasyContractVO
import network.bisq.mobile.domain.data.replicated.contract.RoleEnum
import network.bisq.mobile.domain.data.replicated.identity.IdentityVO
import network.bisq.mobile.domain.data.replicated.trade.TradeRoleEnum
import network.bisq.mobile.domain.data.replicated.trade.TradeRoleEnumExtensions.isMaker
import network.bisq.mobile.domain.data.replicated.trade.TradeRoleEnumExtensions.isSeller
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum


class BisqEasyTradeModel(bisqEasyTradeVO: BisqEasyTradeVO) {
    // Delegates of bisqEasyTradeVO
    val taker = bisqEasyTradeVO.taker
    val maker = bisqEasyTradeVO.maker
    val contract: BisqEasyContractVO = bisqEasyTradeVO.contract
    val id: String = bisqEasyTradeVO.id
    val tradeRole: TradeRoleEnum = bisqEasyTradeVO.tradeRole
    val myIdentity: IdentityVO = bisqEasyTradeVO.myIdentity

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
        get() = when (tradeRole) {
            TradeRoleEnum.BUYER_AS_TAKER -> taker
            TradeRoleEnum.BUYER_AS_MAKER -> maker
            TradeRoleEnum.SELLER_AS_TAKER -> maker
            TradeRoleEnum.SELLER_AS_MAKER -> taker
        }

    val seller: BisqEasyTradePartyVO
        get() = when (tradeRole) {
            TradeRoleEnum.BUYER_AS_TAKER -> maker
            TradeRoleEnum.BUYER_AS_MAKER -> taker
            TradeRoleEnum.SELLER_AS_TAKER -> taker
            TradeRoleEnum.SELLER_AS_MAKER -> maker
        }

    val shortId: String
        get() {
            return id.substring(0, 8)
        }

    // MutableStateFlow
    val tradeState: MutableStateFlow<BisqEasyTradeStateEnum> = MutableStateFlow(bisqEasyTradeVO.tradeState)

    // The role who cancelled or rejected the trade
    val interruptTradeInitiator: MutableStateFlow<RoleEnum?> = MutableStateFlow(bisqEasyTradeVO.interruptTradeInitiator)
    val paymentAccountData: MutableStateFlow<String?> = MutableStateFlow(bisqEasyTradeVO.paymentAccountData)

    // btc address in case of mainChain, or LN invoice if LN is used
    val bitcoinPaymentData: MutableStateFlow<String?> = MutableStateFlow(bisqEasyTradeVO.bitcoinPaymentData)

    // txId in case of mainChain, or preimage if LN is used
    val paymentProof: MutableStateFlow<String?> = MutableStateFlow(bisqEasyTradeVO.paymentProof)
    val errorMessage: MutableStateFlow<String?> = MutableStateFlow(bisqEasyTradeVO.errorMessage)
    val errorStackTrace: MutableStateFlow<String?> = MutableStateFlow(bisqEasyTradeVO.errorStackTrace)
    val peersErrorMessage: MutableStateFlow<String?> = MutableStateFlow(bisqEasyTradeVO.peersErrorMessage)
    val peersErrorStackTrace: MutableStateFlow<String?> = MutableStateFlow(bisqEasyTradeVO.peersErrorStackTrace)
}