package network.bisq.mobile.presentation.ui.components.organisms.offer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.LocalStrings
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
    val strings = LocalStrings.current.bisqEasyTradeWizard
    val stringsCommon = LocalStrings.current.common

    BisqDialog(
        horizontalAlignment = Alignment.Start,
        marginTop = BisqUIConstants.ScreenPadding,
        padding = BisqUIConstants.ScreenPadding,
    ) {

        BisqText.h6Medium(
            text = strings.bisqEasy_tradeWizard_directionAndMarket_feedback_headline,
            color = BisqTheme.colors.warning
        )

        BisqGap.V1()

        BisqText.baseRegular(text = strings.bisqEasy_tradeWizard_directionAndMarket_feedback_subTitle1)

        BisqGap.VHalf()

        LinkButton(
            text =strings.bisqEasy_tradeWizard_directionAndMarket_feedback_gainReputation,
            link = "https://bisq.wiki/Reputation#How_to_build_reputation",
            type = BisqButtonType.Outline,
            onClick = onLearnReputation,
            padding = PaddingValues(horizontal = BisqUIConstants.ScreenPadding, vertical = 8.dp),
            fullWidth = true
        )

        BisqGap.VHalf()

        BisqText.baseRegular(text = strings.bisqEasy_tradeWizard_directionAndMarket_feedback_subTitle2)

        BisqGap.V1()

        BisqButton(
            text = strings.bisqEasy_tradeWizard_directionAndMarket_feedback_backToBuy,
            type = BisqButtonType.Grey,
            onClick = onDismiss,
            padding = PaddingValues(horizontal = BisqUIConstants.ScreenPadding, vertical = 8.dp),
            fullWidth = true
        )

        BisqGap.VHalf()

        BisqButton(
            text = strings.bisqEasy_tradeWizard_directionAndMarket_feedback_tradeWithoutReputation,
            onClick = onConfirm,
            padding = PaddingValues(horizontal = BisqUIConstants.ScreenPadding, vertical = 8.dp),
            fullWidth = true
        )
    }
}