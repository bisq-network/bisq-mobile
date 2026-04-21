package network.bisq.mobile.presentation.trade.trade_detail

import network.bisq.mobile.data.replicated.common.network.TransportTypeEnum
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.formatters.PriceSpecFormatter
import network.bisq.mobile.i18n.i18n

data class TradeDetailsHeaderTradeUiState(
    val directionalTitle: String,
    val peersUserProfile: UserProfileVO,
    val peersReputationScore: ReputationScoreVO,
    val priceDisplay: String,
    val formattedDate: String,
    val formattedTime: String,
    val fiatPaymentMethodDisplayString: String,
    val bitcoinSettlementMethodDisplayString: String,
    val shortTradeId: String,
    val tradeId: String,
    val mediatorUserName: String?,
    val isSell: Boolean,
    val isSmallScreen: Boolean,
    val leftAmountDescription: String,
    val rightAmountDescription: String,
    val leftAmount: String,
    val leftCode: String,
    val rightAmount: String,
    val rightCode: String,
    val isMainChainPayment: Boolean,
    val peerNetworkAddress: String?,
)

data class TradeDetailsHeaderSessionUiState(
    val showDetails: Boolean = false,
    val isInteractive: Boolean = true,
    val interruptTradeButtonText: String = "",
    val openMediationButtonText: String = "",
    val isInMediation: Boolean = false,
    val isCompleted: Boolean = false,
    val paymentProof: String? = null,
    val receiverAddress: String? = null,
    val formattedTradeDuration: String = "",
)

fun TradeItemPresentationModel.toHeaderTradeUiState(
    isSmallScreen: Boolean,
): TradeDetailsHeaderTradeUiState {
    val isSell = bisqEasyTradeModel.isSeller
    val leftAmountDescription =
        if (isSell) {
            "bisqEasy.tradeState.header.send".i18n()
        } else {
            "bisqEasy.tradeState.header.pay".i18n()
        }
    val leftAmount: String
    val leftCode: String
    val rightAmount: String
    val rightCode: String
    if (isSell) {
        leftAmount = formattedBaseAmount
        leftCode = baseCurrencyCode
        rightAmount = formattedQuoteAmount
        rightCode = quoteCurrencyCode
    } else {
        leftAmount = formattedQuoteAmount
        leftCode = quoteCurrencyCode
        rightAmount = formattedBaseAmount
        rightCode = baseCurrencyCode
    }
    return TradeDetailsHeaderTradeUiState(
        directionalTitle = directionalTitle,
        peersUserProfile = peersUserProfile,
        peersReputationScore = peersReputationScore,
        priceDisplay = PriceSpecFormatter.formatPriceWithSpec(formattedPrice, bisqEasyOffer.priceSpec),
        formattedDate = formattedDate,
        formattedTime = formattedTime,
        fiatPaymentMethodDisplayString = fiatPaymentMethodDisplayString,
        bitcoinSettlementMethodDisplayString = bitcoinSettlementMethodDisplayString,
        shortTradeId = shortTradeId,
        tradeId = tradeId,
        mediatorUserName = mediatorUserName,
        isSell = isSell,
        isSmallScreen = isSmallScreen,
        leftAmountDescription = leftAmountDescription,
        rightAmountDescription = "bisqEasy.tradeState.header.receive".i18n(),
        leftAmount = leftAmount,
        leftCode = leftCode,
        rightAmount = rightAmount,
        rightCode = rightCode,
        isMainChainPayment = bitcoinSettlementMethod == "MAIN_CHAIN",
        peerNetworkAddress = getPeerNetworkAddress(),
    )
}

private fun TradeItemPresentationModel.getPeerNetworkAddress(): String? =
    bisqEasyTradeModel.peer.networkId.addressByTransportTypeMap.map[TransportTypeEnum.TOR]
        ?.toString()
        ?: bisqEasyTradeModel.peer.networkId.addressByTransportTypeMap.map.values
            .firstOrNull()
            ?.toString()
