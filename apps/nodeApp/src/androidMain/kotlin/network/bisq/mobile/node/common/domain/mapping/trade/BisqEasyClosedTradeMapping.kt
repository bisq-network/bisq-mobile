package network.bisq.mobile.node.common.domain.mapping.trade

import bisq.trade.bisq_easy.protocol.BisqEasyClosedTrade
import bisq.user.reputation.ReputationService
import network.bisq.mobile.data.mapping.trade.toTradeOutcome
import network.bisq.mobile.domain.model.trade.ClosedTradeListItem
import network.bisq.mobile.node.common.domain.mapping.Mappings

fun BisqEasyClosedTrade.toClosedTradeListItem(reputationService: ReputationService): ClosedTradeListItem {
    val trade = trade()
    val contract = trade.contract
    val myUserProfile = myUserProfile()
    val peerUserProfile = peerUserProfile()
    val peersReputation = reputationService.getReputationScore(peerUserProfile.id)
    val mediator = contract.mediator.map(Mappings.UserProfileMapping::fromBisq2Model).orElse(null)

    return ClosedTradeListItem(
        tradeId = trade.id,
        peersUserProfile = Mappings.UserProfileMapping.fromBisq2Model(peerUserProfile),
        peersReputationScore = Mappings.ReputationScoreMapping.fromBisq2Model(peersReputation),
        mediatorUserProfile = mediator,
        myUserProfile = Mappings.UserProfileMapping.fromBisq2Model(myUserProfile),
        priceQuote = Mappings.PriceQuoteMapping.fromBisq2Model(trade.priceQuote),
        fiatPaymentMethod = contract.quoteSidePaymentMethodSpec.paymentMethodName,
        bitcoinSettlementMethod = contract.baseSidePaymentMethodSpec.paymentMethodName,
        isMaker = trade.isMaker,
        isBuyer = trade.isBuyer,
        outcome = Mappings.BisqEasyTradeStateMapping.fromBisq2Model(trade.tradeState).toTradeOutcome(),
        takeOfferDate = contract.takeOfferDate,
        tradeCompletedDate = trade.tradeCompletedDate.orElse(null),
        baseAmount = contract.baseSideAmount,
        quoteAmount = contract.quoteSideAmount,
        paymentAccountData = trade.paymentAccountData.get(),
        bitcoinPaymentData = trade.bitcoinPaymentData.get(),
        paymentProof = trade.paymentProof.get(),
    )
}
