package network.bisq.mobile.presentation.peer_profile

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import network.bisq.mobile.presentation.common.ui.components.LoadingState
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.common.ui.i18n.i18nText
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycleBackStackAware

/**
 * The full "Trade again" offer list for one peer — the overflow screen behind the capped section
 * on [PeerProfileScreen]. Reuses [PeerProfilePresenter] (its own back-stack entry gets its own
 * instance, which loads the peer's offers independently — nothing is serialized through nav args)
 * and the section's row/header composables, so this list and the profile preview cannot drift.
 */
@ExcludeFromCoverage
@Composable
fun PeerOffersScreen(profileId: String) {
    val presenter = RememberPresenterLifecycleBackStackAware<PeerProfilePresenter>()
    val uiState by presenter.uiState.collectAsState()

    LaunchedEffect(presenter, profileId) {
        presenter.initialize(profileId)
    }

    PeerOffersScreenContent(
        uiState = uiState,
        onAction = presenter::onAction,
        topBar = {
            TopBar(
                title = peerOffersTitle(uiState),
                showUserAvatar = false,
            )
        },
    )
}

/** Same header copy as the profile section: the screen is the section, expanded. */
@Composable
internal fun peerOffersTitle(uiState: PeerProfileUiState): String {
    val titleKey =
        if (uiState.hasTradedBefore) {
            "mobile.peerProfile.offers.sectionTitle"
        } else {
            "mobile.peerProfile.offers.sectionTitleContactOnly"
        }
    return i18nText(titleKey, uiState.displayName)
}

@Composable
internal fun PeerOffersScreenContent(
    uiState: PeerProfileUiState,
    onAction: (PeerProfileUiAction) -> Unit,
    topBar: @Composable () -> Unit = {},
) {
    BisqScaffold(topBar = topBar) { paddingValues ->
        if (uiState.isLoading) {
            LoadingState(paddingValues)
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = BisqUIConstants.ScreenPadding2X),
            ) {
                item { BisqGap.V1() }
                if (uiState.peerOffers.isEmpty()) {
                    item {
                        if (uiState.isPeerOffersSyncing) PeerOfferSyncingRow() else PeerOfferEmptyRow()
                    }
                } else {
                    uiState.peerOffers.forEach { group ->
                        item(key = "header-${group.marketCodes}") {
                            PeerOfferGroupHeader(marketCodes = group.marketCodes)
                            BisqGap.VHalf()
                        }
                        items(group.offers, key = { it.offerId }) { offer ->
                            PeerOfferRow(
                                item = offer,
                                onClick = { onAction(PeerProfileUiAction.OnPeerOfferClick(offer.offerId)) },
                            )
                            BisqGap.VHalf()
                        }
                        item { BisqGap.VHalf() }
                    }
                }
                item { BisqGap.V1() }
            }
        }

        NotEnoughReputationDialogs(
            notEnoughReputation = uiState.notEnoughReputation,
            onAction = onAction,
        )
    }
}
