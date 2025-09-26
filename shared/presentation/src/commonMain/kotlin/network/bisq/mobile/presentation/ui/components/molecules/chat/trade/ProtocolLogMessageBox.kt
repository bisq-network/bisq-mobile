package network.bisq.mobile.presentation.ui.components.molecules.chat.trade

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun ProtocolLogMessageBox(message: BisqEasyOpenTradeMessageModel, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(BisqTheme.colors.dark_grey30)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center

    ) {
        Column(
            modifier = Modifier.padding(all = BisqUIConstants.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf)
        ) {
            BisqText.smallLight(message.decodedText, textAlign = TextAlign.Center)
            BisqText.xsmallLightGrey(message.dateString)
        }
    }
}