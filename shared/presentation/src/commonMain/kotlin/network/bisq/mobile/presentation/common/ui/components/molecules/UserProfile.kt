package network.bisq.mobile.presentation.common.ui.components.molecules

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.presentation.common.ui.components.atoms.AutoResizeText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.StarRating
import network.bisq.mobile.presentation.common.ui.components.atoms.debouncedClickable
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.LanguageIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.InlineOrStacked
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants

const val USER_PROFILE_LANGUAGES_TAG = "userProfile.languages"

@Composable
fun UserProfile(
    userProfile: UserProfileVO,
    userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage,
    reputation: StateFlow<ReputationScoreVO>,
    supportedLanguageCodes: List<String>,
    modifier: Modifier = Modifier,
    showUserName: Boolean = true,
    onIconClick: (() -> Unit)? = null,
) {
    val reputationScore by reputation.collectAsState()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        // The avatar alone carries the click, as in the chat bubble's `ProfileIconAndText` — the
        // rating and languages below belong to whatever surface hosts this column.
        val iconModifier =
            if (onIconClick == null) {
                Modifier
            } else {
                Modifier.debouncedClickable(role = Role.Button, onClick = onIconClick)
            }
        Box(modifier = iconModifier) {
            UserProfileIcon(userProfile, userProfileIconProvider)
        }

        BisqGap.V1()
        Column {
            if (showUserName) {
                BisqText.BaseLight(
                    text = userProfile.userName,
                    singleLine = true,
                )
                BisqGap.VQuarter()
            }
            StarRating(reputationScore.fiveSystemScore)
        }
        BisqGap.V2()
        // In a narrow column at a large font size the codes move below the glyph rather than
        // splitting a code such as "EN" across two lines.
        InlineOrStacked(
            leading = { LanguageIcon() },
            separator = { BisqText.SmallRegularGrey(" : ") },
            trailing = {
                AutoResizeText(
                    text = supportedLanguageCodes.joinToString(", ").uppercase(),
                    overflow = TextOverflow.Ellipsis,
                    textStyle = BisqTheme.typography.smallRegular,
                    maxLines = 2,
                    minimumFontSize = 10.sp,
                    modifier = Modifier.testTag(USER_PROFILE_LANGUAGES_TAG),
                )
            },
            stackedSpacing = BisqUIConstants.ScreenPaddingQuarter,
            stackedAlignment = Alignment.CenterHorizontally,
        )
    }
}
