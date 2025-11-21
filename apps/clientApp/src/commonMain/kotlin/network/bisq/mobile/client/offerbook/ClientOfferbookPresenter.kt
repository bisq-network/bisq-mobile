package network.bisq.mobile.client.offerbook

import network.bisq.mobile.client.common.domain.service.user_profile.ClientUserProfileServiceFacade
import network.bisq.mobile.domain.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.reputation.ReputationServiceFacade
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferPresenter
import network.bisq.mobile.presentation.ui.uicases.offerbook.OfferbookPresenter
import network.bisq.mobile.presentation.ui.uicases.take_offer.TakeOfferPresenter

class ClientOfferbookPresenter(
    mainPresenter: MainPresenter,
    offersServiceFacade: OffersServiceFacade,
    takeOfferPresenter: TakeOfferPresenter,
    createOfferPresenter: CreateOfferPresenter,
    marketPriceServiceFacade: MarketPriceServiceFacade,
    reputationServiceFacade: ReputationServiceFacade,
    private val userProfileServiceFacade: ClientUserProfileServiceFacade,
) : OfferbookPresenter(
    mainPresenter,
    offersServiceFacade,
    takeOfferPresenter,
    createOfferPresenter,
    marketPriceServiceFacade,
    userProfileServiceFacade,
    reputationServiceFacade
) {
    override fun isOfferFromIgnoredUserCached(offer: BisqEasyOfferVO): Boolean {
        val makerUserProfileId = offer.makerNetworkId.pubKey.id
        return try {
            // Use cached check for hot path performance
            val isIgnored = userProfileServiceFacade.isUserIgnoredCached(makerUserProfileId)
            if (isIgnored) {
                log.v { "Offer ${offer.id} from ignored user $makerUserProfileId (cached)" }
            }
            isIgnored
        } catch (e: Exception) {
            log.w("isUserIgnoredCached failed for $makerUserProfileId", e)
            false
        }
    }
}