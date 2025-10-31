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
package network.bisq.mobile.android.node.mapping

import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel
import bisq.contract.bisq_easy.BisqEasyContract
import bisq.presentation.formatters.DateFormatter
import bisq.trade.bisq_easy.BisqEasyTrade
import bisq.trade.bisq_easy.BisqEasyTradeFormatter
import bisq.trade.bisq_easy.BisqEasyTradeUtils
import bisq.user.profile.UserProfileService
import bisq.user.reputation.ReputationService
import network.bisq.mobile.android.node.mapping.Mappings.ReputationScoreMapping
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationDto
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO


object TradeItemPresentationDtoFactory {
    fun create(
        trade: BisqEasyTrade,
        channel: BisqEasyOpenTradeChannel,
        userProfileService: UserProfileService,
        reputationService: ReputationService
    ): TradeItemPresentationDto {

        val myUserProfile = userProfileService.getManagedUserProfile(channel.myUserIdentity.userProfile)
        val myUserProfileVO = Mappings.UserProfileMapping.fromBisq2Model(myUserProfile)
        val peersUserProfile = userProfileService.getManagedUserProfile(channel.peer)
        val peersUserProfileVO = Mappings.UserProfileMapping.fromBisq2Model(peersUserProfile)
        val contract: BisqEasyContract = trade.contract
        val date = contract.takeOfferDate
        val directionalTitle: String = BisqEasyTradeFormatter.getDirectionalTitle(trade)
        val formattedDate: String = DateFormatter.formatDate(date)
        val formattedTime: String = DateFormatter.formatTime(date)
        val market: String = trade.offer.market.toString()
        val price: Long = BisqEasyTradeUtils.getPriceQuote(trade).value
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


        val makerUserProfile: UserProfileVO
        val takerUserProfile: UserProfileVO
        if (trade.isMaker) {
            makerUserProfile = myUserProfileVO
            takerUserProfile = peersUserProfileVO
        } else {
            makerUserProfile = peersUserProfileVO
            takerUserProfile = myUserProfileVO
        }

        val mediatorUserProfile: UserProfileVO? = if (contractVO.mediator != null) contractVO.mediator!! else null
        val peersReputationScore = reputationService.getReputationScore(peersUserProfile.id)
        val peersRReputationScoreVO = ReputationScoreMapping.fromBisq2Model(peersReputationScore)

        return TradeItemPresentationDto(
            channelVO,
            tradeVO,
            makerUserProfile,
            takerUserProfile,
            mediatorUserProfile,
            directionalTitle,
            formattedDate,
            formattedTime,
            market,
            price,
            priceString,
            baseAmount,
            baseAmountString,
            quoteAmount,
            quoteAmountString,
            bitcoinSettlementMethod,
            bitcoinSettlementMethodDisplayString,
            fiatPaymentMethod,
            fiatPaymentMethodDisplayString,
            isFiatPaymentMethodCustom,
            myRole,
            peersRReputationScoreVO
        )
    }
}