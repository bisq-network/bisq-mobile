package network.bisq.mobile.presentation.common.ui.components.molecules

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** A single line of icon + text, e.g. for feature/benefit bullet lists. */
@Composable
fun IconTextRow(
    icon: DrawableResource,
    text: String,
    modifier: Modifier = Modifier,
    iconSize: Dp = 30.dp,
    gap: Dp = 15.dp,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(icon), null, Modifier.size(iconSize))
        Spacer(modifier = Modifier.width(gap))
        BisqText.BaseLight(text)
    }
}
