package network.bisq.mobile.presentation.common.ui.components.molecules.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import network.bisq.mobile.data.replicated.chat.ChatMessage
import network.bisq.mobile.data.replicated.chat.mentionRanges
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.rememberDebouncedClick
import network.bisq.mobile.presentation.common.ui.components.molecules.UserProfileIcon
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants

/**
 * @param onLongClick forwarded from the enclosing message bubble. The avatar carries its own
 *   [combinedClickable], which consumes the pointer-down, so without re-dispatching the long press
 *   the bubble's context menu would become unreachable from the avatar.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileIconAndText(
    message: ChatMessage<*>,
    userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage,
    onPeerProfileClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    myProfiles: Collection<UserProfileVO> = emptyList(),
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier.padding(
                vertical = BisqUIConstants.ScreenPaddingHalf,
                horizontal = BisqUIConstants.ScreenPadding,
            ),
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
    ) {
        val openPeerProfile = rememberDebouncedClick { onPeerProfileClick() }

        val icon = @Composable {
            // Own avatar is never a link — the peer profile screen is not for user's own profile.
            val iconModifier =
                if (!message.isMyMessage) {
                    Modifier.combinedClickable(
                        role = Role.Button,
                        onLongClick = onLongClick,
                        onClick = openPeerProfile,
                    )
                } else {
                    Modifier
                }
            Box(modifier = iconModifier) {
                UserProfileIcon(message.senderUserProfile, userProfileIconProvider, 30.dp)
            }
        }

        val text = @Composable {
            MentionBodyText(message.textString, myProfiles)
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

@Composable
private fun MentionBodyText(
    text: String,
    myProfiles: Collection<UserProfileVO>,
) {
    val ranges = remember(text, myProfiles) { mentionRanges(text, myProfiles) }
    if (ranges.isEmpty()) {
        BisqText.BaseRegular(text)
        return
    }
    val mentionStyle =
        SpanStyle(
            color = BisqTheme.colors.primary,
            fontWeight = FontWeight.Medium,
        )
    val annotated =
        remember(text, ranges, mentionStyle) {
            buildAnnotatedString {
                append(text)
                ranges.forEach { range ->
                    addStyle(mentionStyle, range.start, range.endExclusive)
                }
            }
        }
    BisqText.StyledText(text = annotated)
}
