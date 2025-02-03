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

    BisqDialog(horizontalAlignment = Alignment.Start) {

        BisqText.h6Medium(
            text = strings.bisqEasy_tradeWizard_directionAndMarket_feedback_headline,
            color = BisqTheme.colors.warning
        )

        BisqText.baseRegular(text = strings.bisqEasy_tradeWizard_directionAndMarket_feedback_subTitle1)

        BisqButton(
            text = strings.bisqEasy_tradeWizard_directionAndMarket_feedback_gainReputation,
            onClick = onLearnReputation,
            padding = PaddingValues(horizontal = BisqUIConstants.ScreenPadding, vertical = 8.dp),
            color = BisqTheme.colors.primary,
            type = BisqButtonType.Outline
        )

        BisqText.baseRegular(text = strings.bisqEasy_tradeWizard_directionAndMarket_feedback_subTitle2)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            BisqButton(
                text = strings.bisqEasy_tradeWizard_directionAndMarket_feedback_backToBuy,
                onClick = onDismiss,
                padding = PaddingValues(horizontal = BisqUIConstants.ScreenPadding, vertical = 8.dp),
                backgroundColor = BisqTheme.colors.dark5
            )
            BisqGap.H1()
            BisqButton(
                text = strings.bisqEasy_tradeWizard_directionAndMarket_feedback_tradeWithoutReputation,
                onClick = onConfirm,
                padding = PaddingValues(horizontal = BisqUIConstants.ScreenPadding, vertical = 8.dp)
            )
        }
    }
}