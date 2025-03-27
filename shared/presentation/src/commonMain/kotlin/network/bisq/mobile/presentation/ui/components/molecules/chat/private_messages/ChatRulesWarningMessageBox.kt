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
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.trade_chat.TradeChatPresenter

// todo we could use also a banner style instead of the style similar to ProtocolLogMessageBox
@Composable
fun ChatRulesWarningMessageBox(presenter: TradeChatPresenter) {
    Row(
        modifier = Modifier
            .background(BisqTheme.colors.dark3)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center

    ) {
        Column(
            modifier = Modifier.padding(all = BisqUIConstants.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf)) {
                // WarningIcon() // TODO Use a grey warning here as its less severe, or just drop the icon
                BisqText.h6Regular(
                    "chat.private.chatRulesWarningMessage.headline".i18n(),
                    color = BisqTheme.colors.grey2
                )
            }
            BisqText.baseLightGrey("chat.private.chatRulesWarningMessage.text".i18n())

            BisqButton(
                type = BisqButtonType.Grey,
                text = "action.dontShowAgain".i18n(),
                onClick = { presenter.onDontShowAgainChatRulesWarningBox() }
            )

            // We dont need  the learn more button for mobile IMO
        }
    }
}