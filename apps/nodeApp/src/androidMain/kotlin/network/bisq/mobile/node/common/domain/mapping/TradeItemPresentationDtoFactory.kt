/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */
package network.bisq.mobile.node.common.domain.mapping

import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel
import bisq.contract.bisq_easy.BisqEasyContract
import bisq.presentation.formatters.DateFormatter
import bisq.trade.bisq_easy.BisqEasyTrade
import bisq.trade.bisq_easy.BisqEasyTradeFormatter
import bisq.user.profile.UserProfileService
import bisq.user.reputation.ReputationService
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationDto
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO

object TradeItemPresentationDtoFactory {
    fun create(
        trade: BisqEasyTrade,
        channel: BisqEasyOpenTradeChannel,
        userProfileService: UserProfileService,
        reputationService: ReputationService,
    ): TradeItemPresentationDto {
        val myUserProfile = userProfileService.getManagedUserProfile(channel.myUserIdentity.userProfile)
        val peersUserProfile = userProfileService.getManagedUserProfile(channel.peer)
        val contract: BisqEasyContract = trade.contract
        val date = contract.takeOfferDate
        val directionalTitle: String = BisqEasyTradeFormatter.getDirectionalTitle(trade)
        val formattedDate: String = DateFormatter.formatDate(date)
        val formattedTime: String = DateFormatter.formatTime(date)
        val market: String = trade.offer.market.toString()
        val price: Long = contract.marketPrice
        val priceString: String = BisqEasyTradeFormatter.formatPriceWithCode(trade)
        val baseAmount: Long = contract.baseSideAmount
        val baseAmountString: String = BisqEasyTradeFormatter.formatBaseSideAmount(trade)
        val quoteAmount: Long = contract.quoteSideAmount
        val quoteAmountString: String = BisqEasyTradeFormatter.formatQuoteSideAmount(trade)
        val bitcoinSettlementMethod: String = contract.baseSidePaymentMethodSpec.paymentMethodName
        val bitcoinSettlementMethodDisplayString: String = contract.baseSidePaymentMethodSpec.shortDisplayString
        val fiatPaymentMethod: String = contract.quoteSidePaymentMethodSpec.paymentMethodName
        val fiatPaymentMethodDisplayString: String = contract.quoteSidePaymentMethodSpec.shortDisplayString
        val isFiatPaymentMethodCustom: Boolean = contract.quoteSidePaymentMethodSpec.paymentMethod.isCustomPaymentMethod
        val myRole: String = BisqEasyTradeFormatter.getMakerTakerRole(trade)

        val channelVO = Mappings.BisqEasyOpenTradeChannelVOMapping.fromBisq2Model(channel)
        val tradeVO = Mappings.BisqEasyTradeVOMapping.fromBisq2Model(trade)
        val contractVO = Mappings.BisqEasyContractMapping.fromBisq2Model(trade.contract)
        val myUserProfileVO = Mappings.UserProfileMapping.fromBisq2Model(myUserProfile)
        val peersUserProfileVO = Mappings.UserProfileMapping.fromBisq2Model(peersUserProfile)

        val makerUserProfile: UserProfileVO
        val takerUserProfile: UserProfileVO
        if (trade.isMaker) {
            makerUserProfile = myUserProfileVO
            takerUserProfile = peersUserProfileVO
        } else {
            makerUserProfile = peersUserProfileVO
            takerUserProfile = myUserProfileVO
        }

        val mediatorUserProfile: UserProfileVO? = contractVO.mediator
        val peersReputationScore = reputationService.getReputationScore(peersUserProfile.id)
        val peersRReputationScoreVO = Mappings.ReputationScoreMapping.fromBisq2Model(peersReputationScore)

        return TradeItemPresentationDto(
            channel = channelVO,
            trade = tradeVO,
            makerUserProfile = makerUserProfile,
            takerUserProfile = takerUserProfile,
            mediatorUserProfile = mediatorUserProfile,
            directionalTitle = directionalTitle,
            formattedDate = formattedDate,
            formattedTime = formattedTime,
            market = market,
            price = price,
            formattedPrice = priceString,
            baseAmount = baseAmount,
            formattedBaseAmount = baseAmountString,
            quoteAmount = quoteAmount,
            formattedQuoteAmount = quoteAmountString,
            bitcoinSettlementMethod = bitcoinSettlementMethod,
            bitcoinSettlementMethodDisplayString = bitcoinSettlementMethodDisplayString,
            fiatPaymentMethod = fiatPaymentMethod,
            fiatPaymentMethodDisplayString = fiatPaymentMethodDisplayString,
            isFiatPaymentMethodCustom = isFiatPaymentMethodCustom,
            formattedMyRole = myRole,
            peersReputationScore = peersRReputationScoreVO,
        )
    }
}
