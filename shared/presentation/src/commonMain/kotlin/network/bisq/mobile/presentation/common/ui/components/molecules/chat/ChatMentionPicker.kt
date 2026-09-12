package network.bisq.mobile.presentation.common.ui.components.molecules.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

const val CHAT_MENTION_PICKER_TAG = "chat_mention_picker"

internal const val MAX_VISIBLE_MENTION_ROWS = 4
internal val MentionRowHeight = 48.dp

/**
 * Channel-scoped @mention suggestions. Tap-only: Enter stays a newline in the composer.
 *
 * Height is exactly min(size, 4) rows so the overlay stays compact; further names
 * stay reachable by scrolling. The picker itself must not take composer layout
 * space — [ChatInputField] draws it in a [androidx.compose.ui.window.Popup] over
 * the thread, otherwise hub chrome plus the IME leave no room for messages.
 */
@Composable
fun ChatMentionPicker(
    profiles: List<UserProfileVO>,
    onSelect: (UserProfileVO) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = BisqTheme.colors.dark_grey40,
        shape =
            RoundedCornerShape(
                topStart = BisqUIConstants.BorderRadius,
                topEnd = BisqUIConstants.BorderRadius,
            ),
        modifier =
            modifier
                .fillMaxWidth()
                .testTag(CHAT_MENTION_PICKER_TAG),
    ) {
        if (profiles.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(MentionRowHeight)
                        .padding(horizontal = BisqUIConstants.ScreenPadding),
                contentAlignment = Alignment.CenterStart,
            ) {
                BisqText.BaseRegular(
                    "chat.atMentionPopup.placeholder".i18n(),
                    color = BisqTheme.colors.mid_grey20,
                )
            }
        } else {
            LazyColumn(
                modifier =
                    Modifier.height(
                        MentionRowHeight * minOf(profiles.size, MAX_VISIBLE_MENTION_ROWS),
                    ),
            ) {
                items(profiles, key = { it.id }) { profile ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(MentionRowHeight)
                                .padding(horizontal = BisqUIConstants.ScreenPadding)
                                .clickable { onSelect(profile) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BisqText.BaseRegular(profile.userName)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
@ExcludeFromCoverage
private fun ChatMentionPickerPreview() {
    BisqTheme.Preview {
        ChatMentionPicker(
            profiles =
                listOf(
                    createMockUserProfile("Alice"),
                    createMockUserProfile("Bob"),
                    createMockUserProfile("Alice [a1b2c3]").copy(userName = "Alice [a1b2c3]"),
                ),
            onSelect = {},
        )
    }
}

@Preview
@Composable
@ExcludeFromCoverage
private fun ChatMentionPickerEmptyPreview() {
    BisqTheme.Preview {
        ChatMentionPicker(profiles = emptyList(), onSelect = {})
    }
}
