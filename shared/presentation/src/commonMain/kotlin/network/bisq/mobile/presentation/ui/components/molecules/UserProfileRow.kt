package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.BadgedBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.StarRating
import network.bisq.mobile.presentation.ui.components.atoms.animations.AnimatedBadge
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun UserProfileRow(
    userProfile: UserProfileVO,
    reputation: ReputationScoreVO,
    showUserName: Boolean = true,
    userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage,
    badgeCount: Int = 0,
    modifier: Modifier = Modifier,
) {
    val fiveSystemScore = reputation.fiveSystemScore

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BadgedBox(
            modifier = Modifier.graphicsLayer(clip = false),
            badge = {
                if (badgeCount > 0) {
                    AnimatedBadge(
                        text = badgeCount.toString(),
                        xOffset = 3.dp,
                        yOffset = 35.dp
                    )
                }
            }) {

            UserProfileIcon(userProfile, userProfileIconProvider)
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (showUserName) {
                BisqText.baseLight(
                    text = userProfile.userName,
                    singleLine = true,
                )
            }
            StarRating(fiveSystemScore)
        }
    }
}