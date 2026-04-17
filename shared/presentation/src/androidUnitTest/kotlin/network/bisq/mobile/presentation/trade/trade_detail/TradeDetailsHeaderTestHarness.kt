package network.bisq.mobile.presentation.trade.trade_detail

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelModel
import network.bisq.mobile.data.replicated.common.network.AddressByTransportTypeMapVO
import network.bisq.mobile.data.replicated.common.network.AddressVO
import network.bisq.mobile.data.replicated.common.network.TransportTypeEnum
import network.bisq.mobile.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.security.keys.PubKeyVO
import network.bisq.mobile.data.replicated.security.keys.PublicKeyVO
import network.bisq.mobile.data.replicated.trade.bisq_easy.BisqEasyTradeModel
import network.bisq.mobile.data.replicated.trade.bisq_easy.BisqEasyTradePartyVO
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO

internal data class TradeDetailsHeaderTestHarness(
    val selectedTrade: MutableStateFlow<TradeItemPresentationModel?>,
    val tradeStateFlow: MutableStateFlow<BisqEasyTradeStateEnum>,
    val isInMediationFlow: MutableStateFlow<Boolean>,
    val paymentProofFlow: MutableStateFlow<String?>,
    val bitcoinPaymentDataFlow: MutableStateFlow<String?>,
    val tradeCompletedDateFlow: MutableStateFlow<Long?>,
)

internal fun createTradeDetailsHeaderTestHarness(isSeller: Boolean): TradeDetailsHeaderTestHarness {
    val tradeStateFlow = MutableStateFlow(BisqEasyTradeStateEnum.INIT)
    val isInMediationFlow = MutableStateFlow(false)
    val paymentProofFlow = MutableStateFlow<String?>(null)
    val bitcoinPaymentDataFlow = MutableStateFlow<String?>(null)
    val tradeCompletedDateFlow = MutableStateFlow<Long?>(null)

    val tradeModel = mockk<BisqEasyTradeModel>(relaxed = true)
    every { tradeModel.isSeller } returns isSeller
    every { tradeModel.tradeState } returns tradeStateFlow
    every { tradeModel.takeOfferDate } returns 1_000_000L
    every { tradeModel.paymentProof } returns paymentProofFlow
    every { tradeModel.bitcoinPaymentData } returns bitcoinPaymentDataFlow
    every { tradeModel.tradeCompletedDate } returns tradeCompletedDateFlow

    val networkId =
        NetworkIdVO(
            addressByTransportTypeMap =
                AddressByTransportTypeMapVO(
                    mapOf(TransportTypeEnum.TOR to AddressVO("x.onion", 1)),
                ),
            pubKey =
                PubKeyVO(
                    publicKey = PublicKeyVO("pub"),
                    keyId = "key",
                    hash = "hash",
                    id = "id",
                ),
        )
    every { tradeModel.peer } returns BisqEasyTradePartyVO(networkId)

    val priceSpec = mockk<FixPriceSpecVO>(relaxed = true)
    val offer = mockk<BisqEasyOfferVO>(relaxed = true)
    every { offer.priceSpec } returns priceSpec
    every { offer.market.baseCurrencyCode } returns "BTC"
    every { offer.market.quoteCurrencyCode } returns "USD"

    val channelModel = mockk<BisqEasyOpenTradeChannelModel>(relaxed = true)
    every { channelModel.bisqEasyOffer } returns offer
    every { channelModel.isInMediation } returns isInMediationFlow

    val peersUserProfile: UserProfileVO = createMockUserProfile("peer")
    val peersReputationScore = ReputationScoreVO(0L, 0.0, 0)

    val model = mockk<TradeItemPresentationModel>(relaxed = true)
    every { model.bisqEasyTradeModel } returns tradeModel
    every { model.bisqEasyOpenTradeChannelModel } returns channelModel
    every { model.bisqEasyOffer } returns offer
    every { model.directionalTitle } returns "DIR_TITLE"
    every { model.peersUserProfile } returns peersUserProfile
    every { model.peersReputationScore } returns peersReputationScore
    every { model.formattedPrice } returns "99 USD"
    every { model.formattedDate } returns "d"
    every { model.formattedTime } returns "t"
    every { model.fiatPaymentMethodDisplayString } returns "fiat"
    every { model.bitcoinSettlementMethodDisplayString } returns "btc-settle"
    every { model.shortTradeId } returns "short"
    every { model.tradeId } returns "tid"
    every { model.mediatorUserName } returns null
    every { model.formattedBaseAmount } returns "FMT_BASE"
    every { model.baseCurrencyCode } returns "BTC"
    every { model.formattedQuoteAmount } returns "FMT_QUOTE"
    every { model.quoteCurrencyCode } returns "USD"
    every { model.bitcoinSettlementMethod } returns "LIGHTNING"

    val selectedTrade = MutableStateFlow<TradeItemPresentationModel?>(model)
    return TradeDetailsHeaderTestHarness(
        selectedTrade = selectedTrade,
        tradeStateFlow = tradeStateFlow,
        isInMediationFlow = isInMediationFlow,
        paymentProofFlow = paymentProofFlow,
        bitcoinPaymentDataFlow = bitcoinPaymentDataFlow,
        tradeCompletedDateFlow = tradeCompletedDateFlow,
    )
}
