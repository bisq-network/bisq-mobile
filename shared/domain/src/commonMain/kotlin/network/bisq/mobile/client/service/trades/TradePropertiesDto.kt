package network.bisq.mobile.client.service.trades

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.data.replicated.contract.RoleEnum
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum

@Serializable
data class TradePropertiesDto(
    var tradeState: BisqEasyTradeStateEnum? = null,
    var interruptTradeInitiator: RoleEnum? = null,
    var paymentAccountData: String? = null,
    var bitcoinPaymentData: String? = null,
    var paymentProof: String? = null,
    var errorMessage: String? = null,
    var errorStackTrace: String? = null,
    var peersErrorMessage: String? = null,
    var peersErrorStackTrace: String? = null
)
