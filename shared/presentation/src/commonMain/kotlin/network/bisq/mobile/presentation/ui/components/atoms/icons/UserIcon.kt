package network.bisq.mobile.presentation.ui.components.atoms.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.img_bot_image
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.domain.service.network.ConnectivityService.ConnectivityStatus.BOOTSTRAPPING
import network.bisq.mobile.domain.service.network.ConnectivityService.ConnectivityStatus.CONNECTED
import network.bisq.mobile.domain.service.network.ConnectivityService.ConnectivityStatus.DISCONNECTED
import network.bisq.mobile.domain.service.network.ConnectivityService.ConnectivityStatus.WARN
import network.bisq.mobile.presentation.ui.components.atoms.DynamicImage
import org.jetbrains.compose.resources.painterResource

@Composable
fun UserIcon(
    platformImage: PlatformImage?,
    modifier: Modifier = Modifier,
    connectivityStatus: ConnectivityService.ConnectivityStatus
) {
    Box(modifier = modifier.padding(0.dp), contentAlignment = Alignment.BottomEnd) {
        if (platformImage == null) {
            // show default
            Image(painterResource(Res.drawable.img_bot_image), "User icon", modifier = modifier)
        } else {
            val painter = rememberPlatformImagePainter(platformImage)
            Image(painter = painter, contentDescription = "User icon", modifier = Modifier.fillMaxSize())
        }
        GlowEffect(connectivityStatus, Modifier.align(Alignment.BottomEnd))
    }
}

@Composable
fun GlowEffect(connectivityStatus: ConnectivityService.ConnectivityStatus, modifier: Modifier = Modifier) {
    val icon = when (connectivityStatus) {
        BOOTSTRAPPING, CONNECTED -> "green-small-dot.png"
        WARN -> "yellow-small-dot.png"
        DISCONNECTED -> "red-small-dot.png"
    }
    DynamicImage("drawable/chat/$icon", modifier = modifier.scale(2.0F))
}