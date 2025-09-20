package network.bisq.mobile.presentation.ui.components.organisms.create_offer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.BisqLinks
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.NoteText
import network.bisq.mobile.presentation.ui.components.atoms.button.GreyCloseButton
import network.bisq.mobile.presentation.ui.components.atoms.button.LinkButton
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.molecules.dialog.BisqDialog
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ReputationBasedSellerLimitsPopup(
    onDismiss: () -> Unit,
    onBuildRepLinkClick: () -> Unit,
    onRepLinkClick: () -> Unit,
    reputationScore: String,
    maxSellAmount: String
) {

    BisqDialog(
        horizontalAlignment = Alignment.Start,
        padding = BisqUIConstants.ScreenPadding,
        onDismissRequest = onDismiss,
    ) {

        BisqText.h6Light("bisqEasy.tradeWizard.amount.limitInfo.overlay.headline".i18n())

        BisqGap.V1()

        BisqText.baseLight("bisqEasy.tradeWizard.amount.seller.limitInfo.overlay".i18n(reputationScore, maxSellAmount))

        BisqGap.V3()

        LinkButton(
            text = "bisqEasy.tradeWizard.amount.limitInfo.overlay.learnHowToBuildReputation".i18n(),
            link = BisqLinks.BUILD_REPUTATION_WIKI_URL,
            type = BisqButtonType.Outline,
            padding = PaddingValues(horizontal = BisqUIConstants.ScreenPadding, vertical = 8.dp),
            fullWidth = true,
            onClick = onBuildRepLinkClick
        )

        BisqGap.V1()

        NoteText(
            "bisqEasy.tradeWizard.amount.buyer.limitInfo.overlay.linkToWikiText".i18n(),
            linkText = BisqLinks.REPUTATION_WIKI_URL,
            openConfirmation = true,
            onLinkClick = onRepLinkClick
        )

        BisqGap.V3()

        GreyCloseButton(onClick = onDismiss)
    }
}

@Preview
@Composable
fun ReputationBasedSellerLimitsPopupPreview() {
    BisqTheme.Preview {
        Box(Modifier.background(BisqTheme.colors.backgroundColor).padding(16.dp)) {
            ReputationBasedSellerLimitsPopup(
                onDismiss = {},
                onBuildRepLinkClick = {},
                onRepLinkClick = {},
                reputationScore = "100",
                maxSellAmount = "1000"
            )
        }
    }
}