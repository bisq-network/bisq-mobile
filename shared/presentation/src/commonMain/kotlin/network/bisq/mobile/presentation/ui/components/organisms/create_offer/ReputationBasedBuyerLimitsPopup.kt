package network.bisq.mobile.presentation.ui.components.organisms.create_offer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.BisqLinks
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.NoteText
import network.bisq.mobile.presentation.ui.components.atoms.button.GreyCloseButton
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.molecules.dialog.BisqDialog
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.jetbrains.compose.ui.tooling.preview.Preview

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

        BisqText.h6Light("bisqEasy.tradeWizard.amount.limitInfo.overlay.headline".i18n())

        BisqGap.V1()

        BisqText.baseLight(amountLimitInfo)

        BisqGap.V1()

        NoteText(
            "bisqEasy.tradeWizard.amount.buyer.limitInfo.overlay.linkToWikiText".i18n(),
            linkText = BisqLinks.REPUTATION_BASE_WIKI_URL,
            openConfirmation = true,
            onLinkClick = onRepLinkClick
        )

        BisqGap.V3()

        GreyCloseButton(onClick = onDismiss)
    }
}

@Preview
@Composable
fun ReputationBasedBuyerLimitsPopupPreview() {
    BisqTheme.Preview {
        Box(Modifier.background(BisqTheme.colors.backgroundColor).padding(16.dp)) {
            ReputationBasedBuyerLimitsPopup(
                onDismiss = {},
                onRepLinkClick = {},
                amountLimitInfoOverlayInfo = MutableStateFlow("bisqEasy.tradeWizard.amount.buyer.limitInfo.overlay.info.firstPart".i18n())
            )
        }
    }
}