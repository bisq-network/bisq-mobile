package network.bisq.mobile.presentation.common.ui.components.molecules

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.presentation.common.ui.components.atoms.AutoResizeText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.StarRating
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.LanguageIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme

@Composable
fun UserProfile(
    userProfile: UserProfileVO,
    userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage,
    reputation: StateFlow<ReputationScoreVO>,
    supportedLanguageCodes: List<String>,
    modifier: Modifier = Modifier,
    showUserName: Boolean = true,
) {
    val reputationScore by reputation.collectAsState()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        UserProfileIcon(userProfile, userProfileIconProvider)

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
        Row(verticalAlignment = Alignment.CenterVertically) {
            LanguageIcon()
            BisqText.SmallRegularGrey(" : ")
            AutoResizeText(
                text = supportedLanguageCodes.joinToString(", ").uppercase(),
                overflow = TextOverflow.Ellipsis,
                textStyle = BisqTheme.typography.smallRegular,
                maxLines = 2,
                minimumFontSize = 10.sp,
            )
        }
    }
}
