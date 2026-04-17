package network.bisq.mobile.presentation.trade.trade_detail

import io.mockk.every
import io.mockk.mockk
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
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.formatters.PriceSpecFormatter
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ToHeaderTradeUiStateTest {
    @BeforeTest
    fun initI18n() {
        I18nSupport.initialize("en")
    }

    @Test
    fun mapsSeller_amountsAndDescriptions() {
        val model =
            tradeItemPresentationModelForHeader(
                isSeller = true,
                bitcoinSettlementMethod = "LIGHTNING",
            )
        val state = model.toHeaderTradeUiState(isSmallScreen = false)

        assertTrue(state.isSell)
        assertEquals("bisqEasy.tradeState.header.send".i18n(), state.leftAmountDescription)
        assertEquals("bisqEasy.tradeState.header.receive".i18n(), state.rightAmountDescription)
        assertEquals("FMT_BASE", state.leftAmount)
        assertEquals("BTC", state.leftCode)
        assertEquals("FMT_QUOTE", state.rightAmount)
        assertEquals("USD", state.rightCode)
        assertFalse(state.isMainChainPayment)
    }

    @Test
    fun mapsBuyer_amountsAndDescriptions() {
        val model =
            tradeItemPresentationModelForHeader(
                isSeller = false,
                bitcoinSettlementMethod = "MAIN_CHAIN",
            )
        val state = model.toHeaderTradeUiState(isSmallScreen = false)

        assertFalse(state.isSell)
        assertEquals("bisqEasy.tradeState.header.pay".i18n(), state.leftAmountDescription)
        assertEquals("bisqEasy.tradeState.header.receive".i18n(), state.rightAmountDescription)
        assertEquals("FMT_QUOTE", state.leftAmount)
        assertEquals("USD", state.leftCode)
        assertEquals("FMT_BASE", state.rightAmount)
        assertEquals("BTC", state.rightCode)
        assertTrue(state.isMainChainPayment)
    }

    @Test
    fun passesThrough_isSmallScreen() {
        val model = tradeItemPresentationModelForHeader(isSeller = true)
        assertTrue(model.toHeaderTradeUiState(isSmallScreen = true).isSmallScreen)
        assertFalse(model.toHeaderTradeUiState(isSmallScreen = false).isSmallScreen)
    }

    @Test
    fun priceDisplay_usesFormattedPriceWhenFixPriceSpec() {
        val fixSpec = mockk<FixPriceSpecVO>(relaxed = true)
        val model =
            tradeItemPresentationModelForHeader(
                isSeller = true,
                formattedPrice = "123.45 USD",
                priceSpec = fixSpec,
            )
        val state = model.toHeaderTradeUiState(isSmallScreen = false)
        assertEquals(
            PriceSpecFormatter.formatPriceWithSpec("123.45 USD", fixSpec),
            state.priceDisplay,
        )
    }

    @Test
    fun peerNetworkAddress_prefersTorAddress() {
        val tor = AddressVO("peer.onion", 1234)
        val clear = AddressVO("1.2.3.4", 80)
        val model =
            tradeItemPresentationModelForHeader(
                isSeller = true,
                peerAddresses = mapOf(TransportTypeEnum.TOR to tor, TransportTypeEnum.CLEAR to clear),
            )
        assertEquals("peer.onion:1234", model.toHeaderTradeUiState(isSmallScreen = false).peerNetworkAddress)
    }

    @Test
    fun peerNetworkAddress_fallsBackToFirstWhenNoTor() {
        val clear = AddressVO("1.2.3.4", 8080)
        val model =
            tradeItemPresentationModelForHeader(
                isSeller = true,
                peerAddresses = mapOf(TransportTypeEnum.CLEAR to clear),
            )
        assertEquals("1.2.3.4:8080", model.toHeaderTradeUiState(isSmallScreen = false).peerNetworkAddress)
    }

    @Test
    fun peerNetworkAddress_nullWhenNoAddresses() {
        val model =
            tradeItemPresentationModelForHeader(
                isSeller = true,
                peerAddresses = emptyMap(),
            )
        assertNull(model.toHeaderTradeUiState(isSmallScreen = false).peerNetworkAddress)
    }

    @Test
    fun copiesThroughPresentationFields() {
        val peersProfile = createMockUserProfile("peer-user")
        val reputation = ReputationScoreVO(totalScore = 10L, fiveSystemScore = 4.5, ranking = 2)
        val model =
            tradeItemPresentationModelForHeader(
                isSeller = true,
                peersUserProfile = peersProfile,
                peersReputationScore = reputation,
                formattedDate = "2024-01-02",
                formattedTime = "12:34",
                fiatPaymentMethodDisplayString = "SEPA",
                bitcoinSettlementMethodDisplayString = "On-chain",
                shortTradeId = "abcd1234",
                tradeId = "full-trade-id",
                mediatorUserName = "MedName",
            )
        val state = model.toHeaderTradeUiState(isSmallScreen = false)

        assertEquals(peersProfile, state.peersUserProfile)
        assertEquals(reputation, state.peersReputationScore)
        assertEquals("2024-01-02", state.formattedDate)
        assertEquals("12:34", state.formattedTime)
        assertEquals("SEPA", state.fiatPaymentMethodDisplayString)
        assertEquals("On-chain", state.bitcoinSettlementMethodDisplayString)
        assertEquals("abcd1234", state.shortTradeId)
        assertEquals("full-trade-id", state.tradeId)
        assertEquals("MedName", state.mediatorUserName)
        assertEquals("DIR_TITLE", state.directionalTitle)
    }

    private fun tradeItemPresentationModelForHeader(
        isSeller: Boolean,
        bitcoinSettlementMethod: String = "LIGHTNING",
        formattedPrice: String = "99 USD",
        priceSpec: FixPriceSpecVO = mockk<FixPriceSpecVO>(relaxed = true),
        peersUserProfile: UserProfileVO = createMockUserProfile("peer"),
        peersReputationScore: ReputationScoreVO =
            ReputationScoreVO(0L, 0.0, 0),
        formattedDate: String = "d",
        formattedTime: String = "t",
        fiatPaymentMethodDisplayString: String = "fiat",
        bitcoinSettlementMethodDisplayString: String = "btc-settle",
        shortTradeId: String = "short",
        tradeId: String = "tid",
        mediatorUserName: String? = null,
        peerAddresses: Map<TransportTypeEnum, AddressVO> =
            mapOf(TransportTypeEnum.TOR to AddressVO("x.onion", 1)),
    ): TradeItemPresentationModel {
        val tradeModel = mockk<BisqEasyTradeModel>(relaxed = true)
        every { tradeModel.isSeller } returns isSeller
        val networkId =
            NetworkIdVO(
                addressByTransportTypeMap = AddressByTransportTypeMapVO(peerAddresses),
                pubKey =
                    PubKeyVO(
                        publicKey = PublicKeyVO("pub"),
                        keyId = "key",
                        hash = "hash",
                        id = "id",
                    ),
            )
        every { tradeModel.peer } returns BisqEasyTradePartyVO(networkId)

        val offer = mockk<BisqEasyOfferVO>(relaxed = true)
        every { offer.priceSpec } returns priceSpec
        every { offer.market.baseCurrencyCode } returns "BTC"
        every { offer.market.quoteCurrencyCode } returns "USD"

        val channel = mockk<BisqEasyOpenTradeChannelModel>(relaxed = true)
        every { channel.bisqEasyOffer } returns offer

        val model = mockk<TradeItemPresentationModel>(relaxed = true)
        every { model.bisqEasyTradeModel } returns tradeModel
        every { model.bisqEasyOpenTradeChannelModel } returns channel
        every { model.bisqEasyOffer } returns offer
        every { model.directionalTitle } returns "DIR_TITLE"
        every { model.peersUserProfile } returns peersUserProfile
        every { model.peersReputationScore } returns peersReputationScore
        every { model.formattedPrice } returns formattedPrice
        every { model.formattedDate } returns formattedDate
        every { model.formattedTime } returns formattedTime
        every { model.fiatPaymentMethodDisplayString } returns fiatPaymentMethodDisplayString
        every { model.bitcoinSettlementMethodDisplayString } returns bitcoinSettlementMethodDisplayString
        every { model.shortTradeId } returns shortTradeId
        every { model.tradeId } returns tradeId
        every { model.mediatorUserName } returns mediatorUserName
        every { model.formattedBaseAmount } returns "FMT_BASE"
        every { model.baseCurrencyCode } returns "BTC"
        every { model.formattedQuoteAmount } returns "FMT_QUOTE"
        every { model.quoteCurrencyCode } returns "USD"
        every { model.bitcoinSettlementMethod } returns bitcoinSettlementMethod
        return model
    }
}
