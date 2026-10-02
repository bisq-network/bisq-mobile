package network.bisq.mobile.presentation.offers_below_reputation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.data.replicated.common.currency.MarketVOFactory
import network.bisq.mobile.domain.service.offers.OffendingOffer
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.CloseIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.WarningIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

/** Shown above every tab while some of my sell offers exceed my reputation; tapping it opens the review dialog. */
@Composable
fun OffersBelowReputationBanner(
    uiState: OffersBelowReputationUiState,
    onAction: (OffersBelowReputationUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!uiState.isBannerVisible || uiState.offendingOffers.isEmpty()) return

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .background(BisqTheme.colors.warning.copy(alpha = 0.12f))
                .clickable { onAction(OffersBelowReputationUiAction.OpenDialog) }
                .padding(start = BisqUIConstants.ScreenPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WarningIcon()
        BisqGap.HHalf()
        BisqText.SmallMedium(
            text = "bisqEasy.offerbook.offerList.popup.offersWithInsufficientReputationWarning.headline".i18n(),
            color = BisqTheme.colors.warning,
            modifier = Modifier.weight(1f),
        )
        val dismissLabel = "mobile.alert.actions.dismiss.label".i18n()
        IconButton(
            onClick = { onAction(OffersBelowReputationUiAction.DismissBanner) },
            modifier = Modifier.semantics { contentDescription = dismissLabel },
        ) {
            CloseIcon()
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun OffersBelowReputationBannerPreview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            OffersBelowReputationBanner(
                uiState =
                    OffersBelowReputationUiState(
                        offendingOffers = listOf(OffendingOffer(offerId = "a", market = MarketVOFactory.USD, formattedAmount = "50 USD")),
                        isBannerVisible = true,
                    ),
                onAction = {},
            )
        }
    }
}
