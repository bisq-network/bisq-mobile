package network.bisq.mobile.presentation.ui.components.molecules.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun UsernameAndDate(message: BisqEasyOpenTradeMessageModel) {
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalfQuarter),
    ) {
        val username = @Composable {
            BisqText.baseRegular(text = message.senderUserName)
        }

        val date = @Composable {
            BisqText.xsmallLightGrey(message.dateString, modifier = Modifier.offset(y = (-1).dp))
        }

        if (message.isMyMessage) {
            date()
            username()
        } else {
            username()
            date()
        }
    }
}