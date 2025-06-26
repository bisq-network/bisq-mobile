package network.bisq.mobile.presentation.ui.components.organisms.create_offer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.BisqLinks
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.NoteText
import network.bisq.mobile.presentation.ui.components.atoms.button.GreyCloseButton
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.molecules.dialog.BisqDialog
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun ReputationBasedBuyerLimitsPopup(
    onDismiss: () -> Unit,
    onRepLinkClick: () -> Unit,
    amountLimitInfoOverlayInfo: StateFlow<String>
) {
    val amountLimitInfo by amountLimitInfoOverlayInfo.collectAsState()

    BisqDialog(
        horizontalAlignment = Alignment.Start,
        padding = BisqUIConstants.ScreenPadding,
        onDismissRequest = onDismiss,
    ) {

        BisqText.h6Medium("bisqEasy.tradeWizard.amount.limitInfo.overlay.headline".i18n())

        BisqGap.V1()

        BisqText.baseRegular(amountLimitInfo)

        NoteText(
            "bisqEasy.tradeWizard.amount.buyer.limitInfo.overlay.linkToWikiText".i18n(),
            linkText = BisqLinks.REPUTATION_BUILD_WIKI_URL,
            openConfirmation = true,
            onLinkClick = onRepLinkClick
        )

        BisqGap.V3()

        GreyCloseButton(onClick = onDismiss)
    }
}