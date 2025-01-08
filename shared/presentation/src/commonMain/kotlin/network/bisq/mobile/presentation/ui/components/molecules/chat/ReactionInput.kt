package network.bisq.mobile.presentation.ui.components.molecules.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.components.atoms.DynamicImage
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun ChatReactionInput(
    onReact: (ReactionType) -> Unit
) {
    val reactionTypes = listOf(
        ReactionType.THUMBS_UP,
        ReactionType.THUMBS_DOWN,
        ReactionType.HEART,
        ReactionType.LAUGH,
        ReactionType.HAPPY,
        ReactionType.PARTY
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        modifier = Modifier.padding(horizontal = BisqUIConstants.ScreenPadding)
    ) {

        reactionTypes.forEach { reactionType ->
            DynamicImage(
                path = reactionType.fileName,
                modifier = Modifier.size(16.dp).clickable(
                    onClick = { onReact(reactionType) },
                    interactionSource = MutableInteractionSource(),
                    indication = null
                )
            )
        }
    }
}
