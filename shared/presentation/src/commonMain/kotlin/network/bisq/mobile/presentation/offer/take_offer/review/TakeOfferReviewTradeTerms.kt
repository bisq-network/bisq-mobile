package network.bisq.mobile.presentation.offer.take_offer.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.common.ui.components.molecules.trade_terms.TradeTermsAcceptanceNotice
import network.bisq.mobile.presentation.common.ui.components.molecules.trade_terms.TradeTermsBlock
import network.bisq.mobile.presentation.common.ui.components.molecules.trade_terms.TradeTermsUiState
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants

/**
 * The maker's trade terms as the last section of the review: conditions on the deal, not a number,
 * so they get their own section after the divider. The acceptance notice is the last thing read
 * before the "Take offer" button. Composes nothing, divider included, when the offer has no terms.
 */
@Composable
fun TakeOfferReviewTradeTerms(
    tradeTerms: TradeTermsUiState?,
    onAction: (TakeOfferReviewUiAction) -> Unit,
) {
    if (tradeTerms == null) return

    BisqHDivider()
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
    ) {
        TradeTermsBlock(
            state = tradeTerms,
            title = "bisqEasy.offerDetails.makersTradeTerms".i18n(),
            onToggleExpand = { onAction(TakeOfferReviewUiAction.OnToggleTradeTerms) },
        )
        TradeTermsAcceptanceNotice()
    }
}
