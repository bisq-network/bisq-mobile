package network.bisq.mobile.presentation.offers_below_reputation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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

/**
 * Shown above every tab while some of my sell offers exceed my reputation; tapping it opens the
 * review dialog. While the offers are being removed it says so and cannot be dismissed.
 */
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
            text =
                if (uiState.isRemoving) {
                    "mobile.bisqEasy.offerbook.offersBelowReputation.banner.removing".i18n()
                } else {
                    "bisqEasy.offerbook.offerList.popup.offersWithInsufficientReputationWarning.headline".i18n()
                },
            color = BisqTheme.colors.warning,
            modifier = Modifier.weight(1f),
        )
        if (uiState.isRemoving) {
            // Takes the close icon's place and size, so the row does not jump.
            Box(modifier = Modifier.size(ICON_BUTTON_SIZE), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = BisqTheme.colors.warning,
                    strokeWidth = 2.dp,
                )
            }
        } else {
            val dismissLabel = "mobile.alert.actions.dismiss.label".i18n()
            IconButton(
                onClick = { onAction(OffersBelowReputationUiAction.DismissBanner) },
                modifier = Modifier.semantics { contentDescription = dismissLabel },
            ) {
                CloseIcon()
            }
        }
    }
}

// Material3's IconButton default.
private val ICON_BUTTON_SIZE = 40.dp

@ExcludeFromCoverage
@Preview
@Composable
private fun OffersBelowReputationBannerPreview() {
    BisqTheme.Preview {
        val uiState =
            OffersBelowReputationUiState(
                offendingOffers = listOf(OffendingOffer(offerId = "a", market = MarketVOFactory.USD, formattedAmount = "50 USD")),
                isBannerVisible = true,
            )
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            OffersBelowReputationBanner(uiState = uiState, onAction = {})
            BisqGap.V1()
            OffersBelowReputationBanner(uiState = uiState.copy(isRemoving = true), onAction = {})
        }
    }
}
