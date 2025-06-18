package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.img_bot_image
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.StarRating
import network.bisq.mobile.presentation.ui.components.atoms.icons.rememberPlatformImagePainter
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.jetbrains.compose.resources.painterResource

@Composable
fun UserProfileRow(
    user: UserProfileVO,
    reputation: ReputationScoreVO,
    showUserName: Boolean = true,
    modifier: Modifier = Modifier,
    userAvatar: PlatformImage? = null,
) {
    val fiveSystemScore = reputation.fiveSystemScore

    val painter: Painter = if (userAvatar == null) {
        painterResource(Res.drawable.img_bot_image)
    } else {
        rememberPlatformImagePainter(userAvatar)
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter, "",
            modifier = Modifier.size(BisqUIConstants.ScreenPadding3X)
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (showUserName) {
                BisqText.baseRegular(
                    text = user.userName,
                    singleLine = true,
                )
            }
            StarRating(fiveSystemScore)
        }
    }
}