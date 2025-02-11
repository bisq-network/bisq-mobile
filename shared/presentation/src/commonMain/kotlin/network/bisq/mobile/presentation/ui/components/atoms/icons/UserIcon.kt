package network.bisq.mobile.presentation.ui.components.atoms.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.img_bot_image
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.presentation.ui.components.atoms.DynamicImage
import org.jetbrains.compose.resources.painterResource

@Composable
fun UserIcon(
    platformImage: PlatformImage?,
    modifier: Modifier = Modifier,
    connectivityStatus: ConnectivityService.ConnectivityStatus
) {
    if (platformImage == null) {
        // show default
        Image(painterResource(Res.drawable.img_bot_image), "User icon", modifier = modifier)
    } else {
        Box(modifier = modifier.padding(0.dp), contentAlignment = Alignment.BottomEnd) {
            val painter = rememberPlatformImagePainter(platformImage)
            Image(painter = painter, contentDescription = "User icon", modifier = Modifier.fillMaxSize())
            GlowEffect(connectivityStatus)
        }
    }
}

@Composable
fun GlowEffect(connectivityStatus: ConnectivityService.ConnectivityStatus) {
    var scale by remember { mutableStateOf(0) }
    val name = when (connectivityStatus) {
        ConnectivityService.ConnectivityStatus.CONNECTED -> "green-small-dot.png"
        ConnectivityService.ConnectivityStatus.SLOW -> "yellow-small-dot.png"
        ConnectivityService.ConnectivityStatus.DISCONNECTED -> "grey-small-dot.png"
    }
    // For UserProfileSettingsScreen (scale > 20), need different offset to look right
    val offset = if (scale > 20)
        scale / 4
    else
        scale / 5
    DynamicImage(
        "drawable/chat/$name",
        modifier = Modifier
            .size(width = scale.dp, height = scale.dp)
            .offset(x = offset.dp, y = offset.dp)
            .onGloballyPositioned { layoutCoordinates ->
                scale = (layoutCoordinates.parentLayoutCoordinates?.size?.width ?: 0) / 6
            },
    )
}

/*
@Composable
fun GlowEffect() {
    var scale by remember { mutableStateOf(0) }
    Box(
        modifier = Modifier
            .size(width = scale.dp, height = scale.dp)
            .padding(0.dp)
            .offset(x = (-scale / 4).dp, y = (-scale / 4).dp)
            .background(Color.Green.copy(alpha = 0.5F), CircleShape)
            .onGloballyPositioned { layoutCoordinates ->
                scale = (layoutCoordinates.parentLayoutCoordinates?.size?.width ?: 0) / 10
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = (scale / 2).dp, height = (scale / 2).dp)
                .shadow((scale / 4).dp, CircleShape)
                .background(Color.Green, CircleShape)
        )
    }

}
*/