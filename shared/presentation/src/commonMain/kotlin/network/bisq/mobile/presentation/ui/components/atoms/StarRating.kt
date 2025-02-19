package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.components.atoms.icons.StarEmptyIcon
import network.bisq.mobile.presentation.ui.components.atoms.icons.StarFillIcon
import network.bisq.mobile.presentation.ui.components.atoms.icons.StarHalfFilledIcon
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun StarRating(rating: Double) {

    val fullStars = rating.toInt()
    val hasHalfStar = rating - fullStars >= 0.5
    val emptyStars = 5 - fullStars - if (hasHalfStar) 1 else 0

    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        repeat(fullStars) {
            StarFillIcon(modifier = Modifier.size(BisqUIConstants.ScreenPadding))
        }
        if(hasHalfStar) {
            StarHalfFilledIcon(modifier = Modifier.size(BisqUIConstants.ScreenPadding))
        }
        repeat(emptyStars) {
            StarEmptyIcon(modifier = Modifier.size(BisqUIConstants.ScreenPadding))
        }
    }
}