package network.bisq.mobile.presentation.common.ui.components.atoms.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.connected_and_data_received
import bisqapps.shared.presentation.generated.resources.no_connections
import bisqapps.shared.presentation.generated.resources.requesting_inventory
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.service.network.ConnectivityService.ConnectivityStatus
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.presentation.common.ui.components.atoms.animations.ShineOverlay
import network.bisq.mobile.presentation.common.ui.components.molecules.UserProfileIcon
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import org.jetbrains.compose.resources.painterResource

@Composable
fun MyUserProfileIcon(
    userProfile: UserProfileVO,
    userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage,
    connectivityStatus: ConnectivityStatus,
    showAnimations: Boolean,
    modifier: Modifier = Modifier,
) {
    val useAnimation = showAnimations && connectivityStatus == ConnectivityStatus.CONNECTED_AND_DATA_RECEIVED
    Box(modifier = modifier.padding(0.dp), contentAlignment = Alignment.BottomEnd) {
        if (useAnimation) {
            ShineOverlay {
                UserProfileIcon(userProfile, userProfileIconProvider, BisqUIConstants.topBarAvatarSize)
            }
        } else {
            UserProfileIcon(userProfile, userProfileIconProvider, BisqUIConstants.topBarAvatarSize)
        }
        ConnectivityIndicator(connectivityStatus)
    }
}

@Composable
fun ConnectivityIndicator(connectivityStatus: ConnectivityStatus) {
    val iconRes =
        when (connectivityStatus) {
            ConnectivityStatus.CONNECTED_AND_DATA_RECEIVED -> Res.drawable.connected_and_data_received

            ConnectivityStatus.REQUESTING_INVENTORY,
            ConnectivityStatus.CONNECTED_WITH_LIMITATIONS,
            -> Res.drawable.requesting_inventory

            else ->
                Res.drawable.no_connections
        }

    Image(
        painter = painterResource(iconRes),
        contentDescription = connectivityStatus.i18n(),
    )
}
