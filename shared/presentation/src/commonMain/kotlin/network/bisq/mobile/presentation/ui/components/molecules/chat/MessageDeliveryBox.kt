package network.bisq.mobile.presentation.ui.components.molecules.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.data.replicated.network.confidential.ack.MessageDeliveryInfoVO
import network.bisq.mobile.domain.data.replicated.network.confidential.ack.MessageDeliveryStatusEnum
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.button.BisqIconButton
import network.bisq.mobile.presentation.ui.components.atoms.icons.DeliveryStatusConnecting
import network.bisq.mobile.presentation.ui.components.atoms.icons.DeliveryStatusMailbox
import network.bisq.mobile.presentation.ui.components.atoms.icons.DeliveryStatusReceived
import network.bisq.mobile.presentation.ui.components.atoms.icons.DeliveryStatusSent
import network.bisq.mobile.presentation.ui.components.atoms.icons.DeliveryStatusTrySendingAgain
import network.bisq.mobile.presentation.ui.components.atoms.icons.DeliveryStatusUndelivered
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun MessageDeliveryBox(
    onResendMessage: (String) -> Unit,
    userNameProvider: suspend (String) -> String,
    messageDeliveryInfoByPeersProfileId: StateFlow<Map<String, MessageDeliveryInfoVO>>,
    showInfo: Boolean,
    onDismissMenu: () -> Unit,
) {
    val map by messageDeliveryInfoByPeersProfileId.collectAsState()

    if (map.isEmpty()) {
        return
    }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.End,
    ) {
        DropdownMenu(
            expanded = showInfo,
            onDismissRequest = onDismissMenu,
            containerColor = BisqTheme.colors.dark_grey50,
            offset = DpOffset(
                x = 0.dp,
                y = BisqUIConstants.ScreenPaddingQuarter
            ),
        ) {
            MessageDeliveryInfo(map, userNameProvider)
        }
        map.values.forEach { messageDeliveryInfo ->
            val messageDeliveryStatus = messageDeliveryInfo.messageDeliveryStatus

            Row(
                modifier = Modifier
                    .semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalfQuarter),
            ) {
                if (messageDeliveryInfo.canManuallyResendMessage) {
                    BisqIconButton(
                        onClick = { onResendMessage.invoke(messageDeliveryInfo.ackRequestingMessageId) },
                        size = 12.dp,
                    ) {
                        DeliveryStatusTrySendingAgain(
                            colorFilter = ColorFilter.tint(BisqTheme.colors.yellow)
                        )
                    }
                }

                val grey: ColorFilter = ColorFilter.tint(BisqTheme.colors.mid_grey20)
                val iconModifier = Modifier.size(13.dp)
                when (messageDeliveryStatus) {
                    MessageDeliveryStatusEnum.CONNECTING -> DeliveryStatusConnecting(modifier = iconModifier, colorFilter = grey)
                    MessageDeliveryStatusEnum.SENT, MessageDeliveryStatusEnum.TRY_ADD_TO_MAILBOX -> DeliveryStatusSent(
                        modifier = iconModifier,
                        colorFilter = grey
                    )

                    MessageDeliveryStatusEnum.ACK_RECEIVED, MessageDeliveryStatusEnum.MAILBOX_MSG_RECEIVED -> DeliveryStatusReceived(
                        modifier = iconModifier,
                        colorFilter = grey
                    )

                    MessageDeliveryStatusEnum.ADDED_TO_MAILBOX -> DeliveryStatusMailbox(modifier = iconModifier, colorFilter = grey)
                    MessageDeliveryStatusEnum.FAILED -> DeliveryStatusUndelivered(
                        modifier = iconModifier,
                        colorFilter = ColorFilter.tint(BisqTheme.colors.yellow)
                    )
                }
            }
        }
    }
}

@Composable
fun MessageDeliveryInfo(
    map: Map<String, MessageDeliveryInfoVO>,
    userNameProvider: suspend (String) -> String,
) {
    val multiplePeers = map.size > 1

    val usernames by produceState(initialValue = emptyMap(), map) {
        value = map.keys.associateWith { id ->
            if (multiplePeers) {
                userNameProvider.invoke(id)
            } else {
                "" // Ignore as we only show user name if multiplePeers are used
            }
        }
    }

    val infoText = map.entries.joinToString("\n\n") { (peerProfileId, messageDeliveryInfo) ->
        val messageDeliveryStatus = messageDeliveryInfo.messageDeliveryStatus
        val deliveryState = "chat.message.deliveryState.${messageDeliveryStatus.name}".i18n()
        if (multiplePeers) {
            val userName = usernames[peerProfileId] ?: "data.na".i18n()
            "chat.message.deliveryState.multiplePeers".i18n(userName, deliveryState)
        } else {
            deliveryState
        }
    }

    BisqText.xsmallLight(
        text = infoText,
        color = BisqTheme.colors.white,
        modifier = Modifier.padding(
            vertical = BisqUIConstants.ScreenPaddingQuarter,
            horizontal = BisqUIConstants.ScreenPadding
        )
    )
}