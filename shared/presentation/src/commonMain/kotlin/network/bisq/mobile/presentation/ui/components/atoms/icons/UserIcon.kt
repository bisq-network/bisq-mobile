package network.bisq.mobile.presentation.ui.components.atoms.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.connected_and_data_received
import bisqapps.shared.presentation.generated.resources.img_bot_image
import bisqapps.shared.presentation.generated.resources.no_connections
import bisqapps.shared.presentation.generated.resources.requesting_inventory
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.domain.service.network.ConnectivityService.ConnectivityStatus.CONNECTED_AND_DATA_RECEIVED
import network.bisq.mobile.domain.service.network.ConnectivityService.ConnectivityStatus.REQUESTING_INVENTORY
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
            Image(painter = painter, contentDescription = "User icon", modifier = Modifier.padding(2.dp))
        }
        ConnectivityIndicator(connectivityStatus)
    }
}

@Composable
fun ConnectivityIndicator(connectivityStatus: ConnectivityService.ConnectivityStatus) {
    if (connectivityStatus == CONNECTED_AND_DATA_RECEIVED) {
        Image(painterResource(Res.drawable.connected_and_data_received), "Connected and data received")
    } else if (connectivityStatus == REQUESTING_INVENTORY) {
        Image(painterResource(Res.drawable.requesting_inventory), "Requesting inventory data")
    } else {
        Image(painterResource(Res.drawable.no_connections), "No connections")
    }
}