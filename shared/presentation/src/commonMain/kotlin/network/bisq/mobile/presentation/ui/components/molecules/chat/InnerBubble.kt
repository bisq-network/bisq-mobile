package network.bisq.mobile.presentation.ui.components.molecules.chat

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.DynamicImage
import network.bisq.mobile.presentation.ui.composeModels.ChatMessage
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun ChatInnerBubble(
    message: ChatMessage,
    isUserMe: Boolean,
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.padding(
            vertical = BisqUIConstants.ScreenPaddingHalf,
            horizontal = BisqUIConstants.ScreenPadding
        ),
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding)
    ) {
        if (!isUserMe) {
            DynamicImage(
                "drawable/img_bot_image.png",
                modifier = Modifier.size(24.dp)
            )
        }
        BisqText.baseRegular(text = message.content)
    }
}