package network.bisq.mobile.presentation.ui.components.molecules.chat.private_messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

//todo this is a statically added msg in a similar style as protocol log messages
@Composable
fun ChatRulesWarningMessageBox(message: BisqEasyOpenTradeMessageModel) {
    Row(
        modifier = Modifier
            .padding(horizontal = BisqUIConstants.ScreenPadding3X)
            .background(BisqTheme.colors.secondaryDisabled)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center

    ) {
        Column(
            modifier = Modifier.padding(all = BisqUIConstants.ScreenPadding2X),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding)
        ) {
            BisqText.baseRegular(message.textString)
            BisqText.smallRegular(message.dateString)
        }
    }
}