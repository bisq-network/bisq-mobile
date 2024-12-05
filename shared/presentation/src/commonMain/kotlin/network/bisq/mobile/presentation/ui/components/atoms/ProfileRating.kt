package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.icon_star
import bisqapps.shared.presentation.generated.resources.img_bot_image
import network.bisq.mobile.domain.data.model.OfferListItem
import network.bisq.mobile.presentation.ui.components.atoms.icons.StarEmptyIcon
import network.bisq.mobile.presentation.ui.components.atoms.icons.StarFillIcon
import org.jetbrains.compose.resources.painterResource

// TODO: Get params and render apt
@Composable
fun ProfileRating(item: OfferListItem) {
    val fiveSystemScore:Int = 3 // item.reputationScore.fiveSystemScore.toInt()

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(
            painterResource(Res.drawable.img_bot_image), "",
            modifier = Modifier.size(48.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            BisqText.largeRegular(text = item.userName)
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                // TODO: Find right icons from Bisq2 and update
                repeat(fiveSystemScore) {
                    StarFillIcon()
                }
                repeat(5 - fiveSystemScore) {
                    StarEmptyIcon()
                }
            }
        }
    }
}