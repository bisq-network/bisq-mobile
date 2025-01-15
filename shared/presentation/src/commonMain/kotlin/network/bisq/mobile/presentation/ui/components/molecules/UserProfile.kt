package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.img_bot_image
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.StarRating
import org.jetbrains.compose.resources.painterResource

// TODO: Get params and render apt
@Composable
fun UserProfile(item: UserProfileVO, showUserName: Boolean = true) {
    val fiveSystemScore: Double = 3.5 // TODO: item.reputationScore.fiveSystemScore

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(
            painterResource(Res.drawable.img_bot_image), "",
            modifier = Modifier.size(36.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (showUserName) {
                BisqText.baseRegular(
                    text = item.userName,
                    singleLine = true,
                )
            }
            StarRating(fiveSystemScore)
        }
    }
}