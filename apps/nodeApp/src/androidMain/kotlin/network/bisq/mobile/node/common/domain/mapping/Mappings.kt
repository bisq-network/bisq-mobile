/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */
package network.bisq.mobile.node.common.domain.mapping

import bisq.account.payment_method.BitcoinPaymentMethodSpec
import bisq.account.payment_method.PaymentMethodSpec
import bisq.account.payment_method.PaymentMethodSpecUtil
import bisq.account.payment_method.fiat.FiatPaymentMethodSpec
import bisq.account.protocol_type.TradeProtocolType
import bisq.chat.ChatChannelDomain
import bisq.chat.ChatMessageType
import bisq.chat.Citation
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage
import bisq.chat.notifications.ChatChannelNotificationType
import bisq.chat.reactions.BisqEasyOfferbookMessageReaction
import bisq.chat.reactions.BisqEasyOpenTradeMessageReaction
import bisq.chat.reactions.Reaction
import bisq.common.encoding.Hex
import bisq.common.market.Market
import bisq.common.monetary.Coin
import bisq.common.monetary.Fiat
import bisq.common.monetary.Monetary
import bisq.common.monetary.PriceQuote
import bisq.common.network.Address
import bisq.common.network.AddressByTransportTypeMap
import bisq.common.network.ClearnetAddress
import bisq.common.network.I2PAddress
import bisq.common.network.TorAddress
import bisq.common.network.TransportType
import bisq.contract.ContractSignatureData
import bisq.contract.Party
import bisq.contract.Role
import bisq.contract.bisq_easy.BisqEasyContract
import bisq.identity.Identity
import bisq.network.identity.NetworkId
import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatus
import bisq.offer.Direction
import bisq.offer.amount.spec.AmountSpec
import bisq.offer.amount.spec.BaseSideFixedAmountSpec
import bisq.offer.amount.spec.BaseSideRangeAmountSpec
import bisq.offer.amount.spec.FixedAmountSpec
import bisq.offer.amount.spec.QuoteSideFixedAmountSpec
import bisq.offer.amount.spec.QuoteSideRangeAmountSpec
import bisq.offer.amount.spec.RangeAmountSpec
import bisq.offer.bisq_easy.BisqEasyOffer
import bisq.offer.options.OfferOption
import bisq.offer.options.ReputationOption
import bisq.offer.options.TradeTermsOption
import bisq.offer.price.spec.FixPriceSpec
import bisq.offer.price.spec.FloatPriceSpec
import bisq.offer.price.spec.MarketPriceSpec
import bisq.offer.price.spec.PriceSpec
import bisq.security.DigestUtil
import bisq.security.keys.KeyBundle
import bisq.security.keys.KeyGeneration
import bisq.security.keys.PubKey
import bisq.security.keys.TorKeyPair
import bisq.security.pow.ProofOfWork
import bisq.settings.SettingsService
import bisq.trade.TradeRole
import bisq.trade.bisq_easy.BisqEasyTrade
import bisq.trade.bisq_easy.BisqEasyTradeParty
import bisq.trade.bisq_easy.protocol.BisqEasyTradeState
import bisq.user.identity.UserIdentity
import bisq.user.profile.UserProfile
import bisq.user.reputation.ReputationScore
import network.bisq.mobile.android.node.BuildNodeConfig
import network.bisq.mobile.domain.data.replicated.account.protocol_type.TradeProtocolTypeEnum
import network.bisq.mobile.domain.data.replicated.chat.ChatChannelDomainEnum
import network.bisq.mobile.domain.data.replicated.chat.ChatMessageTypeEnum
import network.bisq.mobile.domain.data.replicated.chat.CitationVO
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.offerbook.BisqEasyOfferbookMessageDto
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelDto
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageDto
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel
import network.bisq.mobile.domain.data.replicated.chat.notifications.ChatChannelNotificationTypeEnum
import network.bisq.mobile.domain.data.replicated.chat.reactions.BisqEasyOfferbookMessageReactionVO
import network.bisq.mobile.domain.data.replicated.chat.reactions.BisqEasyOpenTradeMessageReactionVO
import network.bisq.mobile.domain.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.common.monetary.CoinVO
import network.bisq.mobile.domain.data.replicated.common.monetary.CoinVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.CoinVOFactory.from
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVO
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory.from
import network.bisq.mobile.domain.data.replicated.common.monetary.MonetaryVO
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.domain.data.replicated.common.network.AddressByTransportTypeMapVO
import network.bisq.mobile.domain.data.replicated.common.network.AddressVO
import network.bisq.mobile.domain.data.replicated.common.network.TransportTypeEnum
import network.bisq.mobile.domain.data.replicated.contract.BisqEasyContractVO
import network.bisq.mobile.domain.data.replicated.contract.ContractSignatureDataVO
import network.bisq.mobile.domain.data.replicated.contract.PartyVO
import network.bisq.mobile.domain.data.replicated.contract.RoleEnum
import network.bisq.mobile.domain.data.replicated.identity.IdentityVO
import network.bisq.mobile.domain.data.replicated.network.confidential.ack.MessageDeliveryStatusEnum
import network.bisq.mobile.domain.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.AmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.BaseSideFixedAmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.BaseSideRangeAmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.FixedAmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.QuoteSideFixedAmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.QuoteSideRangeAmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.RangeAmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.domain.data.replicated.offer.options.OfferOptionVO
import network.bisq.mobile.domain.data.replicated.offer.options.ReputationOptionVO
import network.bisq.mobile.domain.data.replicated.offer.options.TradeTermsOptionVO
import network.bisq.mobile.domain.data.replicated.offer.payment_method.BitcoinPaymentMethodSpecVO
import network.bisq.mobile.domain.data.replicated.offer.payment_method.FiatPaymentMethodSpecVO
import network.bisq.mobile.domain.data.replicated.offer.payment_method.PaymentMethodSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.FloatPriceSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.MarketPriceSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.PriceSpecVO
import network.bisq.mobile.domain.data.replicated.security.keys.KeyBundleVO
import network.bisq.mobile.domain.data.replicated.security.keys.KeyPairVO
import network.bisq.mobile.domain.data.replicated.security.keys.PrivateKeyVO
import network.bisq.mobile.domain.data.replicated.security.keys.PubKeyVO
import network.bisq.mobile.domain.data.replicated.security.keys.PublicKeyVO
import network.bisq.mobile.domain.data.replicated.security.keys.TorKeyPairVO
import network.bisq.mobile.domain.data.replicated.security.pow.ProofOfWorkVO
import network.bisq.mobile.domain.data.replicated.settings.SettingsVO
import network.bisq.mobile.domain.data.replicated.trade.TradeRoleEnum
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.BisqEasyTradeDto
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.BisqEasyTradeModel
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.BisqEasyTradePartyVO
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.domain.data.replicated.user.identity.UserIdentityVO
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.profile.userProfileDemoObj
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.util.Base64
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

class Mappings {
    // account.protocol_type

    object TradeProtocolTypeMapping {
        fun toBisq2Model(value: TradeProtocolTypeEnum): TradeProtocolType =
            when (value) {
                TradeProtocolTypeEnum.BISQ_EASY -> TradeProtocolType.BISQ_EASY
                TradeProtocolTypeEnum.MU_SIG -> TradeProtocolType.MU_SIG
                TradeProtocolTypeEnum.SUBMARINE -> TradeProtocolType.SUBMARINE
                TradeProtocolTypeEnum.LIQUID_MU_SIG -> TradeProtocolType.LIQUID_MU_SIG
                TradeProtocolTypeEnum.BISQ_LIGHTNING -> TradeProtocolType.BISQ_LIGHTNING
                TradeProtocolTypeEnum.LIQUID_SWAP -> TradeProtocolType.LIQUID_SWAP
                TradeProtocolTypeEnum.BSQ_SWAP -> TradeProtocolType.BSQ_SWAP
                TradeProtocolTypeEnum.LIGHTNING_ESCROW -> TradeProtocolType.LIGHTNING_ESCROW
                TradeProtocolTypeEnum.MONERO_SWAP -> TradeProtocolType.MONERO_SWAP
            }

        fun fromBisq2Model(value: TradeProtocolType): TradeProtocolTypeEnum =
            when (value) {
                TradeProtocolType.BISQ_EASY -> TradeProtocolTypeEnum.BISQ_EASY
                TradeProtocolType.MU_SIG -> TradeProtocolTypeEnum.MU_SIG
                TradeProtocolType.SUBMARINE -> TradeProtocolTypeEnum.SUBMARINE
                TradeProtocolType.LIQUID_MU_SIG -> TradeProtocolTypeEnum.LIQUID_MU_SIG
                TradeProtocolType.BISQ_LIGHTNING -> TradeProtocolTypeEnum.BISQ_LIGHTNING
                TradeProtocolType.LIQUID_SWAP -> TradeProtocolTypeEnum.LIQUID_SWAP
                TradeProtocolType.BSQ_SWAP -> TradeProtocolTypeEnum.BSQ_SWAP
                TradeProtocolType.LIGHTNING_ESCROW -> TradeProtocolTypeEnum.LIGHTNING_ESCROW
                TradeProtocolType.MONERO_SWAP -> TradeProtocolTypeEnum.MONERO_SWAP
            }
    }

    // chat

    object ChatChannelDomainMapping {
        fun toBisq2Model(value: ChatChannelDomainEnum): ChatChannelDomain =
            when (value) {
                ChatChannelDomainEnum.BISQ_EASY_OFFERBOOK -> ChatChannelDomain.BISQ_EASY_OFFERBOOK
                ChatChannelDomainEnum.BISQ_EASY_OPEN_TRADES -> ChatChannelDomain.BISQ_EASY_OPEN_TRADES
                ChatChannelDomainEnum.DISCUSSION -> ChatChannelDomain.DISCUSSION
                ChatChannelDomainEnum.SUPPORT -> ChatChannelDomain.SUPPORT
                ChatChannelDomainEnum.MU_SIG_OPEN_TRADES -> ChatChannelDomain.MU_SIG_OPEN_TRADES
            }

        fun fromBisq2Model(value: ChatChannelDomain): ChatChannelDomainEnum =
            when (value) {
                ChatChannelDomain.BISQ_EASY_OFFERBOOK -> ChatChannelDomainEnum.BISQ_EASY_OFFERBOOK
                ChatChannelDomain.BISQ_EASY_OPEN_TRADES -> ChatChannelDomainEnum.BISQ_EASY_OPEN_TRADES
                ChatChannelDomain.DISCUSSION -> ChatChannelDomainEnum.DISCUSSION
                ChatChannelDomain.SUPPORT -> ChatChannelDomainEnum.SUPPORT
                ChatChannelDomain.BISQ_EASY_PRIVATE_CHAT -> ChatChannelDomainEnum.DISCUSSION
                ChatChannelDomain.MU_SIG_OPEN_TRADES -> ChatChannelDomainEnum.MU_SIG_OPEN_TRADES
                ChatChannelDomain.EVENTS -> ChatChannelDomainEnum.DISCUSSION
            }
    }

    object ChatMessageTypeMapping {
        fun toBisq2Model(value: ChatMessageTypeEnum): ChatMessageType =
            when (value) {
                ChatMessageTypeEnum.TEXT -> ChatMessageType.TEXT
                ChatMessageTypeEnum.LEAVE -> ChatMessageType.LEAVE
                ChatMessageTypeEnum.TAKE_BISQ_EASY_OFFER -> ChatMessageType.TAKE_BISQ_EASY_OFFER
                ChatMessageTypeEnum.PROTOCOL_LOG_MESSAGE -> ChatMessageType.PROTOCOL_LOG_MESSAGE
                ChatMessageTypeEnum.CHAT_RULES_WARNING -> ChatMessageType.CHAT_RULES_WARNING
            }

        fun fromBisq2Model(value: ChatMessageType): ChatMessageTypeEnum =
            when (value) {
                ChatMessageType.TEXT -> ChatMessageTypeEnum.TEXT
                ChatMessageType.LEAVE -> ChatMessageTypeEnum.LEAVE
                ChatMessageType.TAKE_BISQ_EASY_OFFER -> ChatMessageTypeEnum.TAKE_BISQ_EASY_OFFER
                ChatMessageType.PROTOCOL_LOG_MESSAGE -> ChatMessageTypeEnum.PROTOCOL_LOG_MESSAGE
                ChatMessageType.CHAT_RULES_WARNING -> ChatMessageTypeEnum.CHAT_RULES_WARNING
            }
    }

    object CitationMapping {
        fun toBisq2Model(value: CitationVO): Citation = Citation(value.authorUserProfileId, value.text, Optional.ofNullable(value.chatMessageId))

        fun fromBisq2Model(value: Citation): CitationVO = CitationVO(value.authorUserProfileId, value.text, value.chatMessageId.getOrNull())
    }

    // chat.bisq_easy.offerbook

    object BisqEasyOfferbookMessageMapping {
        fun toBisq2Model(value: BisqEasyOfferbookMessageDto): BisqEasyOfferbookMessage =
            BisqEasyOfferbookMessage(
                value.id,
                ChatChannelDomain.BISQ_EASY_OFFERBOOK,
                value.channelId,
                value.authorUserProfileId,
                Optional.ofNullable(value.bisqEasyOffer?.let { BisqEasyOfferMapping.toBisq2Model(it) }),
                Optional.ofNullable(value.text),
                Optional.ofNullable(value.citation?.let { CitationMapping.toBisq2Model(it) }),
                value.date,
                value.wasEdited,
                ChatMessageTypeMapping.toBisq2Model(value.chatMessageType),
            )

        fun fromBisq2Model(value: BisqEasyOfferbookMessage): BisqEasyOfferbookMessageDto =
            BisqEasyOfferbookMessageDto(
                value.id,
                value.channelId,
                value.authorUserProfileId,
                value.bisqEasyOffer.getOrNull()?.let { BisqEasyOfferMapping.fromBisq2Model(it) },
                value.text.getOrNull(),
                value.citation.getOrNull()?.let { CitationMapping.fromBisq2Model(it) },
                value.date,
                value.isWasEdited,
                ChatMessageTypeMapping.fromBisq2Model(value.chatMessageType),
            )
    }

    // chat.bisq_easy.open_trades

    object BisqEasyOpenTradeChannelVOMapping {
        /* fun toBisq2Model(value: BisqEasyOpenTradeChannelVO): BisqEasyOpenTradeChannel {
             return BisqEasyOpenTradeChannel(
                 value.id,
                 value.tradeId,
                 BisqEasyOfferMapping.toBisq2Model(value.bisqEasyOffer),
                 UserIdentityMapping.toBisq2Model(value.myUserIdentity),
                 value.traders.map { UserProfileMapping.toBisq2Model(it) }.toSet(),
                 Optional.ofNullable(value.mediator?.let { UserProfileMapping.toBisq2Model(it) }),
                emptySet(),
                 false,
                 ChatChannelNotificationType.GLOBAL_DEFAULT,
             )
         }*/

        fun fromBisq2Model(value: BisqEasyOpenTradeChannel): BisqEasyOpenTradeChannelDto =
            BisqEasyOpenTradeChannelDto(
                value.id,
                value.tradeId,
                BisqEasyOfferMapping.fromBisq2Model(value.bisqEasyOffer),
                UserIdentityMapping.fromBisq2Model(value.myUserIdentity),
                value.traders.map { UserProfileMapping.fromBisq2Model(it) }.toSet(),
                value.mediator.getOrNull()?.let { UserProfileMapping.fromBisq2Model(it) },
            )
    }

    object BisqEasyOpenTradeMessageModelMapping {
        fun fromBisq2Model(
            message: BisqEasyOpenTradeMessage,
            citationAuthorUserProfile: UserProfile?,
            myUserProfile: UserProfile,
        ): BisqEasyOpenTradeMessageModel {
            userProfileDemoObj
            val citationAuthorUserProfileVO = citationAuthorUserProfile?.let { UserProfileMapping.fromBisq2Model(it) }
            val bisqEasyOpenTradeMessage = BisqEasyOpenTradeMessageVOMapping.fromBisq2Model(message, citationAuthorUserProfileVO)
            val myUserProfileVO = UserProfileMapping.fromBisq2Model(myUserProfile)
            val chatMessageReactions: List<BisqEasyOpenTradeMessageReactionVO> =
                message.chatMessageReactions
                    .filter { !it.isRemoved }
                    .map { reaction ->
                        BisqEasyOpenTradeMessageReactionMapping.fromBisq2Model(reaction)
                    }
            return BisqEasyOpenTradeMessageModel(
                bisqEasyOpenTradeMessage,
                myUserProfileVO,
                chatMessageReactions,
            )
        }
    }

    object BisqEasyOpenTradeMessageVOMapping {
        fun toBisq2Model(value: BisqEasyOpenTradeMessageDto): BisqEasyOpenTradeMessage {
            // citationAuthorUserProfile not required for Bisq Easy model
            return BisqEasyOpenTradeMessage(
                value.tradeId,
                value.messageId,
                value.channelId,
                UserProfileMapping.toBisq2Model(value.senderUserProfile),
                value.receiverUserProfileId,
                NetworkIdMapping.toBisq2Model(value.receiverNetworkId),
                value.text,
                Optional.ofNullable(value.citation?.let { CitationMapping.toBisq2Model(it) }),
                value.date,
                false,
                Optional.ofNullable(value.mediator?.let { UserProfileMapping.toBisq2Model(it) }),
                ChatMessageTypeMapping.toBisq2Model(value.chatMessageType),
                Optional.ofNullable(value.bisqEasyOffer?.let { BisqEasyOfferMapping.toBisq2Model(it) }),
                value.chatMessageReactions.map { BisqEasyOpenTradeMessageReactionMapping.toBisq2Model(it) }.toSet(),
            )
        }

        fun fromBisq2Model(
            value: BisqEasyOpenTradeMessage,
            citationAuthorUserProfileVO: UserProfileVO?,
        ): BisqEasyOpenTradeMessageDto =
            BisqEasyOpenTradeMessageDto(
                value.tradeId,
                value.id,
                value.channelId,
                UserProfileMapping.fromBisq2Model(value.senderUserProfile),
                value.receiverUserProfileId,
                NetworkIdMapping.fromBisq2Model(value.receiverNetworkId),
                value.text.getOrNull(),
                value.citation.getOrNull()?.let { CitationMapping.fromBisq2Model(it) },
                value.date,
                value.mediator.getOrNull()?.let { UserProfileMapping.fromBisq2Model(it) },
                ChatMessageTypeMapping.fromBisq2Model(value.chatMessageType),
                value.bisqEasyOffer.getOrNull()?.let { BisqEasyOfferMapping.fromBisq2Model(it) },
                value.chatMessageReactions.map { BisqEasyOpenTradeMessageReactionMapping.fromBisq2Model(it) }.toSet(),
                citationAuthorUserProfileVO,
            )
    }

    // chat.notifications

    object ChatChannelNotificationTypeMapping {
        fun toBisq2Model(value: ChatChannelNotificationTypeEnum): ChatChannelNotificationType =
            when (value) {
                ChatChannelNotificationTypeEnum.GLOBAL_DEFAULT -> ChatChannelNotificationType.GLOBAL_DEFAULT
                ChatChannelNotificationTypeEnum.ALL -> ChatChannelNotificationType.ALL
                ChatChannelNotificationTypeEnum.MENTION -> ChatChannelNotificationType.MENTION
                ChatChannelNotificationTypeEnum.OFF -> ChatChannelNotificationType.OFF
            }

        fun fromBisq2Model(value: ChatChannelNotificationType): ChatChannelNotificationTypeEnum =
            when (value) {
                ChatChannelNotificationType.GLOBAL_DEFAULT -> ChatChannelNotificationTypeEnum.GLOBAL_DEFAULT
                ChatChannelNotificationType.ALL -> ChatChannelNotificationTypeEnum.ALL
                ChatChannelNotificationType.MENTION -> ChatChannelNotificationTypeEnum.MENTION
                ChatChannelNotificationType.OFF -> ChatChannelNotificationTypeEnum.OFF
            }
    }

    // chat.reactions
    object BisqEasyOpenTradeMessageReactionMapping {
        fun toBisq2Model(value: BisqEasyOpenTradeMessageReactionVO): BisqEasyOpenTradeMessageReaction =
            BisqEasyOpenTradeMessageReaction(
                value.id,
                UserProfileMapping.toBisq2Model(value.senderUserProfile),
                value.receiverUserProfileId,
                NetworkIdMapping.toBisq2Model(value.receiverNetworkId),
                value.chatChannelId,
                ChatChannelDomainMapping.toBisq2Model(value.chatChannelDomain),
                value.chatMessageId,
                value.reactionId,
                value.date,
                value.isRemoved,
            )

        fun fromBisq2Model(value: BisqEasyOpenTradeMessageReaction): BisqEasyOpenTradeMessageReactionVO =
            BisqEasyOpenTradeMessageReactionVO(
                value.id,
                UserProfileMapping.fromBisq2Model(value.senderUserProfile),
                value.receiverUserProfileId,
                NetworkIdMapping.fromBisq2Model(value.receiverNetworkId),
                value.chatChannelId,
                ChatChannelDomainMapping.fromBisq2Model(value.chatChannelDomain),
                value.chatMessageId,
                value.reactionId,
                value.date,
                value.isRemoved,
            )
    }

    object BisqEasyOfferbookMessageReactionMapping {
        fun toBisq2Model(value: BisqEasyOfferbookMessageReactionVO): BisqEasyOfferbookMessageReaction =
            BisqEasyOfferbookMessageReaction(
                value.id,
                value.userProfileId,
                value.chatChannelId,
                ChatChannelDomainMapping.toBisq2Model(value.chatChannelDomain),
                value.chatMessageId,
                value.reactionId,
                value.date,
            )

        fun fromBisq2Model(value: BisqEasyOfferbookMessageReaction): BisqEasyOfferbookMessageReactionVO =
            BisqEasyOfferbookMessageReactionVO(
                value.id,
                value.userProfileId,
                value.chatChannelId,
                ChatChannelDomainMapping.fromBisq2Model(value.chatChannelDomain),
                value.chatMessageId,
                value.reactionId,
                value.date,
            )
    }

    object ReactionMapping {
        fun toBisq2Model(value: ReactionEnum): Reaction =
            when (value) {
                ReactionEnum.THUMBS_UP -> Reaction.THUMBS_UP
                ReactionEnum.THUMBS_DOWN -> Reaction.THUMBS_DOWN
                ReactionEnum.HAPPY -> Reaction.HAPPY
                ReactionEnum.LAUGH -> Reaction.LAUGH
                ReactionEnum.HEART -> Reaction.HEART
                ReactionEnum.PARTY -> Reaction.PARTY
            }

        fun fromBisq2Model(value: Reaction): ReactionEnum =
            when (value) {
                Reaction.THUMBS_UP -> ReactionEnum.THUMBS_UP
                Reaction.THUMBS_DOWN -> ReactionEnum.THUMBS_DOWN
                Reaction.HAPPY -> ReactionEnum.HAPPY
                Reaction.LAUGH -> ReactionEnum.LAUGH
                Reaction.HEART -> ReactionEnum.HEART
                Reaction.PARTY -> ReactionEnum.PARTY
            }
    }

    // common.currency

    object MarketMapping {
        fun toBisq2Model(value: MarketVO): Market =
            Market(
                value.baseCurrencyCode,
                value.quoteCurrencyCode,
                value.baseCurrencyName,
                value.quoteCurrencyName,
            )

        fun fromBisq2Model(value: Market): MarketVO =
            MarketVO(
                value.baseCurrencyCode,
                value.quoteCurrencyCode,
                value.baseCurrencyName,
                value.quoteCurrencyName,
            )
    }

    // common.monetary

    object CoinMapping {
        fun toBisq2Model(value: CoinVO): Coin = Coin(value.id, value.value, value.code, value.precision, value.lowPrecision)

        fun fromBisq2Model(value: Coin): CoinVO =
            CoinVOFactory.from(
                value.id,
                value.value,
                value.code,
                value.precision,
                value.lowPrecision,
            )
    }

    object FiatMapping {
        fun toBisq2Model(value: FiatVO): Fiat = Fiat(value.id, value.value, value.code, value.precision, value.lowPrecision)

        fun fromBisq2Model(value: Fiat): FiatVO = FiatVOFactory.from(value.id, value.value, value.code, value.precision, value.lowPrecision)
    }

    object MonetaryMapping {
        fun toBisq2Model(value: MonetaryVO): Monetary =
            if (value is FiatVO) {
                FiatMapping.toBisq2Model(value)
            } else {
                CoinMapping.toBisq2Model(value as CoinVO)
            }

        fun fromBisq2Model(value: Monetary): MonetaryVO =
            if (value is Fiat) {
                FiatVOFactory.from(
                    value.getId(),
                    value.getValue(),
                    value.getCode(),
                    value.getPrecision(),
                    value.getLowPrecision(),
                )
            } else {
                CoinVOFactory.from(
                    value.id,
                    value.value,
                    value.code,
                    value.precision,
                    value.lowPrecision,
                )
            }
    }

    object PriceQuoteMapping {
        fun toBisq2Model(value: PriceQuoteVO): PriceQuote {
            val baseCurrencyCode = value.market.baseCurrencyCode
            val quoteCurrencyCode = value.market.quoteCurrencyCode
            if (baseCurrencyCode == "BTC") {
                val baseSideMonetary: Monetary = Coin.asBtcFromFaceValue(1.0)
                val quoteSideMonetary: Monetary = Fiat.from(value.value, quoteCurrencyCode)
                return PriceQuote(value.value, baseSideMonetary, quoteSideMonetary)
            } else {
                throw UnsupportedOperationException("Altcoin price quote mapping is not supported yet")
            }
        }

        fun fromBisq2Model(value: PriceQuote): PriceQuoteVO =
            PriceQuoteVO(
                value.value,
                value.precision,
                value.lowPrecision,
                MarketMapping.fromBisq2Model(value.market),
                MonetaryMapping.fromBisq2Model(value.baseSideMonetary),
                MonetaryMapping.fromBisq2Model(value.quoteSideMonetary),
            )
    }

    // common.network

    object AddressByTransportTypeMapMapping {
        fun toBisq2Model(value: AddressByTransportTypeMapVO): AddressByTransportTypeMap =
            AddressByTransportTypeMap(
                value.map.entries.associate {
                    TransportTypeMapping.toBisq2Model(it.key) to AddressMapping.toBisq2Model(it.key, it.value)
                },
            )

        fun fromBisq2Model(value: AddressByTransportTypeMap): AddressByTransportTypeMapVO =
            AddressByTransportTypeMapVO(
                value.map.entries.associate {
                    TransportTypeMapping.fromBisq2Model(it.key) to AddressMapping.fromBisq2Model(it.value)
                },
            )
    }

    object AddressMapping {
        fun toBisq2Model(
            key: TransportTypeEnum,
            value: AddressVO,
        ): Address =
            when (key) {
                TransportTypeEnum.CLEAR -> ClearnetAddress(value.host, value.port)
                TransportTypeEnum.I2P -> I2PAddress(value.host, value.port)
                else -> TorAddress(value.host, value.port)
            }

        fun fromBisq2Model(value: Address): AddressVO = AddressVO(value.host, value.port)
    }

    object TransportTypeMapping {
        fun toBisq2Model(value: TransportTypeEnum): TransportType =
            if (value == TransportTypeEnum.CLEAR) {
                TransportType.CLEAR
            } else if (value == TransportTypeEnum.TOR) {
                TransportType.TOR
            } else if (value == TransportTypeEnum.I2P) {
                TransportType.I2P
            } else {
                throw IllegalArgumentException("Unsupported enum $value")
            }

        fun fromBisq2Model(value: TransportType): TransportTypeEnum =
            if (value == TransportType.CLEAR) {
                TransportTypeEnum.CLEAR
            } else if (value == TransportType.TOR) {
                TransportTypeEnum.TOR
            } else if (value == TransportType.I2P) {
                TransportTypeEnum.I2P
            } else {
                throw IllegalArgumentException("Unsupported enum $value")
            }
    }

    // contract

    object BisqEasyContractMapping {
        fun toBisq2Model(value: BisqEasyContractVO): BisqEasyContract =
            BisqEasyContract(
                value.takeOfferDate,
                BisqEasyOfferMapping.toBisq2Model(value.offer),
                TradeProtocolType.BISQ_EASY,
                PartyMapping.toBisq2Model(value.taker),
                value.baseSideAmount,
                value.quoteSideAmount,
                BitcoinPaymentMethodSpecMapping.toBisq2Model(value.baseSidePaymentMethodSpec),
                FiatPaymentMethodSpecMapping.toBisq2Model(value.quoteSidePaymentMethodSpec),
                Optional.ofNullable(value.mediator.let { UserProfileMapping.toBisq2Model(value.mediator!!) }),
                PriceSpecMapping.toBisq2Model(value.priceSpec),
                value.marketPrice,
            )

        fun fromBisq2Model(value: BisqEasyContract): BisqEasyContractVO =
            BisqEasyContractVO(
                value.takeOfferDate,
                BisqEasyOfferMapping.fromBisq2Model(value.offer),
                PartyMapping.fromBisq2Model(value.maker),
                PartyMapping.fromBisq2Model(value.taker),
                value.baseSideAmount,
                value.quoteSideAmount,
                BitcoinPaymentMethodSpecMapping.fromBisq2Model(value.baseSidePaymentMethodSpec),
                FiatPaymentMethodSpecMapping.fromBisq2Model(value.quoteSidePaymentMethodSpec),
                value.mediator.getOrNull()?.let { UserProfileMapping.fromBisq2Model(it) },
                PriceSpecMapping.fromBisq2Model(value.priceSpec),
                value.marketPrice,
            )
    }

    object ContractSignatureDataMapping {
        fun toBisq2Model(value: ContractSignatureDataVO): ContractSignatureData =
            ContractSignatureData(
                Base64.getDecoder().decode(value.contractHashEncoded),
                Base64.getDecoder().decode(value.signatureEncoded),
                PublicKeyMapping.toBisq2Model(value.publicKey),
            )

        fun fromBisq2Model(value: ContractSignatureData): ContractSignatureDataVO =
            ContractSignatureDataVO(
                Base64.getEncoder().encodeToString(value.contractHash),
                Base64.getEncoder().encodeToString(value.signature),
                PublicKeyMapping.fromBisq2Model(value.publicKey),
            )
    }

    object PartyMapping {
        fun toBisq2Model(value: PartyVO): Party = Party(RoleMapping.toBisq2Model(value.role), NetworkIdMapping.toBisq2Model(value.networkId))

        fun fromBisq2Model(value: Party): PartyVO = PartyVO(RoleMapping.fromBisq2Model(value.role), NetworkIdMapping.fromBisq2Model(value.networkId))
    }

    object RoleMapping {
        fun toBisq2Model(value: RoleEnum): Role =
            when (value) {
                RoleEnum.MAKER -> Role.MAKER
                RoleEnum.TAKER -> Role.TAKER
                RoleEnum.ESCROW_AGENT -> Role.ESCROW_AGENT
            }

        fun fromBisq2Model(value: Role): RoleEnum =
            when (value) {
                Role.MAKER -> RoleEnum.MAKER
                Role.TAKER -> RoleEnum.TAKER
                Role.ESCROW_AGENT -> RoleEnum.ESCROW_AGENT
            }
    }

    // identity

    object IdentityMapping {
        fun toBisq2Model(value: IdentityVO): Identity =
            Identity(
                value.tag,
                NetworkIdMapping.toBisq2Model(value.networkId),
                KeyBundleMapping.toBisq2Model(value.keyBundle),
            )

        fun fromBisq2Model(value: Identity): IdentityVO =
            IdentityVO(
                value.tag,
                NetworkIdMapping.fromBisq2Model(value.networkId),
                KeyBundleMapping.fromBisq2Model(value.keyBundle),
            )
    }

    // network.identity

    object NetworkIdMapping {
        fun toBisq2Model(value: NetworkIdVO): NetworkId =
            NetworkId(
                AddressByTransportTypeMapMapping.toBisq2Model(value.addressByTransportTypeMap),
                PubKeyMapping.toBisq2Model(value.pubKey),
            )

        fun fromBisq2Model(value: NetworkId): NetworkIdVO =
            NetworkIdVO(
                AddressByTransportTypeMapMapping.fromBisq2Model(value.addressByTransportTypeMap),
                PubKeyMapping.fromBisq2Model(value.pubKey),
            )
    }

    // network.p2p.services.confidential.ack

    object MessageDeliveryStatusMapping {
        fun toBisq2Model(value: MessageDeliveryStatusEnum): MessageDeliveryStatus =
            when (value) {
                MessageDeliveryStatusEnum.CONNECTING -> MessageDeliveryStatus.CONNECTING
                MessageDeliveryStatusEnum.SENT -> MessageDeliveryStatus.SENT
                MessageDeliveryStatusEnum.ACK_RECEIVED -> MessageDeliveryStatus.ACK_RECEIVED
                MessageDeliveryStatusEnum.TRY_ADD_TO_MAILBOX -> MessageDeliveryStatus.TRY_ADD_TO_MAILBOX
                MessageDeliveryStatusEnum.ADDED_TO_MAILBOX -> MessageDeliveryStatus.ADDED_TO_MAILBOX
                MessageDeliveryStatusEnum.MAILBOX_MSG_RECEIVED -> MessageDeliveryStatus.MAILBOX_MSG_RECEIVED
                MessageDeliveryStatusEnum.FAILED -> MessageDeliveryStatus.FAILED
            }

        fun fromBisq2Model(value: MessageDeliveryStatus): MessageDeliveryStatusEnum =
            when (value) {
                MessageDeliveryStatus.CONNECTING -> MessageDeliveryStatusEnum.CONNECTING
                MessageDeliveryStatus.SENT -> MessageDeliveryStatusEnum.SENT
                MessageDeliveryStatus.ACK_RECEIVED -> MessageDeliveryStatusEnum.ACK_RECEIVED
                MessageDeliveryStatus.TRY_ADD_TO_MAILBOX -> MessageDeliveryStatusEnum.TRY_ADD_TO_MAILBOX
                MessageDeliveryStatus.ADDED_TO_MAILBOX -> MessageDeliveryStatusEnum.ADDED_TO_MAILBOX
                MessageDeliveryStatus.MAILBOX_MSG_RECEIVED -> MessageDeliveryStatusEnum.MAILBOX_MSG_RECEIVED
                MessageDeliveryStatus.FAILED -> MessageDeliveryStatusEnum.FAILED
            }
    }

    // offer

    object DirectionMapping {
        fun toBisq2Model(value: DirectionEnum): Direction =
            if (value == DirectionEnum.BUY) {
                Direction.BUY
            } else {
                Direction.SELL
            }

        fun fromBisq2Model(value: Direction): DirectionEnum =
            if (value == Direction.BUY) {
                DirectionEnum.BUY
            } else {
                DirectionEnum.SELL
            }
    }

    // offer.amount.spec

    object AmountSpecMapping {
        fun toBisq2Model(value: AmountSpecVO): AmountSpec =
            if (value is RangeAmountSpecVO) {
                RangeAmountSpecMapping.toBisq2Model(value)
            } else {
                FixedAmountSpecMapping.toBisq2Model(value as FixedAmountSpecVO)
            }

        fun fromBisq2Model(value: AmountSpec): AmountSpecVO =
            if (value is RangeAmountSpec) {
                RangeAmountSpecMapping.fromBisq2Model(value)
            } else {
                FixedAmountSpecMapping.fromBisq2Model(value as FixedAmountSpec)
            }
    }

    object BaseSideFixedAmountSpecMapping {
        fun toBisq2Model(value: BaseSideFixedAmountSpecVO): BaseSideFixedAmountSpec = BaseSideFixedAmountSpec(value.amount)

        fun fromBisq2Model(value: BaseSideFixedAmountSpec): BaseSideFixedAmountSpecVO = BaseSideFixedAmountSpecVO(value.amount)
    }

    object BaseSideRangeAmountSpecMapping {
        fun toBisq2Model(value: BaseSideRangeAmountSpecVO): BaseSideRangeAmountSpec = BaseSideRangeAmountSpec(value.minAmount, value.maxAmount)

        fun fromBisq2Model(value: BaseSideRangeAmountSpec): BaseSideRangeAmountSpecVO = BaseSideRangeAmountSpecVO(value.minAmount, value.maxAmount)
    }

    object FixedAmountSpecMapping {
        fun toBisq2Model(value: FixedAmountSpecVO): FixedAmountSpec =
            if (value is BaseSideFixedAmountSpecVO) {
                BaseSideFixedAmountSpecMapping.toBisq2Model(value)
            } else if (value is QuoteSideFixedAmountSpecVO) {
                QuoteSideFixedAmountSpecMapping.toBisq2Model(value)
            } else {
                throw IllegalArgumentException("Unsupported FixedAmountSpecVO $value")
            }

        fun fromBisq2Model(value: FixedAmountSpec): FixedAmountSpecVO =
            if (value is BaseSideFixedAmountSpec) {
                BaseSideFixedAmountSpecMapping.fromBisq2Model(value)
            } else if (value is QuoteSideFixedAmountSpec) {
                QuoteSideFixedAmountSpecMapping.fromBisq2Model(value)
            } else {
                throw IllegalArgumentException("Unsupported FixedAmountSpec $value")
            }
    }

    object QuoteSideFixedAmountSpecMapping {
        fun toBisq2Model(value: QuoteSideFixedAmountSpecVO): QuoteSideFixedAmountSpec = QuoteSideFixedAmountSpec(value.amount)

        fun fromBisq2Model(value: QuoteSideFixedAmountSpec): QuoteSideFixedAmountSpecVO = QuoteSideFixedAmountSpecVO(value.amount)
    }

    object QuoteSideRangeAmountSpecMapping {
        fun toBisq2Model(value: QuoteSideRangeAmountSpecVO): QuoteSideRangeAmountSpec = QuoteSideRangeAmountSpec(value.minAmount, value.maxAmount)

        fun fromBisq2Model(value: QuoteSideRangeAmountSpec): QuoteSideRangeAmountSpecVO = QuoteSideRangeAmountSpecVO(value.minAmount, value.maxAmount)
    }

    object RangeAmountSpecMapping {
        fun toBisq2Model(value: RangeAmountSpecVO): RangeAmountSpec =
            if (value is BaseSideRangeAmountSpecVO) {
                BaseSideRangeAmountSpecMapping.toBisq2Model(value)
            } else if (value is QuoteSideRangeAmountSpecVO) {
                QuoteSideRangeAmountSpecMapping.toBisq2Model(value)
            } else {
                throw IllegalArgumentException("Unsupported RangeAmountSpecVO $value")
            }

        fun fromBisq2Model(value: RangeAmountSpec): RangeAmountSpecVO =
            if (value is BaseSideRangeAmountSpec) {
                BaseSideRangeAmountSpecMapping.fromBisq2Model(value)
            } else if (value is QuoteSideRangeAmountSpec) {
                QuoteSideRangeAmountSpecMapping.fromBisq2Model(value)
            } else {
                throw IllegalArgumentException("Unsupported RangeAmountSpec $value")
            }
    }

    // offer.bisq_easy

    object BisqEasyOfferMapping {
        fun toBisq2Model(value: BisqEasyOfferVO): BisqEasyOffer =
            BisqEasyOffer(
                value.id,
                value.date,
                NetworkIdMapping.toBisq2Model(value.makerNetworkId),
                DirectionMapping.toBisq2Model(value.direction),
                MarketMapping.toBisq2Model(value.market),
                AmountSpecMapping.toBisq2Model(value.amountSpec),
                PriceSpecMapping.toBisq2Model(value.priceSpec),
                value.protocolTypes.map { TradeProtocolTypeMapping.toBisq2Model(it) }.toList(),
                value.baseSidePaymentMethodSpecs.map { BitcoinPaymentMethodSpecMapping.toBisq2Model(it) }.toList(),
                value.quoteSidePaymentMethodSpecs.map { FiatPaymentMethodSpecMapping.toBisq2Model(it) }.toList(),
                value.offerOptions.map { OfferOptionMapping.toBisq2Model(it) }.toList(),
                value.supportedLanguageCodes,
                BuildNodeConfig.TRADE_OFFER_VERSION,
                BuildNodeConfig.TRADE_PROTOCOL_VERSION,
                BuildNodeConfig.APP_VERSION,
            )

        fun fromBisq2Model(value: BisqEasyOffer): BisqEasyOfferVO =
            BisqEasyOfferVO(
                value.id,
                value.date,
                NetworkIdMapping.fromBisq2Model(value.makerNetworkId),
                DirectionMapping.fromBisq2Model(value.direction),
                MarketMapping.fromBisq2Model(value.market),
                AmountSpecMapping.fromBisq2Model(value.amountSpec),
                PriceSpecMapping.fromBisq2Model(value.priceSpec),
                value.protocolTypes.map { TradeProtocolTypeMapping.fromBisq2Model(it) },
                value.baseSidePaymentMethodSpecs.map { BitcoinPaymentMethodSpecMapping.fromBisq2Model(it) },
                value.quoteSidePaymentMethodSpecs.map { FiatPaymentMethodSpecMapping.fromBisq2Model(it) },
                value.offerOptions.map { OfferOptionMapping.fromBisq2Model(it) },
                value.supportedLanguageCodes,
            )
    }

    // offer.options

    object OfferOptionMapping {
        fun toBisq2Model(value: OfferOptionVO): OfferOption =
            if (value is ReputationOptionVO) {
                ReputationOptionMapping.toBisq2Model(value)
            } else if (value is TradeTermsOptionVO) {
                TradeTermsOptionMapping.toBisq2Model(value)
            } else {
                throw IllegalArgumentException("Unsupported OfferOptionVO $value")
            }

        fun fromBisq2Model(value: OfferOption): OfferOptionVO =
            if (value is ReputationOption) {
                @Suppress("DEPRECATION")
                ReputationOptionVO(value.requiredTotalReputationScore)
            } else if (value is TradeTermsOption) {
                TradeTermsOptionVO(value.makersTradeTerms)
            } else {
                throw IllegalArgumentException("Unsupported OfferOption $value")
            }
    }

    object ReputationOptionMapping {
        fun toBisq2Model(value: ReputationOptionVO): ReputationOption = ReputationOption(value.requiredTotalReputationScore)

        fun fromBisq2Model(value: ReputationOption): ReputationOptionVO {
            @Suppress("DEPRECATION")
            return ReputationOptionVO(value.requiredTotalReputationScore)
        }
    }

    object TradeTermsOptionMapping {
        fun toBisq2Model(value: TradeTermsOptionVO): TradeTermsOption = TradeTermsOption(value.makersTradeTerms)

        fun fromBisq2Model(value: TradeTermsOption): TradeTermsOptionVO = TradeTermsOptionVO(value.makersTradeTerms)
    }

    // offer.payment_method

    object BitcoinPaymentMethodSpecMapping {
        fun toBisq2Model(value: BitcoinPaymentMethodSpecVO): BitcoinPaymentMethodSpec {
            val paymentMethod = value.paymentMethod
            val method = PaymentMethodSpecUtil.getBitcoinPaymentMethod(paymentMethod)
            return BitcoinPaymentMethodSpec(method, Optional.ofNullable(value.saltedMakerAccountId))
        }

        fun fromBisq2Model(value: BitcoinPaymentMethodSpec): BitcoinPaymentMethodSpecVO =
            BitcoinPaymentMethodSpecVO(
                value.paymentMethod.paymentRailName,
                value.saltedMakerAccountId.orElse(null),
            )
    }

    object FiatPaymentMethodSpecMapping {
        fun toBisq2Model(value: FiatPaymentMethodSpecVO): FiatPaymentMethodSpec {
            val paymentMethod = value.paymentMethod
            val method = PaymentMethodSpecUtil.getFiatPaymentMethod(paymentMethod)
            return FiatPaymentMethodSpec(method, Optional.ofNullable(value.saltedMakerAccountId))
        }

        fun fromBisq2Model(value: FiatPaymentMethodSpec): FiatPaymentMethodSpecVO =
            FiatPaymentMethodSpecVO(
                value.paymentMethod.paymentRailName,
                value.saltedMakerAccountId.orElse(null),
            )
    }

    object PaymentMethodSpecMapping {
        fun toBisq2Model(value: PaymentMethodSpecVO): PaymentMethodSpec<*> =
            if (value is FiatPaymentMethodSpecVO) {
                FiatPaymentMethodSpecMapping.toBisq2Model(value)
            } else if (value is BitcoinPaymentMethodSpecVO) {
                BitcoinPaymentMethodSpecMapping.toBisq2Model(value)
            } else {
                throw IllegalArgumentException("Unsupported PaymentMethodSpecVO $value")
            }

        fun fromBisq2Model(value: PaymentMethodSpec<*>): PaymentMethodSpecVO =
            if (value is FiatPaymentMethodSpec) {
                FiatPaymentMethodSpecMapping.fromBisq2Model(value)
            } else if (value is BitcoinPaymentMethodSpec) {
                BitcoinPaymentMethodSpecMapping.fromBisq2Model(value)
            } else {
                throw IllegalArgumentException("Unsupported PaymentMethodSpec $value")
            }
    }

    // offer.price.spec

    object MarketPriceSpecMapping {
        fun toBisq2Model(value: MarketPriceSpecVO): MarketPriceSpec = MarketPriceSpec()

        fun fromBisq2Model(value: MarketPriceSpec): MarketPriceSpecVO = MarketPriceSpecVO()
    }

    object FloatPriceSpecMapping {
        fun toBisq2Model(value: FloatPriceSpecVO): FloatPriceSpec = FloatPriceSpec(value.percentage)

        fun fromBisq2Model(value: FloatPriceSpec): FloatPriceSpecVO = FloatPriceSpecVO(value.percentage)
    }

    object FixPriceSpecMapping {
        fun toBisq2Model(value: FixPriceSpecVO): FixPriceSpec = FixPriceSpec(PriceQuoteMapping.toBisq2Model(value.priceQuote))

        fun fromBisq2Model(value: FixPriceSpec): FixPriceSpecVO = FixPriceSpecVO(PriceQuoteMapping.fromBisq2Model(value.priceQuote))
    }

    object PriceSpecMapping {
        fun toBisq2Model(value: PriceSpecVO): PriceSpec =
            when (value) {
                is MarketPriceSpecVO -> MarketPriceSpecMapping.toBisq2Model(value)
                is FixPriceSpecVO -> FixPriceSpecMapping.toBisq2Model(value)
                is FloatPriceSpecVO -> FloatPriceSpecMapping.toBisq2Model(value)
                else -> throw IllegalArgumentException("Unsupported PriceSpecVO $value")
            }

        fun fromBisq2Model(value: PriceSpec): PriceSpecVO =
            when (value) {
                is MarketPriceSpec -> MarketPriceSpecMapping.fromBisq2Model(value)
                is FixPriceSpec -> FixPriceSpecMapping.fromBisq2Model(value)
                is FloatPriceSpec -> FloatPriceSpecMapping.fromBisq2Model(value)
                else -> throw IllegalArgumentException("Unsupported PriceSpec $value")
            }
    }

    // security.keys

    object PrivateKeyMapping {
        fun toBisq2Model(value: PrivateKeyVO): PrivateKey =
            try {
                val decoded = Base64.getDecoder().decode(value.encoded)
                KeyGeneration.generatePrivate(decoded)
            } catch (e: Exception) {
                throw RuntimeException("Failed to generate privateKey", e)
            }

        fun fromBisq2Model(value: PrivateKey): PrivateKeyVO = PrivateKeyVO(Base64.getEncoder().encodeToString(value.encoded))
    }

    object KeyPairMapping {
        fun toBisq2Model(value: KeyPairVO): KeyPair {
            val publicKey = PublicKeyMapping.toBisq2Model(value.publicKey)
            val privateKey = PrivateKeyMapping.toBisq2Model(value.privateKey)
            return KeyPair(publicKey, privateKey)
        }

        fun fromBisq2Model(value: KeyPair): KeyPairVO {
            val privateKeyVO = PrivateKeyMapping.fromBisq2Model(value.private)
            val publicKeyVO = PublicKeyMapping.fromBisq2Model(value.public)
            return KeyPairVO(publicKeyVO, privateKeyVO)
        }
    }

    object PubKeyMapping {
        fun toBisq2Model(value: PubKeyVO): PubKey = PubKey(PublicKeyMapping.toBisq2Model(value.publicKey), value.keyId)

        fun fromBisq2Model(value: PubKey): PubKeyVO {
            val publicKey = value.publicKey
            val publicKeyVO = PublicKeyMapping.fromBisq2Model(publicKey)
            val keyId = value.keyId
            val hash = DigestUtil.hash(publicKey.encoded)
            val hashBase64 = Base64.getEncoder().encodeToString(hash)
            val id = Hex.encode(hash)
            return PubKeyVO(publicKeyVO, keyId, hashBase64, id)
        }
    }

    object PublicKeyMapping {
        fun toBisq2Model(value: PublicKeyVO): PublicKey {
            try {
                val bytes: ByteArray = Base64.getDecoder().decode(value.encoded)
                return KeyGeneration.generatePublic(bytes)
            } catch (e: Exception) {
                throw RuntimeException("Failed to deserialize publicKey", e)
            }
        }

        fun fromBisq2Model(value: PublicKey): PublicKeyVO = PublicKeyVO(Base64.getEncoder().encodeToString(value.encoded))
    }

    object KeyBundleMapping {
        fun toBisq2Model(value: KeyBundleVO): KeyBundle =
            KeyBundle(
                value.keyId,
                KeyPairMapping.toBisq2Model(value.keyPair),
                TorKeyPairMapping.toBisq2Model(value.torKeyPair),
                null, // TODO when supporting i2p gets implemented
            )

        fun fromBisq2Model(value: KeyBundle): KeyBundleVO =
            KeyBundleVO(
                value.keyId,
                KeyPairMapping.fromBisq2Model(value.keyPair),
                TorKeyPairMapping.fromBisq2Model(value.torKeyPair),
                null, // TODO when supporting i2p gets implemented
            )
    }

    object TorKeyPairMapping {
        fun toBisq2Model(value: TorKeyPairVO): TorKeyPair =
            TorKeyPair(
                Base64.getDecoder().decode(value.privateKeyEncoded),
                Base64.getDecoder().decode(value.publicKeyEncoded),
                value.onionAddress,
            )

        fun fromBisq2Model(value: TorKeyPair): TorKeyPairVO =
            TorKeyPairVO(
                Base64.getEncoder().encodeToString(value.privateKey),
                Base64.getEncoder().encodeToString(value.publicKey),
                value.onionAddress,
            )
    }

    // security.pow

    object ProofOfWorkMapping {
        fun toBisq2Model(value: ProofOfWorkVO): ProofOfWork =
            ProofOfWork(
                Base64.getDecoder().decode(value.payloadEncoded),
                value.counter,
                value.challengeEncoded?.let { Base64.getDecoder().decode(it) },
                value.difficulty,
                Base64.getDecoder().decode(value.solutionEncoded),
                value.duration,
            )

        fun fromBisq2Model(value: ProofOfWork): ProofOfWorkVO =
            ProofOfWorkVO(
                Base64.getEncoder().encodeToString(value.payload),
                value.counter,
                value.challenge?.let { Base64.getEncoder().encodeToString(it) },
                value.difficulty,
                Base64.getEncoder().encodeToString(value.solution),
                value.duration,
            )
    }

    // settings

    object SettingsMapping {
        // toPojo method not implemented as we do not have a settings value object in the domain
        fun from(settingsService: SettingsService): SettingsVO =
            SettingsVO(
                settingsService.isTacAccepted.get(),
                settingsService.tradeRulesConfirmed.get(),
                settingsService.closeMyOfferWhenTaken.get(),
                settingsService.languageTag.get(),
                settingsService.supportedLanguageTags,
                settingsService.maxTradePriceDeviation.get(),
                settingsService.useAnimations.get(),
                MarketMapping.fromBisq2Model(settingsService.selectedMuSigMarket.get()),
                settingsService.numDaysAfterRedactingTradeData.get(),
            )
    }

    // payment accounts

    // trade

    object BisqEasyTradeVOMapping {
        fun fromBisq2Model(value: BisqEasyTrade): BisqEasyTradeDto =
            BisqEasyTradeDto(
                BisqEasyContractMapping.fromBisq2Model(value.contract),
                value.id,
                TradeRoleMapping.fromBisq2Model(value.tradeRole),
                IdentityMapping.fromBisq2Model(value.myIdentity),
                BisqEasyTradePartyVOMapping.fromBisq2Model(value.taker),
                BisqEasyTradePartyVOMapping.fromBisq2Model(value.maker),
                BisqEasyTradeStateMapping.fromBisq2Model(value.tradeState),
                value.paymentAccountData.get(),
                value.bitcoinPaymentData.get(),
                value.paymentProof.get(),
                value.interruptTradeInitiator.get()?.let { RoleMapping.fromBisq2Model(it) },
                value.errorMessage,
                value.errorStackTrace,
                value.peersErrorMessage,
                value.peersErrorStackTrace,
            )
    }

    object BisqEasyTradeModelMapping {
        fun fromBisq2Model(value: BisqEasyTrade): BisqEasyTradeModel =
            BisqEasyTradeModel(
                BisqEasyTradeVOMapping.fromBisq2Model(value),
            ).apply {
                // We set initial values if mutable data
                // We update the data with observers
                tradeState.value = BisqEasyTradeStateMapping.fromBisq2Model(value.tradeState)
                interruptTradeInitiator.value =
                    value.interruptTradeInitiator.get()?.let { RoleMapping.fromBisq2Model(it) }
                paymentAccountData.value = value.paymentAccountData.get()
                bitcoinPaymentData.value = value.bitcoinPaymentData.get()
                paymentProof.value = value.paymentProof.get()
                errorMessage.value = value.errorMessage
                errorStackTrace.value = value.errorStackTrace
                peersErrorMessage.value = value.peersErrorMessage
                peersErrorStackTrace.value = value.peersErrorStackTrace
            }
    }

    object BisqEasyTradePartyVOMapping {
        fun fromBisq2Model(value: BisqEasyTradeParty): BisqEasyTradePartyVO = BisqEasyTradePartyVO(NetworkIdMapping.fromBisq2Model(value.networkId))
    }

    object TradeRoleMapping {
        fun toBisq2Model(value: TradeRoleEnum): TradeRole =
            when (value) {
                TradeRoleEnum.BUYER_AS_TAKER -> TradeRole.BUYER_AS_TAKER
                TradeRoleEnum.BUYER_AS_MAKER -> TradeRole.BUYER_AS_MAKER
                TradeRoleEnum.SELLER_AS_TAKER -> TradeRole.SELLER_AS_TAKER
                TradeRoleEnum.SELLER_AS_MAKER -> TradeRole.SELLER_AS_MAKER
            }

        fun fromBisq2Model(value: TradeRole): TradeRoleEnum =
            when (value) {
                TradeRole.BUYER_AS_TAKER -> TradeRoleEnum.BUYER_AS_TAKER
                TradeRole.BUYER_AS_MAKER -> TradeRoleEnum.BUYER_AS_MAKER
                TradeRole.SELLER_AS_TAKER -> TradeRoleEnum.SELLER_AS_TAKER
                TradeRole.SELLER_AS_MAKER -> TradeRoleEnum.SELLER_AS_MAKER
            }
    }

    // trade.bisq_easy.protocol

    object BisqEasyTradeStateMapping {
        fun toBisq2Model(value: BisqEasyTradeStateEnum): BisqEasyTradeState =
            when (value) {
                BisqEasyTradeStateEnum.INIT -> BisqEasyTradeState.INIT
                BisqEasyTradeStateEnum.TAKER_SENT_TAKE_OFFER_REQUEST -> BisqEasyTradeState.TAKER_SENT_TAKE_OFFER_REQUEST
                BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA -> BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA
                BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA -> BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA
                BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA -> BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA
                BisqEasyTradeStateEnum.TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA -> BisqEasyTradeState.TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA
                BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA_ -> BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA_
                BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA -> BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA
                BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS -> BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS
                BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS -> BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS
                BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS -> BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS
                BisqEasyTradeStateEnum.MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS -> BisqEasyTradeState.MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS
                BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS_ -> BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS_
                BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS -> BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS
                BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS -> BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS
                BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS -> BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS
                BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS -> BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS
                BisqEasyTradeStateEnum.TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS -> BisqEasyTradeState.TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS
                BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS_ -> BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS_
                BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS -> BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS
                BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA -> BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA
                BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA -> BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA
                BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA -> BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA
                BisqEasyTradeStateEnum.MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA -> BisqEasyTradeState.MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA
                BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA_ -> BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA_
                BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA -> BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA
                BisqEasyTradeStateEnum.BUYER_SENT_FIAT_SENT_CONFIRMATION -> BisqEasyTradeState.BUYER_SENT_FIAT_SENT_CONFIRMATION
                BisqEasyTradeStateEnum.SELLER_RECEIVED_FIAT_SENT_CONFIRMATION -> BisqEasyTradeState.SELLER_RECEIVED_FIAT_SENT_CONFIRMATION
                BisqEasyTradeStateEnum.SELLER_CONFIRMED_FIAT_RECEIPT -> BisqEasyTradeState.SELLER_CONFIRMED_FIAT_RECEIPT
                BisqEasyTradeStateEnum.BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION -> BisqEasyTradeState.BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION
                BisqEasyTradeStateEnum.SELLER_SENT_BTC_SENT_CONFIRMATION -> BisqEasyTradeState.SELLER_SENT_BTC_SENT_CONFIRMATION
                BisqEasyTradeStateEnum.BUYER_RECEIVED_BTC_SENT_CONFIRMATION -> BisqEasyTradeState.BUYER_RECEIVED_BTC_SENT_CONFIRMATION
                BisqEasyTradeStateEnum.BTC_CONFIRMED -> BisqEasyTradeState.BTC_CONFIRMED
                BisqEasyTradeStateEnum.REJECTED -> BisqEasyTradeState.REJECTED
                BisqEasyTradeStateEnum.PEER_REJECTED -> BisqEasyTradeState.PEER_REJECTED
                BisqEasyTradeStateEnum.CANCELLED -> BisqEasyTradeState.CANCELLED
                BisqEasyTradeStateEnum.PEER_CANCELLED -> BisqEasyTradeState.PEER_CANCELLED
                BisqEasyTradeStateEnum.FAILED -> BisqEasyTradeState.FAILED
                BisqEasyTradeStateEnum.FAILED_AT_PEER -> BisqEasyTradeState.FAILED_AT_PEER
            }

        fun fromBisq2Model(value: BisqEasyTradeState): BisqEasyTradeStateEnum =
            when (value) {
                BisqEasyTradeState.INIT -> BisqEasyTradeStateEnum.INIT
                BisqEasyTradeState.TAKER_SENT_TAKE_OFFER_REQUEST -> BisqEasyTradeStateEnum.TAKER_SENT_TAKE_OFFER_REQUEST
                BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA -> BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA
                BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA -> BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA
                BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA -> BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA
                BisqEasyTradeState.TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA -> BisqEasyTradeStateEnum.TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA
                BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA_ -> BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA_
                BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA -> BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA
                BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS -> BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS
                BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS -> BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS
                BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS -> BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS
                BisqEasyTradeState.MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS -> BisqEasyTradeStateEnum.MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS
                BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS_ -> BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS_
                BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS -> BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS
                BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS -> BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS
                BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS -> BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS
                BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS -> BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS
                BisqEasyTradeState.TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS -> BisqEasyTradeStateEnum.TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS
                BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS_ -> BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS_
                BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS -> BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS
                BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA -> BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA
                BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA -> BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA
                BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA -> BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA
                BisqEasyTradeState.MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA -> BisqEasyTradeStateEnum.MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA
                BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA_ -> BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA_
                BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA -> BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA
                BisqEasyTradeState.BUYER_SENT_FIAT_SENT_CONFIRMATION -> BisqEasyTradeStateEnum.BUYER_SENT_FIAT_SENT_CONFIRMATION
                BisqEasyTradeState.SELLER_RECEIVED_FIAT_SENT_CONFIRMATION -> BisqEasyTradeStateEnum.SELLER_RECEIVED_FIAT_SENT_CONFIRMATION
                BisqEasyTradeState.SELLER_CONFIRMED_FIAT_RECEIPT -> BisqEasyTradeStateEnum.SELLER_CONFIRMED_FIAT_RECEIPT
                BisqEasyTradeState.BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION -> BisqEasyTradeStateEnum.BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION
                BisqEasyTradeState.SELLER_SENT_BTC_SENT_CONFIRMATION -> BisqEasyTradeStateEnum.SELLER_SENT_BTC_SENT_CONFIRMATION
                BisqEasyTradeState.BUYER_RECEIVED_BTC_SENT_CONFIRMATION -> BisqEasyTradeStateEnum.BUYER_RECEIVED_BTC_SENT_CONFIRMATION
                BisqEasyTradeState.BTC_CONFIRMED -> BisqEasyTradeStateEnum.BTC_CONFIRMED
                BisqEasyTradeState.REJECTED -> BisqEasyTradeStateEnum.REJECTED
                BisqEasyTradeState.PEER_REJECTED -> BisqEasyTradeStateEnum.PEER_REJECTED
                BisqEasyTradeState.CANCELLED -> BisqEasyTradeStateEnum.CANCELLED
                BisqEasyTradeState.PEER_CANCELLED -> BisqEasyTradeStateEnum.PEER_CANCELLED
                BisqEasyTradeState.FAILED -> BisqEasyTradeStateEnum.FAILED
                BisqEasyTradeState.FAILED_AT_PEER -> BisqEasyTradeStateEnum.FAILED_AT_PEER
            }
    }

    // user.identity

    object UserIdentityMapping {
        fun toBisq2Model(value: UserIdentityVO): UserIdentity =
            UserIdentity(
                IdentityMapping.toBisq2Model(value.identity),
                UserProfileMapping.toBisq2Model(value.userProfile),
            )

        fun fromBisq2Model(value: UserIdentity): UserIdentityVO =
            UserIdentityVO(
                IdentityMapping.fromBisq2Model(value.identity),
                UserProfileMapping.fromBisq2Model(value.userProfile),
            )
    }

    // user.profile

    object UserProfileMapping {
        fun toBisq2Model(value: UserProfileVO): UserProfile =
            UserProfile(
                value.version,
                value.nickName,
                ProofOfWorkMapping.toBisq2Model(value.proofOfWork),
                value.avatarVersion,
                NetworkIdMapping.toBisq2Model(value.networkId),
                value.terms,
                value.statement,
                value.applicationVersion,
            )

        fun fromBisq2Model(value: UserProfile): UserProfileVO =
            UserProfileVO(
                value.version,
                value.nickName,
                ProofOfWorkMapping.fromBisq2Model(value.proofOfWork),
                value.avatarVersion,
                NetworkIdMapping.fromBisq2Model(value.networkId),
                value.terms,
                value.statement,
                value.applicationVersion,
                value.nym,
                value.userName,
                value.publishDate,
            )
    }

    // user.reputation

    object ReputationScoreMapping {
        fun toBisq2Model(value: ReputationScoreVO): ReputationScore = ReputationScore(value.totalScore, value.fiveSystemScore, value.ranking)

        fun fromBisq2Model(value: ReputationScore): ReputationScoreVO = ReputationScoreVO(value.totalScore, value.fiveSystemScore, value.ranking)
    }
}
