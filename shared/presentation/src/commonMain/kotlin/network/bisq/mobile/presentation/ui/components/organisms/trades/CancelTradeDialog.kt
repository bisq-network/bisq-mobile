package network.bisq.mobile.presentation.ui.components.organisms.trades

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
// import network.bisq.mobile.presentation.ui.components.atoms.icons.WarningIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.molecules.BisqDialog
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun CancelTradeDialog(
    onCancelConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current.bisqEasy
    val stringsApplication = LocalStrings.current.application
    val stringsCommon = LocalStrings.current.common

    val isBuyer: Boolean = true

    // TODO: Use this if trade steps is not started yet: bisqEasy_openTrades_rejectTrade_warning
    val warningText1 = if (isBuyer)
        strings.bisqEasy_openTrades_cancelTrade_warning_buyer
    else
        strings.bisqEasy_openTrades_cancelTrade_warning_seller

    val warningText2 = strings.bisqEasy_openTrades_cancelTrade_warning_part2

    BisqDialog(horizontalAlignment = Alignment.Start) {
        Row(
            modifier = Modifier.padding(bottom = BisqUIConstants.ScreenPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // WarningIcon()
            BisqGap.H1()
            BisqText.h6Medium(
                text = stringsApplication.popup_headline_warning,
                color = BisqTheme.colors.warning
            )
        }

        BisqText.baseRegular(text = warningText1 + warningText2)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            BisqButton(
                text = stringsCommon.common_no,
                onClick = onCancelConfirm,
                padding = PaddingValues(horizontal = BisqUIConstants.ScreenPadding, vertical = 8.dp),
                backgroundColor = BisqTheme.colors.dark5
            )
            BisqGap.H1()
            BisqButton(
                text = stringsCommon.common_yes,
                onClick = onDismiss,
                padding = PaddingValues(horizontal = BisqUIConstants.ScreenPadding, vertical = 8.dp)
            )
        }
    }
}