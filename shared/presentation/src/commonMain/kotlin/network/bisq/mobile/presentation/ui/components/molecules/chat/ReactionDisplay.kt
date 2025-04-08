package network.bisq.mobile.presentation.ui.components.molecules.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel
import network.bisq.mobile.domain.data.replicated.chat.reactions.BisqEasyOpenTradeMessageReactionVO
import network.bisq.mobile.domain.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.presentation.ui.components.atoms.DynamicImage

@Composable
fun ReactionDisplay(
    message: BisqEasyOpenTradeMessageModel,
    onAddReaction: (ReactionEnum) -> Unit,
    onRemoveReaction: (BisqEasyOpenTradeMessageReactionVO) -> Unit,
    modifier: Modifier = Modifier
) {
    val reactions by message.chatReactions.collectAsState()

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        // TODO If there are multiple reactions of same type we display a number next to the image.
        //  Now we just show the duplicated
        items(reactions) { reaction ->
            Box(
                modifier = Modifier.clickable {
                    if (message.isMyChatReaction(reaction)) {
                        onRemoveReaction(reaction)
                    } else {
                        onAddReaction(ReactionEnum.entries[reaction.reactionId])
                    }
                },
            ) {
                DynamicImage(
                    reaction.imagePath(),
                    modifier = Modifier.size(24.dp)
                        .offset(y = (-3).dp)
                )
            }
        }
    }
}