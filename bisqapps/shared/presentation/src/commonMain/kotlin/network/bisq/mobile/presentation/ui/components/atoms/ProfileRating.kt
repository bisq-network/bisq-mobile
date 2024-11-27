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
import network.bisq.mobile.domain.data.model.offerbook.OfferListItem
import org.jetbrains.compose.resources.painterResource

// TODO: Get params and render apt
@Composable
fun ProfileRating(item: OfferListItem) {
    val fiveSystemScore:Int = item.reputationScore.fiveSystemScore.toInt()

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(
            painterResource(Res.drawable.img_bot_image), "",
            modifier = Modifier.size(32.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            BisqText.smallMedium(
                text = item.userName
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                items(fiveSystemScore) {
                    Image(
                        painterResource(Res.drawable.icon_star), "",
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
    }
}