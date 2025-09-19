package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BadgedBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.img_bot_image
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.StarRating
import network.bisq.mobile.presentation.ui.components.atoms.animations.AnimatedBadge
import network.bisq.mobile.presentation.ui.components.atoms.icons.rememberPlatformImagePainter
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.jetbrains.compose.resources.painterResource

@Composable
fun UserProfileRow(
    user: UserProfileVO,
    reputation: ReputationScoreVO,
    showUserName: Boolean = true,
    userAvatar: PlatformImage? = null,
    badgeCount: Int = 0,
    modifier: Modifier = Modifier,
) {
    val fiveSystemScore = reputation.fiveSystemScore

    val painter: Painter = if (userAvatar == null) {
        painterResource(Res.drawable.img_bot_image)
    } else {
        rememberPlatformImagePainter(userAvatar)
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BadgedBox(
            modifier = Modifier.graphicsLayer(clip = false),
            badge = {
                if (badgeCount > 0) {
                    AnimatedBadge(showAnimation = true, xOffset = 2.dp, yOffset = 24.dp) {
                        BisqText.xsmallMedium(
                            badgeCount.toString(),
                            textAlign = TextAlign.Center, color = BisqTheme.colors.dark_grey20
                        )
                    }
                }
            }) {
            Image(
                painter, "",
                modifier = Modifier.size(BisqUIConstants.ScreenPadding3X)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (showUserName) {
                BisqText.baseLight(
                    text = user.userName,
                    singleLine = true,
                )
            }
            StarRating(fiveSystemScore)
        }
    }
}