package network.bisq.mobile.client.common.domain.service.trades

import network.bisq.mobile.data.replicated.account.protocol_type.TradeProtocolTypeEnum
import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOFactory
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOFactory.fromPrice
import network.bisq.mobile.data.replicated.contract.BisqEasyContractVO
import network.bisq.mobile.data.replicated.contract.PartyVO
import network.bisq.mobile.data.replicated.contract.RoleEnum
import network.bisq.mobile.data.replicated.identity.IdentityVO
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.offer.amount.spec.QuoteSideFixedAmountSpecVO
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.data.replicated.offer.payment_method.BitcoinPaymentMethodSpecVO
import network.bisq.mobile.data.replicated.offer.payment_method.FiatPaymentMethodSpecVO
import network.bisq.mobile.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.data.replicated.security.keys.I2pKeyPairVO
import network.bisq.mobile.data.replicated.security.keys.KeyBundleVO
import network.bisq.mobile.data.replicated.security.keys.KeyPairVO
import network.bisq.mobile.data.replicated.security.keys.PrivateKeyVO
import network.bisq.mobile.data.replicated.security.keys.PublicKeyVO
import network.bisq.mobile.data.replicated.security.keys.TorKeyPairVO
import network.bisq.mobile.data.replicated.trade.TradeRoleEnum
import network.bisq.mobile.data.replicated.trade.bisq_easy.BisqEasyTradeDto
import network.bisq.mobile.data.replicated.trade.bisq_easy.BisqEasyTradePartyVO
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.data.replicated.user.identity.UserIdentityVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * Guards the mapping that replaced `TradeItemPresentationModel.from(dto)` when the DTO became
 * client-owned. The node builds the same model from its Bisq 2 types in
 * `TradeItemPresentationModelFactory`, so a field silently dropped here would show up as a
 * client-only bug.
 */
class TradeItemPresentationDtoMappingTest {
    private val maker = createMockUserProfile("Maker")
    private val taker = createMockUserProfile("Taker")
    private val mediator = createMockUserProfile("Mediator")

    @Test
    fun `carries every presentation field`() {
        val dto = createDto()

        val model = dto.toDomain()

        assertEquals("2024-01-01", model.formattedDate)
        assertEquals("12:30", model.formattedTime)
        assertEquals("BTC/USD", model.market)
        assertEquals(9_500_000L, model.price)
        assertEquals("95,000 USD", model.formattedPrice)
        assertEquals(100_000L, model.baseAmount)
        assertEquals("0.001 BTC", model.formattedBaseAmount)
        assertEquals(9_500L, model.quoteAmount)
        assertEquals("95 USD", model.formattedQuoteAmount)
        assertEquals("MAIN_CHAIN", model.bitcoinSettlementMethod)
        assertEquals("Bitcoin", model.bitcoinSettlementMethodDisplayString)
        assertEquals("ZELLE", model.fiatPaymentMethod)
        assertEquals("Zelle", model.fiatPaymentMethodDisplayString)
        assertEquals(true, model.isFiatPaymentMethodCustom)
        assertEquals("Buyer (Taker)", model.formattedMyRole)
        assertEquals(dto.peersReputationScore, model.peersReputationScore)
        assertSame(maker, model.makerUserProfile)
        assertSame(taker, model.takerUserProfile)
        assertEquals("trade-1", model.bisqEasyTradeModel.id)
    }

    /**
     * `directionalTitle` and `mediatorUserProfile` are deliberately not carried over — the model
     * derives both from the trade, so the DTO's copies were already dead. If someone re-adds them to
     * the mapping this test still passes, but it pins where the values must come from.
     */
    @Test
    fun `derives direction and mediator from the trade not from the dto copies`() {
        val model = createDto().toDomain()

        assertSame(mediator, model.mediator)
        assertEquals(true, model.bisqEasyTradeModel.isBuyer)
    }

    @Test
    fun `my profile follows the trade role`() {
        val asTaker = createDto(tradeRole = TradeRoleEnum.BUYER_AS_TAKER).toDomain()
        assertSame(taker, asTaker.myUserProfile)
        assertSame(maker, asTaker.peersUserProfile)

        val asMaker = createDto(tradeRole = TradeRoleEnum.BUYER_AS_MAKER).toDomain()
        assertSame(maker, asMaker.myUserProfile)
        assertSame(taker, asMaker.peersUserProfile)
    }

    @Test
    fun `channel dto maps into the open trade channel`() {
        val model = createDto().toDomain()

        val channel = model.bisqEasyOpenTradeChannelModel
        assertEquals("channel-1", channel.id)
        assertEquals("trade-1", channel.tradeId)
        assertEquals(setOf(maker), channel.traders)
        assertSame(mediator, channel.mediator)
        assertEquals(taker, channel.myUserIdentity.userProfile)
    }

    private fun createDto(tradeRole: TradeRoleEnum = TradeRoleEnum.BUYER_AS_TAKER): TradeItemPresentationDto {
        val offer = createOffer()
        return TradeItemPresentationDto(
            channel =
                BisqEasyOpenTradeChannelDto(
                    id = "channel-1",
                    tradeId = "trade-1",
                    bisqEasyOffer = offer,
                    myUserIdentity = createUserIdentity(taker),
                    traders = setOf(maker),
                    mediator = mediator,
                ),
            trade = createTrade(offer, tradeRole),
            makerUserProfile = maker,
            takerUserProfile = taker,
            mediatorUserProfile = mediator,
            directionalTitle = "Buy Bitcoin",
            formattedDate = "2024-01-01",
            formattedTime = "12:30",
            market = "BTC/USD",
            price = 9_500_000L,
            formattedPrice = "95,000 USD",
            baseAmount = 100_000L,
            formattedBaseAmount = "0.001 BTC",
            quoteAmount = 9_500L,
            formattedQuoteAmount = "95 USD",
            bitcoinSettlementMethod = "MAIN_CHAIN",
            bitcoinSettlementMethodDisplayString = "Bitcoin",
            fiatPaymentMethod = "ZELLE",
            fiatPaymentMethodDisplayString = "Zelle",
            isFiatPaymentMethodCustom = true,
            formattedMyRole = "Buyer (Taker)",
            peersReputationScore = ReputationScoreVO(totalScore = 1_200L, fiveSystemScore = 3.5, ranking = 7),
        )
    }

    private fun createOffer(): BisqEasyOfferVO {
        val market = MarketVO("BTC", "USD", "Bitcoin", "US Dollar")
        return BisqEasyOfferVO(
            id = "offer-1",
            date = 0L,
            makerNetworkId = maker.networkId,
            direction = DirectionEnum.SELL,
            market = market,
            amountSpec = QuoteSideFixedAmountSpecVO(9_500L),
            priceSpec = FixPriceSpecVO(PriceQuoteVOFactory.fromPrice(9_500_000L, market)),
            protocolTypes = listOf(TradeProtocolTypeEnum.BISQ_EASY),
            baseSidePaymentMethodSpecs = emptyList(),
            quoteSidePaymentMethodSpecs = emptyList(),
            offerOptions = emptyList(),
            supportedLanguageCodes = emptyList(),
        )
    }

    private fun createTrade(
        offer: BisqEasyOfferVO,
        tradeRole: TradeRoleEnum,
    ) = BisqEasyTradeDto(
        contract =
            BisqEasyContractVO(
                takeOfferDate = 0L,
                offer = offer,
                maker = PartyVO(role = RoleEnum.MAKER, networkId = maker.networkId),
                taker = PartyVO(role = RoleEnum.TAKER, networkId = taker.networkId),
                baseSideAmount = 100_000L,
                quoteSideAmount = 9_500L,
                baseSidePaymentMethodSpec =
                    BitcoinPaymentMethodSpecVO(paymentMethod = "MAIN_CHAIN", saltedMakerAccountId = "btc"),
                quoteSidePaymentMethodSpec =
                    FiatPaymentMethodSpecVO(paymentMethod = "ZELLE", saltedMakerAccountId = "fiat"),
                mediator = mediator,
                priceSpec = FixPriceSpecVO(PriceQuoteVOFactory.fromPrice(9_500_000L, offer.market)),
                marketPrice = 9_500_000L,
            ),
        id = "trade-1",
        tradeRole = tradeRole,
        myIdentity = createIdentity(taker),
        taker = BisqEasyTradePartyVO(networkId = taker.networkId),
        maker = BisqEasyTradePartyVO(networkId = maker.networkId),
        tradeState = BisqEasyTradeStateEnum.INIT,
        paymentAccountData = null,
        bitcoinPaymentData = null,
        paymentProof = null,
        interruptTradeInitiator = null,
        errorMessage = null,
        errorStackTrace = null,
        peersErrorMessage = null,
        peersErrorStackTrace = null,
    )

    private fun createUserIdentity(userProfile: UserProfileVO) = UserIdentityVO(identity = createIdentity(userProfile), userProfile = userProfile)

    private fun createIdentity(userProfile: UserProfileVO) =
        IdentityVO(
            tag = "identity-1",
            networkId = userProfile.networkId,
            keyBundle =
                KeyBundleVO(
                    keyId = "key-1",
                    keyPair =
                        KeyPairVO(
                            publicKey = PublicKeyVO("public-key"),
                            privateKey = PrivateKeyVO("private-key"),
                        ),
                    torKeyPair =
                        TorKeyPairVO(
                            privateKeyEncoded = "tor-private",
                            publicKeyEncoded = "tor-public",
                            onionAddress = "address.onion",
                        ),
                    i2pKeyPair =
                        I2pKeyPairVO(
                            identityBytes = "identity-bytes",
                            destinationBytes = "destination-bytes",
                        ),
                ),
        )
}
