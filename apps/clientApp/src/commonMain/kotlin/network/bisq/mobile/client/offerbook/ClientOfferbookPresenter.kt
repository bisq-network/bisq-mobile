package network.bisq.mobile.client.offerbook

import network.bisq.mobile.client.common.domain.service.user_profile.ClientUserProfileServiceFacade
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.data.service.alert.TradeRestrictingAlertServiceFacade
import network.bisq.mobile.data.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.data.service.offers.OffersServiceFacade
import network.bisq.mobile.data.service.reputation.ReputationServiceFacade
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.offer.create_offer.CreateOfferCoordinator
import network.bisq.mobile.presentation.offer.take_offer.TakeOfferCoordinator
import network.bisq.mobile.presentation.offerbook.OfferbookPresenter

class ClientOfferbookPresenter(
    mainPresenter: MainPresenter,
    offersServiceFacade: OffersServiceFacade,
    takeOfferCoordinator: TakeOfferCoordinator,
    createOfferCoordinator: CreateOfferCoordinator,
    marketPriceServiceFacade: MarketPriceServiceFacade,
    reputationServiceFacade: ReputationServiceFacade,
    private val userProfileServiceFacade: ClientUserProfileServiceFacade,
    tradeRestrictingAlertServiceFacade: TradeRestrictingAlertServiceFacade,
) : OfferbookPresenter(
        mainPresenter,
        offersServiceFacade,
        takeOfferCoordinator,
        createOfferCoordinator,
        marketPriceServiceFacade,
        userProfileServiceFacade,
        reputationServiceFacade,
        tradeRestrictingAlertServiceFacade,
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
