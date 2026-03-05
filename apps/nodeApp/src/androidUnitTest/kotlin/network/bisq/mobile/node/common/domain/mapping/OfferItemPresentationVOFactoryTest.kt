package network.bisq.mobile.node.common.domain.mapping

import bisq.bonded_roles.market_price.MarketPriceService
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage
import bisq.common.market.Market
import bisq.offer.Direction
import bisq.offer.amount.spec.QuoteSideFixedAmountSpec
import bisq.offer.bisq_easy.BisqEasyOffer
import bisq.offer.price.spec.MarketPriceSpec
import bisq.user.identity.UserIdentityService
import bisq.user.profile.UserProfileService
import bisq.user.reputation.ReputationService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertNull

class OfferItemPresentationVOFactoryTest {
    private val userProfileService = mockk<UserProfileService>()
    private val userIdentityService = mockk<UserIdentityService>(relaxed = true)
    private val marketPriceService = mockk<MarketPriceService>(relaxed = true)
    private val reputationService = mockk<ReputationService>(relaxed = true)

    @Test
    fun `create returns null when author profile not found`() {
        val market = Market("BTC", "USD", "Bitcoin", "US Dollar")
        val offer = mockk<BisqEasyOffer>(relaxed = true)
        every { offer.direction } returns Direction.BUY
        every { offer.market } returns market
        every { offer.amountSpec } returns QuoteSideFixedAmountSpec(100_00L)
        every { offer.priceSpec } returns MarketPriceSpec()
        every { offer.baseSidePaymentMethodSpecs } returns emptyList()
        every { offer.quoteSidePaymentMethodSpecs } returns emptyList()

        val message = mockk<BisqEasyOfferbookMessage>(relaxed = true)
        every { message.bisqEasyOffer } returns Optional.of(offer)
        every { message.authorUserProfileId } returns "missing-profile-id"
        every { userProfileService.findUserProfile("missing-profile-id") } returns Optional.empty()

        val result =
            OfferItemPresentationVOFactory.create(
                userProfileService,
                userIdentityService,
                marketPriceService,
                reputationService,
                message,
            )

        assertNull(result, "Factory should return null when author profile is not yet synced")
        verify(exactly = 1) { userProfileService.findUserProfile("missing-profile-id") }
    }
}
