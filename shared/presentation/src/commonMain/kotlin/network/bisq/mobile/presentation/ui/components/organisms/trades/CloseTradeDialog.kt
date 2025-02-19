package network.bisq.mobile.presentation.ui.components.organisms.trades

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.molecules.BisqDialog
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun CloseTradeDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) {
    val strings = LocalStrings.current.bisqEasyTradeState
    val stringsBisqEasy = LocalStrings.current.bisqEasy
    val stringsCommon = LocalStrings.current.common

    BisqDialog {

        BisqText.h4Regular(
            text = strings.bisqEasy_tradeState_phase4,
            modifier = Modifier.padding(bottom= BisqUIConstants.ScreenPadding)
        )

        BisqText.baseRegularGrey(
            text = stringsBisqEasy.bisqEasy_openTrades_closeTrade_warning_completed,
            textAlign = TextAlign.Center
        )

        Row {
            BisqButton(
                text = stringsCommon.buttons_cancel,
                type = BisqButtonType.Grey,
                onClick = onDismissRequest,
                padding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
            )
            BisqGap.H1()
            BisqButton(
                text = stringsBisqEasy.bisqEasy_openTrades_confirmCloseTrade,
                onClick = onConfirm,
                padding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}