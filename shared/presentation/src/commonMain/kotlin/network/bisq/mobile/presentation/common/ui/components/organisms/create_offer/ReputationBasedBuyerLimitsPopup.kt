package network.bisq.mobile.presentation.common.ui.components.organisms.create_offer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.NoteText
import network.bisq.mobile.presentation.common.ui.components.atoms.button.GreyCloseButton
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.BisqDialog
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.BisqLinks

@Composable
fun ReputationBasedBuyerLimitsPopup(
    onDismiss: () -> Unit,
    amountLimitInfoOverlayInfo: String,
) {
    BisqDialog(
        horizontalAlignment = Alignment.Start,
        padding = BisqUIConstants.ScreenPadding,
        onDismissRequest = onDismiss,
    ) {
        BisqText.H6Light("bisqEasy.tradeWizard.amount.limitInfo.overlay.headline".i18n())

        BisqGap.V1()

        BisqText.BaseLight(amountLimitInfoOverlayInfo)

        BisqGap.V1()

        NoteText(
            "bisqEasy.tradeWizard.amount.buyer.limitInfo.overlay.linkToWikiText".i18n(),
            linkText = BisqLinks.REPUTATION_WIKI_URL,
            uri = BisqLinks.REPUTATION_WIKI_URL,
            openConfirmation = true,
        )

        BisqGap.V3()

        GreyCloseButton(onClick = onDismiss)
    }
}

@Preview
@Composable
private fun ReputationBasedBuyerLimitsPopupPreview() {
    BisqTheme.Preview {
        Box(Modifier.background(BisqTheme.colors.backgroundColor).padding(16.dp)) {
            ReputationBasedBuyerLimitsPopup(
                onDismiss = {},
                amountLimitInfoOverlayInfo = "bisqEasy.tradeWizard.amount.buyer.limitInfo.overlay.info.firstPart".i18n(),
            )
        }
    }
}
