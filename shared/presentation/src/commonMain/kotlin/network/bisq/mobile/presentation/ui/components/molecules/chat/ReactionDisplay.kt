package network.bisq.mobile.presentation.ui.components.molecules.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel
import network.bisq.mobile.domain.data.replicated.chat.reactions.BisqEasyOpenTradeMessageReactionVO
import network.bisq.mobile.domain.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.DynamicImage
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun ReactionDisplay(
    message: BisqEasyOpenTradeMessageModel,
    onAddReaction: (ReactionEnum) -> Unit,
    onRemoveReaction: (BisqEasyOpenTradeMessageReactionVO) -> Unit,
    modifier: Modifier = Modifier
) {
    val reactions by message.chatReactions.collectAsState()
    val groupedReactions = reactions.groupBy { it.reactionId }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        items(groupedReactions.entries.toList()) { (reactionId, group) ->
            val firstReaction = group.first()
            val myReaction = group.firstOrNull { message.isMyChatReaction(it) }
            val count = group.size
            Box(
                modifier = Modifier.clickable {
                    if (myReaction != null) {
                        onRemoveReaction(myReaction)
                    } else {
                        onAddReaction(ReactionEnum.entries[reactionId])
                    }
                },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(BisqTheme.colors.dark_grey30)
                        .border(
                            1.dp,
                            BisqTheme.colors.mid_grey10,
                            RoundedCornerShape(BisqUIConstants.ScreenPadding)
                        )
                        .padding(all = BisqUIConstants.ScreenPaddingHalfQuarter)
                ) {
                    DynamicImage(
                        firstReaction.imagePath(),
                        modifier = Modifier.size(24.dp)
                            .offset(y = (-3).dp)
                    )
                    if (count > 1) {
                        BisqText.baseLight(
                            text = count.toString(),
                            modifier = Modifier.offset(x = 2.dp, y = (-3).dp),
                        )
                    }
                }
            }
        }
    }
}