package network.bisq.mobile.presentation.ui.components.atoms.icons

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.bell
import bisqapps.shared.presentation.generated.resources.img_bot_image
import org.jetbrains.compose.resources.painterResource

@Composable
fun BellIcon(modifier: Modifier = Modifier) {
    Image(painterResource(Res.drawable.bell), "Bell icon", modifier = modifier)
}

@Composable
fun UserIcon(modifier: Modifier = Modifier) {
    Image(painterResource(Res.drawable.img_bot_image), "User icon", modifier = modifier)
}