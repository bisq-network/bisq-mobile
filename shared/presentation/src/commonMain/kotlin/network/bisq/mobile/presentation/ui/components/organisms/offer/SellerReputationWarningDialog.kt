package network.bisq.mobile.presentation.ui.components.organisms.offer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.button.LinkButton
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.molecules.BisqDialog
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

// TODO: Reduce the amount of text shown here
@Composable
fun SellerReputationWarningDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onLearnReputation: () -> Unit,
) {

    BisqDialog(
        horizontalAlignment = Alignment.Start,
        marginTop = BisqUIConstants.ScreenPadding,
        padding = BisqUIConstants.ScreenPadding,
    ) {

        BisqText.h6Medium(
            text = "bisqEasy.tradeWizard.directionAndMarket.feedback.headline".i18n(),
            color = BisqTheme.colors.warning
        )

        BisqGap.V1()

        BisqText.baseRegular("bisqEasy.tradeWizard.directionAndMarket.feedback.subTitle1".i18n())

        BisqGap.VHalf()

        LinkButton(
            text = "bisqEasy.tradeWizard.directionAndMarket.feedback.gainReputation".i18n(),
            link = "https://bisq.wiki/Reputation#How_to_build_reputation",
            type = BisqButtonType.Outline,
            onClick = onLearnReputation,
            padding = PaddingValues(horizontal = BisqUIConstants.ScreenPadding, vertical = 8.dp),
            fullWidth = true
        )

        BisqGap.VHalf()

        BisqText.baseRegular("bisqEasy.tradeWizard.directionAndMarket.feedback.subTitle2".i18n())

        BisqGap.V1()

        BisqButton(
            text = "action.back".i18n(),
            type = BisqButtonType.Grey,
            onClick = onDismiss,
            padding = PaddingValues(horizontal = BisqUIConstants.ScreenPadding, vertical = 8.dp),
            fullWidth = true
        )

        BisqGap.VHalf()

        BisqButton(
            text = "bisqEasy.tradeWizard.directionAndMarket.feedback.tradeWithoutReputation".i18n(),
            onClick = onConfirm,
            padding = PaddingValues(horizontal = BisqUIConstants.ScreenPadding, vertical = 8.dp),
            fullWidth = true
        )
    }
}