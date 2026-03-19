package network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades

import network.bisq.mobile.data.replicated.account.protocol_type.TradeProtocolTypeEnum
import network.bisq.mobile.data.replicated.chat.ChatChannelDomainEnum
import network.bisq.mobile.data.replicated.chat.ChatMessageTypeEnum
import network.bisq.mobile.data.replicated.chat.reactions.BisqEasyOpenTradeMessageReactionVO
import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOFactory
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOFactory.fromPrice
import network.bisq.mobile.data.replicated.identity.IdentityVO
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.offer.amount.spec.QuoteSideFixedAmountSpecVO
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.data.replicated.security.keys.I2pKeyPairVO
import network.bisq.mobile.data.replicated.security.keys.KeyBundleVO
import network.bisq.mobile.data.replicated.security.keys.KeyPairVO
import network.bisq.mobile.data.replicated.security.keys.PrivateKeyVO
import network.bisq.mobile.data.replicated.security.keys.PublicKeyVO
import network.bisq.mobile.data.replicated.security.keys.TorKeyPairVO
import network.bisq.mobile.data.replicated.user.identity.UserIdentityVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class BisqEasyOpenTradeChannelModelTest {
    @Test
    fun addChatMessages_replacesExistingMessageWithSameId() {
        val myUserProfile = createMockUserProfile("me")
        val sender = createMockUserProfile("sender")
        val channelModel = BisqEasyOpenTradeChannelModel(createChannelDto(myUserProfile, sender))

        val original =
            createMessageModel(
                myUserProfile = myUserProfile,
                sender = sender,
                messageId = "message-1",
                reactionId = 1,
            )
        val updated =
            createMessageModel(
                myUserProfile = myUserProfile,
                sender = sender,
                messageId = "message-1",
                reactionId = 2,
            )

        channelModel.addChatMessages(original)
        channelModel.addChatMessages(updated)

        val storedMessage = channelModel.chatMessages.value.single()
        assertEquals(1, channelModel.chatMessages.value.size)
        assertSame(updated, storedMessage)
        assertEquals(
            2,
            storedMessage.chatReactions.value
                .single()
                .reactionId,
        )
    }

    private fun createChannelDto(
        myUserProfile: UserProfileVO,
        peer: UserProfileVO,
    ): BisqEasyOpenTradeChannelDto =
        BisqEasyOpenTradeChannelDto(
            id = "channel-1",
            tradeId = "trade-1",
            bisqEasyOffer = createOffer(myUserProfile),
            myUserIdentity = createUserIdentity(myUserProfile),
            traders = setOf(peer),
            mediator = null,
        )

    private fun createOffer(userProfile: UserProfileVO): BisqEasyOfferVO {
        val market = MarketVO("BTC", "USD", "Bitcoin", "US Dollar")
        return BisqEasyOfferVO(
            id = "offer-1",
            date = 0L,
            makerNetworkId = userProfile.networkId,
            direction = DirectionEnum.BUY,
            market = market,
            amountSpec = QuoteSideFixedAmountSpecVO(100_00),
            priceSpec = FixPriceSpecVO(PriceQuoteVOFactory.fromPrice(100_00L, market)),
            protocolTypes = listOf(TradeProtocolTypeEnum.BISQ_EASY),
            baseSidePaymentMethodSpecs = emptyList(),
            quoteSidePaymentMethodSpecs = emptyList(),
            offerOptions = emptyList(),
            supportedLanguageCodes = emptyList(),
        )
    }

    private fun createUserIdentity(userProfile: UserProfileVO): UserIdentityVO =
        UserIdentityVO(
            identity =
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
                ),
            userProfile = userProfile,
        )

    private fun createMessageModel(
        myUserProfile: UserProfileVO,
        sender: UserProfileVO,
        messageId: String,
        reactionId: Int,
    ): BisqEasyOpenTradeMessageModel =
        BisqEasyOpenTradeMessageModel(
            bisqEasyOpenTradeMessage =
                BisqEasyOpenTradeMessageDto(
                    tradeId = "trade-1",
                    messageId = messageId,
                    channelId = "channel-1",
                    senderUserProfile = sender,
                    receiverUserProfileId = "receiver-1",
                    receiverNetworkId = myUserProfile.networkId,
                    text = "hello",
                    citation = null,
                    date = 1234L,
                    mediator = null,
                    chatMessageType = ChatMessageTypeEnum.TEXT,
                    bisqEasyOffer = null,
                    chatMessageReactions = emptySet(),
                    citationAuthorUserProfile = null,
                ),
            myUserProfile = myUserProfile,
            chatReactions =
                listOf(
                    BisqEasyOpenTradeMessageReactionVO(
                        id = "reaction-$reactionId",
                        senderUserProfile = sender,
                        receiverUserProfileId = "receiver-1",
                        receiverNetworkId = myUserProfile.networkId,
                        chatChannelId = "channel-1",
                        chatChannelDomain = ChatChannelDomainEnum.BISQ_EASY_OPEN_TRADES,
                        chatMessageId = messageId,
                        reactionId = reactionId,
                        date = 1234L,
                        isRemoved = false,
                    ),
                ),
        )
}
