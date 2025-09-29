package network.bisq.mobile.presentation.ui.components.molecules.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.molecules.UserProfileIcon
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun ProfileIconAndText(
    message: BisqEasyOpenTradeMessageModel,
    userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(
            vertical = BisqUIConstants.ScreenPaddingHalf, horizontal = BisqUIConstants.ScreenPadding
        ), horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding)
    ) {

        val icon = @Composable {
            UserProfileIcon(message.senderUserProfile, userProfileIconProvider, 30.dp)
        }

        val text = @Composable {
            BisqText.baseRegular(message.textString)
        }

        if (message.isMyMessage) {
            text()
            icon()
        } else {
            icon()
            text()
        }
    }
}