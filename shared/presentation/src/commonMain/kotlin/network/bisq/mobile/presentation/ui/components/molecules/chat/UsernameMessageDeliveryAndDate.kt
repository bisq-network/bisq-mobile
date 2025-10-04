package network.bisq.mobile.presentation.ui.components.molecules.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel
import network.bisq.mobile.domain.data.replicated.network.confidential.ack.MessageDeliveryInfoVO
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun UsernameMessageDeliveryAndDate(
    message: BisqEasyOpenTradeMessageModel,
    onResendMessage: (String) -> Unit,
    userNameProvider: suspend (String) -> String,
    messageDeliveryInfoByPeersProfileId: StateFlow<Map<String, MessageDeliveryInfoVO>>,
) {
    var showInfo by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .semantics(mergeDescendants = true) {}
            .clickable(message.isMyMessage) { showInfo = true },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalfQuarter),
    ) {
        val username = @Composable {
            BisqText.baseRegular(message.senderUserName, modifier = Modifier.offset(y = (-1).dp))
        }

        val date = @Composable {
            BisqText.xsmallLightGrey(message.dateString)
        }

        if (message.isMyMessage) {
            date()
            MessageDeliveryBox(
                onResendMessage = onResendMessage,
                userNameProvider = userNameProvider,
                messageDeliveryInfoByPeersProfileId = messageDeliveryInfoByPeersProfileId,
                showInfo,
                onDismissMenu = {
                    showInfo = false
                }
            )
            username()
        } else {
            username()
            date()
        }
    }
}