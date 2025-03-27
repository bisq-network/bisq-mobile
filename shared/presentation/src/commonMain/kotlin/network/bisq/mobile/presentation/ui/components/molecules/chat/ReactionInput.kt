package network.bisq.mobile.presentation.ui.components.molecules.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.data.replicated.chat.reactions.BisqEasyOpenTradeMessageReactionVO
import network.bisq.mobile.domain.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.presentation.ui.components.atoms.DynamicImage
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun ChatReactionInput(
    onAddReaction: (ReactionEnum) -> Unit,
    onRemoveReaction: (BisqEasyOpenTradeMessageReactionVO) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        modifier = Modifier
            .padding(
                top = BisqUIConstants.ScreenPaddingHalfQuarter,
                start = BisqUIConstants.ScreenPadding,
                end = BisqUIConstants.ScreenPadding,
                bottom = BisqUIConstants.ScreenPadding,
            )
    ) {
        ReactionEnum.entries.forEach { reaction ->
            // todo make a toggle button with calling onRemoveReaction if it was selected
            DynamicImage(
                path = reaction.imagePath(),
                modifier = Modifier.size(20.dp).clickable(
                    onClick = { onAddReaction(reaction) },
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                )
            )
        }
    }
}

fun ReactionEnum.imagePath(): String {
    return when (this) {
        ReactionEnum.THUMBS_UP -> "drawable/icon_reaction_thumbs_up.png"
        ReactionEnum.THUMBS_DOWN -> "drawable/icon_reaction_thumbs_down.png"
        ReactionEnum.HAPPY -> "drawable/icon_reaction_happy.png"
        ReactionEnum.LAUGH -> "drawable/icon_reaction_laugh.png"
        ReactionEnum.HEART -> "drawable/icon_reaction_heart.png"
        ReactionEnum.PARTY -> "drawable/icon_reaction_party.png"
    }
}

fun BisqEasyOpenTradeMessageReactionVO.imagePath(): String {
    return when (ReactionEnum.entries[this.reactionId]) {
        ReactionEnum.THUMBS_UP -> "drawable/icon_reaction_thumbs_up.png"
        ReactionEnum.THUMBS_DOWN -> "drawable/icon_reaction_thumbs_down.png"
        ReactionEnum.HAPPY -> "drawable/icon_reaction_happy.png"
        ReactionEnum.LAUGH -> "drawable/icon_reaction_laugh.png"
        ReactionEnum.HEART -> "drawable/icon_reaction_heart.png"
        ReactionEnum.PARTY -> "drawable/icon_reaction_party.png"
    }
}

private fun mapReactions(chatMessageReactions: List<BisqEasyOpenTradeMessageReactionVO>) =
    chatMessageReactions
        .filter { !it.isRemoved }
        .map { ReactionEnum.entries[it.reactionId] }




