package network.bisq.mobile.presentation.ui.components.organisms.create_offer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.i18n.i18nPlural
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
    reputationScore: String,
    maxBuyAmount: String,
    takersCount: Int,
) {

    BisqDialog(
        horizontalAlignment = Alignment.Start,
        padding = BisqUIConstants.ScreenPadding,
    ) {

        BisqText.h6Medium("bisqEasy.tradeWizard.amount.limitInfo.overlay.headline".i18n())

        BisqGap.V1()

        val description = if (takersCount == 0) {
            "bisqEasy.tradeWizard.amount.buyer.limitInfo.overlay.info.noSellers".i18n(maxBuyAmount, reputationScore)
        } else {
            val sellers = "bisqEasy.tradeWizard.amount.buyer.numSellers".i18nPlural(takersCount)
            "bisqEasy.tradeWizard.amount.buyer.limitInfo.overlay.info.wSellers".i18n(maxBuyAmount, reputationScore, sellers)
        }
        BisqText.baseRegular(description)

        BisqGap.V1()

        NoteText(
            "bisqEasy.tradeWizard.amount.buyer.limitInfo.overlay.linkToWikiText".i18n(),
            linkText = "https://bisq.wiki/Reputation",
            openConfirmation = true,
            onLinkClick = onRepLinkClick
        )

        BisqGap.V1()

        GreyCloseButton(onClick = onDismiss)

    }
}