package network.bisq.mobile.client.common.data.mapping.trade

import network.bisq.mobile.data.mapping.trade.toTradeOutcome
import network.bisq.mobile.data.model.trade.ClosedTradeListItemDto
import network.bisq.mobile.data.replicated.trade.TradeRoleEnumExtensions.isMaker
import network.bisq.mobile.domain.model.trade.ClosedTradeListItem

fun ClosedTradeListItemDto.toClosedTradeListItem(): ClosedTradeListItem {
    val tradeId = trade.id
    val isMaker = trade.tradeRole.isMaker
    val peersUserProfile = if (isMaker) takerUserProfile else makerUserProfile
    val myUserProfile = if (isMaker) makerUserProfile else takerUserProfile
    return ClosedTradeListItem(
        tradeId = tradeId,
        peersUserProfile = peersUserProfile,
        peersReputationScore = peersReputationScore,
        mediatorUserProfile = mediatorUserProfile,
        myUserProfile = myUserProfile,
        priceQuote = priceQuote,
        fiatPaymentMethod = fiatPaymentMethod,
        bitcoinSettlementMethod = bitcoinSettlementMethod,
        isMaker = isMaker,
        isBuyer = trade.tradeRole.isBuyer,
        outcome = trade.tradeState.toTradeOutcome(),
        takeOfferDate = trade.contract.takeOfferDate,
        tradeCompletedDate = tradeCompletedDate,
        baseAmount = baseAmount,
        quoteAmount = quoteAmount,
        paymentAccountData = paymentAccountData,
        bitcoinPaymentData = bitcoinPaymentData,
        paymentProof = paymentProof,
    )
}
