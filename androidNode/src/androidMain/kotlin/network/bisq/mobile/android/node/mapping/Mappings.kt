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
package network.bisq.mobile.android.node.mapping

import bisq.account.protocol_type.TradeProtocolType
import bisq.common.currency.Market
import bisq.common.encoding.Hex
import bisq.common.monetary.Coin
import bisq.common.monetary.Fiat
import bisq.common.monetary.Monetary
import bisq.common.monetary.PriceQuote
import bisq.common.network.Address
import bisq.common.network.AddressByTransportTypeMap
import bisq.common.network.TransportType
import bisq.network.identity.NetworkId
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
import bisq.offer.payment_method.BitcoinPaymentMethodSpec
import bisq.offer.payment_method.FiatPaymentMethodSpec
import bisq.offer.payment_method.PaymentMethodSpec
import bisq.offer.payment_method.PaymentMethodSpecUtil
import bisq.offer.price.spec.FixPriceSpec
import bisq.offer.price.spec.FloatPriceSpec
import bisq.offer.price.spec.MarketPriceSpec
import bisq.offer.price.spec.PriceSpec
import bisq.security.DigestUtil
import bisq.security.keys.KeyGeneration
import bisq.security.keys.PubKey
import bisq.security.pow.ProofOfWork
import bisq.user.profile.UserProfile
import bisq.user.reputation.ReputationScore
import network.bisq.mobile.domain.replicated.account.protocol_type.TradeProtocolTypeEnum
import network.bisq.mobile.domain.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.replicated.common.monetary.CoinVO
import network.bisq.mobile.domain.replicated.common.monetary.FiatVO
import network.bisq.mobile.domain.replicated.common.monetary.MonetaryVO
import network.bisq.mobile.domain.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.domain.replicated.common.monetary.from
import network.bisq.mobile.domain.replicated.common.network.AddressByTransportTypeMapVO
import network.bisq.mobile.domain.replicated.common.network.AddressVO
import network.bisq.mobile.domain.replicated.common.network.TransportTypeEnum
import network.bisq.mobile.domain.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.domain.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.replicated.offer.amount.spec.AmountSpecVO
import network.bisq.mobile.domain.replicated.offer.amount.spec.BaseSideFixedAmountSpecVO
import network.bisq.mobile.domain.replicated.offer.amount.spec.BaseSideRangeAmountSpecVO
import network.bisq.mobile.domain.replicated.offer.amount.spec.FixedAmountSpecVO
import network.bisq.mobile.domain.replicated.offer.amount.spec.QuoteSideFixedAmountSpecVO
import network.bisq.mobile.domain.replicated.offer.amount.spec.QuoteSideRangeAmountSpecVO
import network.bisq.mobile.domain.replicated.offer.amount.spec.RangeAmountSpecVO
import network.bisq.mobile.domain.replicated.offer.amount.spec.from
import network.bisq.mobile.domain.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.domain.replicated.offer.options.OfferOptionVO
import network.bisq.mobile.domain.replicated.offer.options.ReputationOptionVO
import network.bisq.mobile.domain.replicated.offer.options.TradeTermsOptionVO
import network.bisq.mobile.domain.replicated.offer.options.from
import network.bisq.mobile.domain.replicated.offer.payment_method.BitcoinPaymentMethodSpecVO
import network.bisq.mobile.domain.replicated.offer.payment_method.FiatPaymentMethodSpecVO
import network.bisq.mobile.domain.replicated.offer.payment_method.PaymentMethodSpecVO
import network.bisq.mobile.domain.replicated.offer.payment_method.from
import network.bisq.mobile.domain.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.domain.replicated.offer.price.spec.FloatPriceSpecVO
import network.bisq.mobile.domain.replicated.offer.price.spec.MarketPriceSpecVO
import network.bisq.mobile.domain.replicated.offer.price.spec.PriceSpecVO
import network.bisq.mobile.domain.replicated.offer.price.spec.from
import network.bisq.mobile.domain.replicated.security.keys.KeyPairVO
import network.bisq.mobile.domain.replicated.security.keys.PrivateKeyVO
import network.bisq.mobile.domain.replicated.security.keys.PubKeyVO
import network.bisq.mobile.domain.replicated.security.keys.PublicKeyVO
import network.bisq.mobile.domain.replicated.security.pow.ProofOfWorkVO
import network.bisq.mobile.domain.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.replicated.user.reputation.ReputationScoreVO
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.util.Base64
import java.util.Optional

class Mappings {

    object PrivateKeyMapping {
        fun toPojo(vo: PrivateKeyVO): PrivateKey {
            return try {
                val decoded = Base64.getDecoder().decode(vo.encoded)
                KeyGeneration.generatePrivate(decoded)
            } catch (e: Exception) {
                throw RuntimeException("Failed to generate privateKey", e)
            }
        }

        fun from(value: PrivateKey): PrivateKeyVO {
            return PrivateKeyVO(Base64.getEncoder().encodeToString(value.encoded))
        }
    }

    object KeyPairMapping {
        fun toPojo(vo: KeyPairVO): KeyPair {
            val publicKey = PublicKeyMapping.toPojo(vo.publicKey)
            val privateKey = PrivateKeyMapping.toPojo(vo.privateKey)
            return KeyPair(publicKey, privateKey)
        }

        fun from(value: KeyPair): KeyPairVO {
            val privateKeyVO = PrivateKeyMapping.from(value.private)
            val publicKeyVO = PublicKeyMapping.from(value.public)
            return KeyPairVO(publicKeyVO, privateKeyVO)
        }
    }

    object PubKeyMapping {
        fun toPojo(vo: PubKeyVO): PubKey {
            return PubKey(PublicKeyMapping.toPojo(vo.publicKey), vo.keyId)
        }

        fun from(value: PubKey): PubKeyVO {
            val publicKey = value.publicKey
            val publicKeyVO = PublicKeyMapping.from(publicKey)
            val keyId = value.keyId
            val hash = DigestUtil.hash(publicKey.encoded)
            val hashBase64 = Base64.getEncoder().encodeToString(hash)
            val id = Hex.encode(hash)
            return PubKeyVO(publicKeyVO, keyId, hashBase64, id)
        }
    }

    object PublicKeyMapping {
        fun toPojo(vo: PublicKeyVO): PublicKey {
            try {
                val bytes: ByteArray = Base64.getDecoder().decode(vo.encoded)
                return KeyGeneration.generatePublic(bytes)
            } catch (e: Exception) {
                throw RuntimeException("Failed to deserialize publicKey", e)
            }
        }

        fun from(value: PublicKey): PublicKeyVO {
            return PublicKeyVO(Base64.getEncoder().encodeToString(value.encoded))
        }
    }

    object ProofOfWorkMapping {
        fun toPojo(vo: ProofOfWorkVO): ProofOfWork {
            return ProofOfWork(
                Base64.getDecoder().decode(vo.payload),
                vo.counter,
                vo.challenge?.let { Base64.getDecoder().decode(it) },
                vo.difficulty,
                Base64.getDecoder().decode(vo.solution),
                vo.duration
            )
        }

        fun from(value: ProofOfWork): ProofOfWorkVO {
            return ProofOfWorkVO(
                Base64.getEncoder().encodeToString(value.payload),
                value.counter,
                value.challenge?.let { Base64.getEncoder().encodeToString(it) },
                value.difficulty,
                Base64.getEncoder().encodeToString(value.solution),
                value.duration
            )
        }
    }

    object UserProfileMapping {
        fun toPojo(vo: UserProfileVO): UserProfile {
            return UserProfile(
                vo.version,
                vo.nickName,
                ProofOfWorkMapping.toPojo(vo.proofOfWork),
                vo.avatarVersion,
                NetworkIdMapping.toPojo(vo.networkId),
                vo.terms,
                vo.statement,
                vo.applicationVersion
            )
        }

        fun from(value: UserProfile): UserProfileVO {
            return UserProfileVO(
                value.version,
                value.nickName,
                ProofOfWorkMapping.from(value.proofOfWork),
                value.avatarVersion,
                NetworkIdMapping.from(value.networkId),
                value.terms,
                value.statement,
                value.applicationVersion,
                value.nym,
                value.userName,
                value.publishDate
            )
        }
    }


    object TransportTypeMapping {
        fun toPojo(vo: TransportTypeEnum): TransportType {
            return if (vo == TransportTypeEnum.CLEAR) {
                TransportType.CLEAR
            } else if (vo == TransportTypeEnum.TOR) {
                TransportType.TOR
            } else if (vo == TransportTypeEnum.I2P) {
                TransportType.I2P
            } else {
                throw IllegalArgumentException("Unsupported enum $vo")
            }
        }

        fun from(value: TransportType): TransportTypeEnum {
            return if (value == TransportType.CLEAR) {
                TransportTypeEnum.CLEAR
            } else if (value == TransportType.TOR) {
                TransportTypeEnum.TOR
            } else if (value == TransportType.I2P) {
                TransportTypeEnum.I2P
            } else {
                throw IllegalArgumentException("Unsupported enum $value")
            }
        }
    }

    object AddressMapping {
        fun toPojo(vo: AddressVO): Address {
            return Address(vo.host, vo.port)
        }

        fun from(value: Address): AddressVO {
            return AddressVO(value.host, value.port)
        }
    }

    object AddressByTransportTypeMapMapping {
        fun toPojo(vo: AddressByTransportTypeMapVO): AddressByTransportTypeMap {
            return AddressByTransportTypeMap(vo.map.entries.associate {
                TransportTypeMapping.toPojo(it.key) to AddressMapping.toPojo(it.value)
            })
        }

        fun from(map: AddressByTransportTypeMap): AddressByTransportTypeMapVO {
            return AddressByTransportTypeMapVO(map.map.entries.associate {
                TransportTypeMapping.from(it.key) to AddressMapping.from(it.value)
            })
        }
    }


    object PriceQuoteMapping {
        fun toPojo(vo: PriceQuoteVO): PriceQuote {
            val baseCurrencyCode = vo.market.baseCurrencyCode
            val quoteCurrencyCode = vo.market.quoteCurrencyCode
            if (baseCurrencyCode == "BTC") {
                val baseSideMonetary: Monetary = Coin.asBtcFromFaceValue(1.0);
                val quoteSideMonetary: Monetary = Fiat.from(vo.value, quoteCurrencyCode);
                return PriceQuote(vo.value, baseSideMonetary, quoteSideMonetary);
            } else {
                throw UnsupportedOperationException("Altcoin price quote mapping is not supported yet");
            }
        }

        fun from(value: PriceQuote): PriceQuoteVO {
            return PriceQuoteVO(
                value.value,
                MarketMapping.from(value.market)
            )
        }
    }

    object MonetaryMapping {
        fun toPojo(vo: MonetaryVO): Monetary {
            return if (vo is FiatVO) {
                FiatMapping.toPojo(vo)
            } else {
                CoinMapping.toPojo(vo as CoinVO)
            }
        }

        fun from(value: Monetary): MonetaryVO {
            return if (value is Fiat) {
                FiatVO.from(
                    value.getId(),
                    value.getValue(),
                    value.getCode(),
                    value.getPrecision(),
                    value.getLowPrecision(),
                )
            } else {
                CoinVO.from(value.id, value.value, value.code, value.precision, value.lowPrecision)
            }
        }
    }

    object FiatMapping {
        fun toPojo(vo: FiatVO): Fiat {
            return Fiat(vo.id, vo.value, vo.code, vo.precision, vo.lowPrecision)
        }

        fun from(value: Fiat): FiatVO {
            return FiatVO.from(value.id, value.value, value.code, value.precision, value.lowPrecision)
        }
    }

    object CoinMapping {
        fun toPojo(vo: CoinVO): Coin {
            return Coin(vo.id, vo.value, vo.code, vo.precision, vo.lowPrecision)
        }

        fun from(value: Coin): CoinVO {
            return CoinVO.from(value.id, value.value, value.code, value.precision, value.lowPrecision)
        }
    }

    object MarketMapping {
        fun toPojo(vo: MarketVO): Market {
            return Market(vo.baseCurrencyCode, vo.quoteCurrencyCode, vo.baseCurrencyName, vo.quoteCurrencyName)
        }

        fun from(value: Market): MarketVO {
            return MarketVO(value.baseCurrencyCode, value.quoteCurrencyCode, value.baseCurrencyName, value.quoteCurrencyName)
        }
    }


    object TradeProtocolTypeMapping {
        fun toPojo(vo: TradeProtocolTypeEnum): TradeProtocolType {
            return when (vo) {
                TradeProtocolTypeEnum.BISQ_EASY -> TradeProtocolType.BISQ_EASY
                TradeProtocolTypeEnum.BISQ_MU_SIG -> TradeProtocolType.BISQ_MU_SIG
                TradeProtocolTypeEnum.SUBMARINE -> TradeProtocolType.SUBMARINE
                TradeProtocolTypeEnum.LIQUID_MU_SIG -> TradeProtocolType.LIQUID_MU_SIG
                TradeProtocolTypeEnum.BISQ_LIGHTNING -> TradeProtocolType.BISQ_LIGHTNING
                TradeProtocolTypeEnum.LIQUID_SWAP -> TradeProtocolType.LIQUID_SWAP
                TradeProtocolTypeEnum.BSQ_SWAP -> TradeProtocolType.BSQ_SWAP
                TradeProtocolTypeEnum.LIGHTNING_ESCROW -> TradeProtocolType.LIGHTNING_ESCROW
                TradeProtocolTypeEnum.MONERO_SWAP -> TradeProtocolType.MONERO_SWAP
                else -> throw IllegalArgumentException("Unsupported enum $vo")
            }
        }

        fun from(value: TradeProtocolType): TradeProtocolTypeEnum {
            return when (value) {
                TradeProtocolType.BISQ_EASY -> TradeProtocolTypeEnum.BISQ_EASY
                TradeProtocolType.BISQ_MU_SIG -> TradeProtocolTypeEnum.BISQ_MU_SIG
                TradeProtocolType.SUBMARINE -> TradeProtocolTypeEnum.SUBMARINE
                TradeProtocolType.LIQUID_MU_SIG -> TradeProtocolTypeEnum.LIQUID_MU_SIG
                TradeProtocolType.BISQ_LIGHTNING -> TradeProtocolTypeEnum.BISQ_LIGHTNING
                TradeProtocolType.LIQUID_SWAP -> TradeProtocolTypeEnum.LIQUID_SWAP
                TradeProtocolType.BSQ_SWAP -> TradeProtocolTypeEnum.BSQ_SWAP
                TradeProtocolType.LIGHTNING_ESCROW -> TradeProtocolTypeEnum.LIGHTNING_ESCROW
                TradeProtocolType.MONERO_SWAP -> TradeProtocolTypeEnum.MONERO_SWAP
                else -> throw IllegalArgumentException("Unsupported enum $value")
            }
        }
    }

    object ReputationScoreMapping {
        fun toPojo(vo: ReputationScoreVO): ReputationScore {
            return ReputationScore(vo.totalScore, vo.fiveSystemScore, vo.ranking)
        }

        fun from(value: ReputationScore): ReputationScoreVO {
            return ReputationScoreVO(value.totalScore, value.fiveSystemScore, value.ranking)
        }
    }

    object NetworkIdMapping {
        fun toPojo(vo: NetworkIdVO): NetworkId {
            return NetworkId(
                AddressByTransportTypeMapMapping.toPojo(vo.addressByTransportTypeMap),
                PubKeyMapping.toPojo(vo.pubKey)
            )
        }

        fun from(value: NetworkId): NetworkIdVO {
            return NetworkIdVO(
                AddressByTransportTypeMapMapping.from(value.addressByTransportTypeMap),
                PubKeyMapping.from(value.pubKey)
            )
        }
    }

    object DirectionMapping {
        fun toPojo(vo: DirectionEnum): Direction {
            return if (vo == DirectionEnum.BUY) {
                Direction.BUY
            } else {
                Direction.SELL
            }
        }

        fun from(value: Direction): DirectionEnum {
            return if (value == Direction.BUY) {
                DirectionEnum.BUY
            } else {
                DirectionEnum.SELL
            }
        }
    }

    object TradeTermsOptionMapping {
        fun toPojo(vo: TradeTermsOptionVO): TradeTermsOption {
            return TradeTermsOption(vo.makersTradeTerms)
        }

        fun from(value: TradeTermsOption): TradeTermsOptionVO {
            return TradeTermsOptionVO.from(value.makersTradeTerms)
        }
    }

    object ReputationOptionMapping {
        fun toPojo(vo: ReputationOptionVO): ReputationOption {
            return ReputationOption(vo.requiredTotalReputationScore)
        }

        fun from(value: ReputationOption): ReputationOptionVO {
            @Suppress("DEPRECATION")
            return ReputationOptionVO.from(value.requiredTotalReputationScore)
        }
    }

    object OfferOptionMapping {
        fun toPojo(vo: OfferOptionVO): OfferOption {
            return if (vo is ReputationOptionVO) {
                ReputationOptionMapping.toPojo(vo)
            } else if (vo is TradeTermsOptionVO) {
                TradeTermsOptionMapping.toPojo(vo)
            } else {
                throw IllegalArgumentException("Unsupported OfferOptionVO $vo")
            }
        }

        fun from(value: OfferOption): OfferOptionVO {
            return if (value is ReputationOption) {
                @Suppress("DEPRECATION")
                ReputationOptionVO.from(value.requiredTotalReputationScore)
            } else if (value is TradeTermsOption) {
                TradeTermsOptionVO.from(value.makersTradeTerms)
            } else {
                throw IllegalArgumentException("Unsupported OfferOption $value")
            }
        }
    }

    object MarketPriceSpecMapping {
        fun toPojo(vo: MarketPriceSpecVO): MarketPriceSpec {
            return MarketPriceSpec()
        }

        fun from(value: MarketPriceSpec): MarketPriceSpecVO {
            return MarketPriceSpecVO.from()
        }
    }

    object FloatPriceSpecMapping {
        fun toPojo(vo: FloatPriceSpecVO): FloatPriceSpec {
            return FloatPriceSpec(vo.percentage)
        }

        fun from(value: FloatPriceSpec): FloatPriceSpecVO {
            return FloatPriceSpecVO.from(value.percentage)
        }
    }

    object FixPriceSpecMapping {
        fun toPojo(vo: FixPriceSpecVO): FixPriceSpec {
            return FixPriceSpec(PriceQuoteMapping.toPojo(vo.priceQuote))
        }

        fun from(value: FixPriceSpec): FixPriceSpecVO {
            return FixPriceSpecVO.from(PriceQuoteMapping.from(value.priceQuote))
        }
    }

    object PriceSpecMapping {
        fun toPojo(vo: PriceSpecVO): PriceSpec {
            return when (vo) {
                is MarketPriceSpecVO -> MarketPriceSpecMapping.toPojo(vo)
                is FixPriceSpecVO -> FixPriceSpecMapping.toPojo(vo)
                is FloatPriceSpecVO -> FloatPriceSpecMapping.toPojo(vo)
                else -> throw IllegalArgumentException("Unsupported PriceSpecVO $vo")
            }
        }

        fun from(value: PriceSpec): PriceSpecVO {
            return when (value) {
                is MarketPriceSpec -> MarketPriceSpecMapping.from(value)
                is FixPriceSpec -> FixPriceSpecMapping.from(value)
                is FloatPriceSpec -> FloatPriceSpecMapping.from(value)
                else -> throw IllegalArgumentException("Unsupported PriceSpec $value")
            }
        }
    }

    object BaseSideRangeAmountSpecMapping {
        fun toPojo(vo: BaseSideRangeAmountSpecVO): BaseSideRangeAmountSpec {
            return BaseSideRangeAmountSpec(vo.minAmount, vo.maxAmount)
        }

        fun from(value: BaseSideRangeAmountSpec): BaseSideRangeAmountSpecVO {
            return BaseSideRangeAmountSpecVO.from(value.minAmount, value.maxAmount)
        }
    }

    object QuoteSideRangeAmountSpecMapping {
        fun toPojo(vo: QuoteSideRangeAmountSpecVO): QuoteSideRangeAmountSpec {
            return QuoteSideRangeAmountSpec(vo.minAmount, vo.maxAmount)
        }

        fun from(value: QuoteSideRangeAmountSpec): QuoteSideRangeAmountSpecVO {
            return QuoteSideRangeAmountSpecVO.from(value.minAmount, value.maxAmount)
        }
    }


    object RangeAmountSpecMapping {
        fun toPojo(vo: RangeAmountSpecVO): RangeAmountSpec {
            return if (vo is BaseSideRangeAmountSpecVO) {
                BaseSideRangeAmountSpecMapping.toPojo(vo)
            } else if (vo is QuoteSideRangeAmountSpecVO) {
                QuoteSideRangeAmountSpecMapping.toPojo(vo)
            } else {
                throw IllegalArgumentException("Unsupported RangeAmountSpecVO $vo")
            }
        }

        fun from(value: RangeAmountSpec): RangeAmountSpecVO {
            return if (value is BaseSideRangeAmountSpec) {
                BaseSideRangeAmountSpecMapping.from(value)
            } else if (value is QuoteSideRangeAmountSpec) {
                QuoteSideRangeAmountSpecMapping.from(value)
            } else {
                throw IllegalArgumentException("Unsupported RangeAmountSpec $value")
            }
        }
    }

    object BaseSideFixedAmountSpecMapping {
        fun toPojo(vo: BaseSideFixedAmountSpecVO): BaseSideFixedAmountSpec {
            return BaseSideFixedAmountSpec(vo.amount)
        }

        fun from(value: BaseSideFixedAmountSpec): BaseSideFixedAmountSpecVO {
            return BaseSideFixedAmountSpecVO.from(value.amount)
        }
    }

    object QuoteSideFixedAmountSpecMapping {
        fun toPojo(vo: QuoteSideFixedAmountSpecVO): QuoteSideFixedAmountSpec {
            return QuoteSideFixedAmountSpec(vo.amount)
        }

        fun from(value: QuoteSideFixedAmountSpec): QuoteSideFixedAmountSpecVO {
            return QuoteSideFixedAmountSpecVO.from(value.amount)
        }
    }

    object FixedAmountSpecMapping {
        fun toPojo(vo: FixedAmountSpecVO): FixedAmountSpec {
            return if (vo is BaseSideFixedAmountSpecVO) {
                BaseSideFixedAmountSpecMapping.toPojo(vo)
            } else if (vo is QuoteSideFixedAmountSpecVO) {
                QuoteSideFixedAmountSpecMapping.toPojo(vo)
            } else {
                throw IllegalArgumentException("Unsupported FixedAmountSpecVO $vo")
            }
        }

        fun from(value: FixedAmountSpec): FixedAmountSpecVO {
            return if (value is BaseSideFixedAmountSpec) {
                BaseSideFixedAmountSpecMapping.from(value)
            } else if (value is QuoteSideFixedAmountSpec) {
                QuoteSideFixedAmountSpecMapping.from(value)
            } else {
                throw IllegalArgumentException("Unsupported FixedAmountSpec $value")
            }
        }
    }

    object AmountSpecMapping {
        fun toPojo(vo: AmountSpecVO): AmountSpec {
            return if (vo is RangeAmountSpecVO) {
                RangeAmountSpecMapping.toPojo(vo)
            } else {
                FixedAmountSpecMapping.toPojo(vo as FixedAmountSpecVO)
            }
        }

        fun from(value: AmountSpec): AmountSpecVO {
            return if (value is RangeAmountSpec) {
                RangeAmountSpecMapping.from(value)
            } else {
                FixedAmountSpecMapping.from(value as FixedAmountSpec)
            }
        }
    }

    object BitcoinPaymentMethodSpecMapping {
        fun toPojo(vo: BitcoinPaymentMethodSpecVO): BitcoinPaymentMethodSpec {
            val paymentMethod = vo.paymentMethod
            val method = PaymentMethodSpecUtil.getBitcoinPaymentMethod(paymentMethod)
            return BitcoinPaymentMethodSpec(method, Optional.ofNullable(vo.saltedMakerAccountId))
        }

        fun from(value: BitcoinPaymentMethodSpec): BitcoinPaymentMethodSpecVO {
            return BitcoinPaymentMethodSpecVO.from(value.paymentMethod.name, value.saltedMakerAccountId.orElse(null))
        }
    }

    object FiatPaymentMethodSpecMapping {
        fun toPojo(vo: FiatPaymentMethodSpecVO): FiatPaymentMethodSpec {
            val paymentMethod = vo.paymentMethod
            val method = PaymentMethodSpecUtil.getFiatPaymentMethod(paymentMethod)
            return FiatPaymentMethodSpec(method, Optional.ofNullable(vo.saltedMakerAccountId))
        }

        fun from(value: FiatPaymentMethodSpec): FiatPaymentMethodSpecVO {
            return FiatPaymentMethodSpecVO.from(value.paymentMethod.name, value.saltedMakerAccountId.orElse(null))
        }
    }

    object PaymentMethodSpecMapping {
        fun toPojo(vo: PaymentMethodSpecVO): PaymentMethodSpec<*> {
            return if (vo is FiatPaymentMethodSpecVO) {
                FiatPaymentMethodSpecMapping.toPojo(vo)
            } else if (vo is BitcoinPaymentMethodSpecVO) {
                BitcoinPaymentMethodSpecMapping.toPojo(vo)
            } else {
                throw IllegalArgumentException("Unsupported PaymentMethodSpecVO $vo")
            }
        }

        fun from(value: PaymentMethodSpec<*>): PaymentMethodSpecVO {
            return if (value is FiatPaymentMethodSpec) {
                FiatPaymentMethodSpecMapping.from(value)
            } else if (value is BitcoinPaymentMethodSpec) {
                BitcoinPaymentMethodSpecMapping.from(value)
            } else {
                throw IllegalArgumentException("Unsupported PaymentMethodSpec $value")
            }
        }
    }

    object BisqEasyOfferMapping {
        fun toPojo(vo: BisqEasyOfferVO): BisqEasyOffer {
            return BisqEasyOffer(
                vo.id,
                vo.date,
                NetworkIdMapping.toPojo(vo.makerNetworkId),
                DirectionMapping.toPojo(vo.direction),
                MarketMapping.toPojo(vo.market),
                AmountSpecMapping.toPojo(vo.amountSpec),
                PriceSpecMapping.toPojo(vo.priceSpec),
                vo.protocolTypes.map { TradeProtocolTypeMapping.toPojo(it) },
                vo.baseSidePaymentMethodSpecs.map { BitcoinPaymentMethodSpecMapping.toPojo(it) },
                vo.quoteSidePaymentMethodSpecs.map { FiatPaymentMethodSpecMapping.toPojo(it) },
                vo.offerOptions.map { OfferOptionMapping.toPojo(it) },
                vo.supportedLanguageCodes
            )
        }

        fun from(value: BisqEasyOffer): BisqEasyOfferVO {
            return BisqEasyOfferVO(
                value.id,
                value.date,
                NetworkIdMapping.from(value.makerNetworkId),
                DirectionMapping.from(value.direction),
                MarketMapping.from(value.market),
                AmountSpecMapping.from(value.amountSpec),
                PriceSpecMapping.from(value.priceSpec),
                value.protocolTypes.map { TradeProtocolTypeMapping.from(it) },
                value.baseSidePaymentMethodSpecs.map { BitcoinPaymentMethodSpecMapping.from(it) },
                value.quoteSidePaymentMethodSpecs.map { FiatPaymentMethodSpecMapping.from(it) },
                value.offerOptions.map { OfferOptionMapping.from(it) },
                value.supportedLanguageCodes
            )
        }
    }
}