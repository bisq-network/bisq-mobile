package network.bisq.mobile.presentation.common.ui.components.molecules.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.data.replicated.chat.Citation
import network.bisq.mobile.data.replicated.chat.priv.PrivateChatMessage
import network.bisq.mobile.data.replicated.chat.two_party.TwoPartyPrivateChatMessage
import network.bisq.mobile.data.replicated.chat.two_party.createMockTwoPartyPrivateChatMessage
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

@Composable
fun QuoteMessageBubble(
    message: PrivateChatMessage<*>,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val sideBorderColor = BisqTheme.colors.mid_grey20
    val isMyMessage = message.isMyMessage
    val bgColor = if (isMyMessage) BisqTheme.colors.dark_grey50 else BisqTheme.colors.dark_grey20

    Column(
        modifier =
            Modifier
                .padding(
                    top = BisqUIConstants.ScreenPaddingHalf,
                    start = BisqUIConstants.ScreenPaddingHalf,
                    end = BisqUIConstants.ScreenPaddingHalf,
                ).clip(shape = RoundedCornerShape(BisqUIConstants.ScreenPaddingHalf)),
    ) {
        Column(
            modifier =
                Modifier
                    .background(bgColor)
                    .clip(shape = RoundedCornerShape(BisqUIConstants.ScreenPaddingHalf))
                    .clickable(
                        onClick = onClick,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ).drawBehind {
                        drawLine(
                            color = sideBorderColor,
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = 4.dp.toPx(),
                        )
                    }.padding(
                        vertical = BisqUIConstants.ScreenPaddingHalf,
                        horizontal = BisqUIConstants.ScreenPadding,
                    ),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingQuarter),
        ) {
            BisqText.BaseMedium(message.citationAuthorUserName ?: "", color = BisqTheme.colors.mid_grey20)
            BisqText.BaseRegular(message.citationString, color = BisqTheme.colors.mid_grey30) // TODO: Trim this to max 2 lines
        }

        content()
    }
}

/**
 * The two previews differ only in who sent the reply, which is what picks the background shade.
 *
 * `content` is filled with the reply body so the citation-to-reply relation is visible; the only
 * production call site ([ChatTextMessageBox]) leaves the slot empty and places the reply next to the
 * bubble instead.
 */
@ExcludeFromCoverage
private fun previewMessage(isMyMessage: Boolean): TwoPartyPrivateChatMessage {
    val alice = createMockUserProfile("Alice")
    val bob = createMockUserProfile("Bob")
    return createMockTwoPartyPrivateChatMessage(
        id = "msg-1",
        text = "Yes, that works for me.",
        citation =
            Citation(
                authorUserProfileId = "Alice",
                text = "Can we settle tomorrow morning?",
                chatMessageId = "msg-0",
            ),
        citationAuthorUserProfile = alice,
        senderUserProfile = if (isMyMessage) bob else alice,
        myUserProfile = bob,
    )
}

@ExcludeFromCoverage
@Preview
@Composable
private fun QuoteMessageBubbleMyMessagePreview() {
    BisqTheme.Preview {
        val message = previewMessage(isMyMessage = true)
        QuoteMessageBubble(message = message, onClick = {}) {
            BisqText.BaseRegular(
                text = message.textString,
                modifier =
                    Modifier.padding(
                        vertical = BisqUIConstants.ScreenPaddingHalf,
                        horizontal = BisqUIConstants.ScreenPadding,
                    ),
            )
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun QuoteMessageBubblePeerMessagePreview() {
    BisqTheme.Preview {
        val message = previewMessage(isMyMessage = false)
        QuoteMessageBubble(message = message, onClick = {}) {
            BisqText.BaseRegular(
                text = message.textString,
                modifier =
                    Modifier.padding(
                        vertical = BisqUIConstants.ScreenPaddingHalf,
                        horizontal = BisqUIConstants.ScreenPadding,
                    ),
            )
        }
    }
}
