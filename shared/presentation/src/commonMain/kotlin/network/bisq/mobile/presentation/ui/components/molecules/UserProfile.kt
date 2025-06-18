package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.img_bot_image
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.StarRating
import network.bisq.mobile.presentation.ui.components.atoms.icons.LanguageIcon
import network.bisq.mobile.presentation.ui.components.atoms.icons.rememberPlatformImagePainter
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.jetbrains.compose.resources.painterResource

@Composable
fun UserProfile(
    user: UserProfileVO,
    userAvatar: PlatformImage? = null,
    reputation: StateFlow<ReputationScoreVO>,
    supportedLanguageCodes: List<String>,
    showUserName: Boolean = true,
    modifier: Modifier = Modifier
) {
    val fiveSystemScore: Double = reputation.collectAsState().value.fiveSystemScore

    val painter: Painter = if (userAvatar == null) {
        painterResource(Res.drawable.img_bot_image)
    } else {
        rememberPlatformImagePainter(userAvatar)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Image(
            painter, "",
            modifier = Modifier.size(BisqUIConstants.ScreenPadding3X)
        )
        BisqGap.V1()
        Column {
            if (showUserName) {
                BisqText.baseRegular(
                    text = user.userName,
                    singleLine = true,
                )
                BisqGap.VQuarter()
            }
            StarRating(fiveSystemScore)
        }
        BisqGap.V2()
        Row(verticalAlignment = Alignment.CenterVertically) {
            LanguageIcon()
            BisqText.smallRegularGrey(" : ")
            BisqText.smallRegular(supportedLanguageCodes.joinToString(", ").uppercase())
        }
    }
}