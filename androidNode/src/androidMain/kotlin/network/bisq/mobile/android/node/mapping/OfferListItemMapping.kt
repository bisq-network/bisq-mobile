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

import bisq.account.payment_method.BitcoinPaymentMethod
import bisq.account.payment_method.FiatPaymentMethod
import bisq.bonded_roles.market_price.MarketPriceService
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookMessage
import bisq.i18n.Res
import bisq.offer.amount.OfferAmountFormatter
import bisq.offer.amount.spec.RangeAmountSpec
import bisq.offer.payment_method.PaymentMethodSpecUtil
import bisq.offer.price.PriceUtil
import bisq.offer.price.spec.PriceSpecFormatter
import bisq.presentation.formatters.DateFormatter
import bisq.presentation.formatters.PriceFormatter
import bisq.user.identity.UserIdentityService
import bisq.user.profile.UserProfile
import bisq.user.profile.UserProfileService
import bisq.user.reputation.ReputationScore
import bisq.user.reputation.ReputationService
import network.bisq.mobile.android.node.mapping.Mappings.BisqEasyOfferMapping
import network.bisq.mobile.android.node.mapping.Mappings.ReputationScoreMapping
import network.bisq.mobile.domain.replicated.offer.bisq_easy.OfferListItemVO
import java.text.DateFormat
import java.util.Date
import java.util.stream.Collectors


object OfferListItemMapping {
    fun createOfferListItemVO(
        userProfileService: UserProfileService,
        userIdentityService: UserIdentityService?,
        reputationService: ReputationService,
        marketPriceService: MarketPriceService?,
        bisqEasyOfferbookMessage: BisqEasyOfferbookMessage
    ): OfferListItemVO {
        val bisqEasyOffer = bisqEasyOfferbookMessage.bisqEasyOffer.get()
        val bisqEasyOfferVO = BisqEasyOfferMapping.from(bisqEasyOffer)
        val isMyOffer = bisqEasyOfferbookMessage.isMyMessage(userIdentityService)
        val authorUserProfileId = bisqEasyOfferbookMessage.authorUserProfileId
        val senderUserProfile = userProfileService.findUserProfile(authorUserProfileId)
        val nym = senderUserProfile.map { obj: UserProfile -> obj.nym }.orElse("")
        val userName = senderUserProfile.map { obj: UserProfile -> obj.userName }.orElse("")
        val reputationScore = senderUserProfile
            .flatMap { userProfile: UserProfile? -> reputationService.findReputationScore(userProfile) }
            .map { value: ReputationScore -> ReputationScoreMapping.from(value) }
            .orElse(ReputationScoreMapping.from(ReputationScore.NONE))

        // For now, we get also the formatted values as we have not the complex formatters in mobile impl. yet.
        // We might need to replicate the formatters anyway later and then those fields could be removed
        val date = bisqEasyOfferbookMessage.date
        val formattedDate = DateFormatter.formatDateTime(
            Date(date), DateFormat.MEDIUM, DateFormat.SHORT,
            true, " " + Res.get("temporal.at") + " "
        )
        val amountSpec = bisqEasyOffer.amountSpec
        val priceSpec = bisqEasyOffer.priceSpec
        val hasAmountRange = amountSpec is RangeAmountSpec
        val market = bisqEasyOffer.market
        val formattedQuoteAmount = OfferAmountFormatter.formatQuoteAmount(
            marketPriceService,
            amountSpec,
            priceSpec,
            market,
            hasAmountRange,
            true
        )
        val formattedBaseAmount = OfferAmountFormatter.formatBaseAmount(
            marketPriceService,
            amountSpec,
            priceSpec,
            market,
            hasAmountRange,
            true,
            false
        )
        val formattedPrice = PriceUtil.findQuote(marketPriceService, bisqEasyOffer)
            .map { PriceFormatter.format(it) }
            .orElse("")
        val formattedPriceSpec = PriceSpecFormatter.getFormattedPriceSpec(priceSpec, true)
        val quoteSidePaymentMethods: List<String> = PaymentMethodSpecUtil.getPaymentMethods(bisqEasyOffer.quoteSidePaymentMethodSpecs)
            .stream()
            .map { obj: FiatPaymentMethod -> obj.name }
            .collect(Collectors.toList())
        val baseSidePaymentMethods = PaymentMethodSpecUtil.getPaymentMethods(bisqEasyOffer.baseSidePaymentMethodSpecs)
            .stream()
            .map { obj: BitcoinPaymentMethod -> obj.name }
            .collect(Collectors.toList())
        return OfferListItemVO(
            bisqEasyOfferVO,
            isMyOffer,
            nym,
            userName,
            reputationScore,
            formattedDate,
            formattedQuoteAmount,
            formattedBaseAmount,
            formattedPrice,
            formattedPriceSpec,
            quoteSidePaymentMethods,
            baseSidePaymentMethods
        )
    }
}