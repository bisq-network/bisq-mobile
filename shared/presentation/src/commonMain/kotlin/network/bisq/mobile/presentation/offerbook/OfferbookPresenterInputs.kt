package network.bisq.mobile.presentation.offerbook

import network.bisq.mobile.data.model.offerbook.OfferbookMarket
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO

// Strongly-typed input bundle for OfferbookPresenter's combine/map pipeline
data class OfferbookPresenterInputs(
    val offers: List<OfferItemPresentationModel>,
    val direction: DirectionEnum,
    val selectedMarket: OfferbookMarket,
    val selectedProfile: UserProfileVO?,
    val payments: Set<String>,
    val settlements: Set<String>,
    val onlyMine: Boolean,
)
