package network.bisq.mobile.presentation.offers_below_reputation

import network.bisq.mobile.domain.service.offers.OffendingOffer

data class OffersBelowReputationUiState(
    val offendingOffers: List<OffendingOffer> = emptyList(),
    val isBannerVisible: Boolean = false,
    val isDialogVisible: Boolean = false,
    val isRemoving: Boolean = false,
)

sealed interface OffersBelowReputationUiAction {
    data object OpenDialog : OffersBelowReputationUiAction

    data object DismissBanner : OffersBelowReputationUiAction

    data object RemoveOffers : OffersBelowReputationUiAction

    data object Keep : OffersBelowReputationUiAction

    data object BuildReputation : OffersBelowReputationUiAction

    data class GoToMarket(
        val offer: OffendingOffer,
    ) : OffersBelowReputationUiAction
}
