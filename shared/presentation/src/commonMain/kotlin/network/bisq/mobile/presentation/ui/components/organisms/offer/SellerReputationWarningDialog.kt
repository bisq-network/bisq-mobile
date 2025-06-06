package network.bisq.mobile.presentation.ui.components.organisms.offer

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.button.LinkButton
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.molecules.dialog.BisqDialog
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

// TODO: Reduce the amount of text shown here
@Composable
fun SellerReputationWarningDialog(
    onDismiss: () -> Unit,
    onLearnReputation: () -> Unit,
) {
    BisqDialog(
        horizontalAlignment = Alignment.Start,
        marginTop = BisqUIConstants.ScreenPadding,
        padding = BisqUIConstants.ScreenPadding,
        onDismissRequest = onDismiss,
    ) {
        BisqText.h6Light(
            text = "bisqEasy.tradeWizard.directionAndMarket.feedback.headline".i18n(),
            color = BisqTheme.colors.warning
        )

        BisqGap.V1()

        BisqText.baseRegular(
            "bisqEasy.tradeWizard.directionAndMarket.feedback.subTitle1".i18n() + "\n\n" +
                    "bisqEasy.tradeWizard.directionAndMarket.feedback.subTitle2".i18n() + "\n\n" +
                    "bisqEasy.tradeWizard.directionAndMarket.feedback.subTitle3".i18n()
        )

        BisqGap.V2()

        BisqButton(
            text = "action.back".i18n(),
            type = BisqButtonType.Grey,
            onClick = onDismiss,
            padding = PaddingValues(horizontal = BisqUIConstants.ScreenPadding, vertical = 8.dp),
            fullWidth = true
        )

        BisqGap.VHalf()

        LinkButton(
            text = "bisqEasy.tradeWizard.directionAndMarket.feedback.gainReputation".i18n(),
            link = "https://bisq.wiki/Reputation#How_to_build_reputation",
            type = BisqButtonType.Default,
            color = BisqTheme.colors.white,
            onClick = onLearnReputation,
            padding = PaddingValues(horizontal = BisqUIConstants.ScreenPadding, vertical = 8.dp),
            fullWidth = true
        )
    }
}