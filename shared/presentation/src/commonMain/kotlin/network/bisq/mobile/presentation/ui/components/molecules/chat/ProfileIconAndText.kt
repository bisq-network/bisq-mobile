package network.bisq.mobile.presentation.ui.components.molecules.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.img_bot_image
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.icons.rememberPlatformImagePainter
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.jetbrains.compose.resources.painterResource

@Composable
fun ProfileIconAndText(
    message: BisqEasyOpenTradeMessageModel,
    userAvatar: PlatformImage? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(
            vertical = BisqUIConstants.ScreenPaddingHalf, horizontal = BisqUIConstants.ScreenPadding
        ), horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding)
    ) {
        val painter: Painter = if (userAvatar == null) {
            painterResource(Res.drawable.img_bot_image)
        } else {
            rememberPlatformImagePainter(userAvatar)
        }

        val icon = @Composable {
            Image(
                painter = painter, "", modifier = Modifier.size(30.dp) // same size as top bar
            )
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