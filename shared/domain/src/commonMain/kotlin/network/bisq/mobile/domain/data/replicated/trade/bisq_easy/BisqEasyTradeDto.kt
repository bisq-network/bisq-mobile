package network.bisq.mobile.domain.data.replicated.trade.bisq_easy

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.data.replicated.contract.BisqEasyContractVO
import network.bisq.mobile.domain.data.replicated.contract.RoleEnum
import network.bisq.mobile.domain.data.replicated.identity.IdentityVO
import network.bisq.mobile.domain.data.replicated.trade.TradeRoleEnum
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum

@Serializable
data class BisqEasyTradeDto(
    val contract: BisqEasyContractVO,
    val id: String,
    val tradeRole: TradeRoleEnum,
    val myIdentity: IdentityVO,
    val taker: BisqEasyTradePartyVO,
    val maker: BisqEasyTradePartyVO,
    // Below are mutual data which will be handled via websocket updates but are set with their initial state here
    val tradeState: BisqEasyTradeStateEnum, // tradeState is never null
    val paymentAccountData: String?,
    val bitcoinPaymentData: String?,
    val paymentProof: String?,
    val interruptTradeInitiator: RoleEnum?,
    val errorMessage: String?,
    val errorStackTrace: String?,
    val peersErrorMessage: String?,
    val peersErrorStackTrace: String?
)