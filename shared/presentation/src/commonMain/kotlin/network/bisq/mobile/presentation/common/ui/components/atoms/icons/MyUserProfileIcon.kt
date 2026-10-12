package network.bisq.mobile.presentation.common.ui.components.atoms.icons

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.connected_and_data_received
import bisqapps.shared.presentation.generated.resources.dummy_user_profile_icon
import bisqapps.shared.presentation.generated.resources.no_connections
import bisqapps.shared.presentation.generated.resources.requesting_inventory
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.service.network.ConnectivityService.ConnectivityStatus
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.animations.ShineOverlay
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import org.jetbrains.compose.resources.painterResource

const val MY_USER_PROFILE_ICON_PLACEHOLDER_TAG = "my_user_profile_icon_placeholder"
const val MY_USER_PROFILE_ICON_AVATAR_TAG = "my_user_profile_icon_avatar"

private const val AVATAR_CROSSFADE_MILLIS = 200

/**
 * The placeholder stays until the avatar image is generated, then the two crossfade. The profile
 * can be missing for a moment after app start, before the user profile service is activated.
 */
@Composable
fun MyUserProfileIcon(
    userProfile: UserProfileVO?,
    userProfileIconProvider: (suspend (UserProfileVO) -> PlatformImage)?,
    connectivityStatus: ConnectivityStatus,
    showAnimations: Boolean,
    modifier: Modifier = Modifier,
) {
    val useAnimation = showAnimations && connectivityStatus == ConnectivityStatus.CONNECTED_AND_DATA_RECEIVED
    val avatar by produceState<PlatformImage?>(initialValue = null, key1 = userProfile) {
        value = userProfile?.let { userProfileIconProvider?.invoke(it) }
    }

    Box(modifier = modifier.padding(0.dp), contentAlignment = Alignment.BottomEnd) {
        Crossfade(
            targetState = avatar,
            animationSpec = if (showAnimations) tween(AVATAR_CROSSFADE_MILLIS) else snap(),
        ) { image ->
            if (image == null) {
                Image(
                    painter = painterResource(Res.drawable.dummy_user_profile_icon),
                    contentDescription = "",
                    modifier = Modifier.size(BisqUIConstants.topBarAvatarSize).testTag(MY_USER_PROFILE_ICON_PLACEHOLDER_TAG),
                )
            } else if (useAnimation) {
                ShineOverlay {
                    Avatar(image)
                }
            } else {
                Avatar(image)
            }
        }
        if (userProfile != null) {
            ConnectivityIndicator(connectivityStatus)
        }
    }
}

@Composable
private fun Avatar(image: PlatformImage) {
    val painter = remember(image) { getPlatformImagePainter(image) }
    Image(
        painter = painter,
        contentDescription = "mobile.createProfile.iconGenerated".i18n(),
        modifier = Modifier.size(BisqUIConstants.topBarAvatarSize).testTag(MY_USER_PROFILE_ICON_AVATAR_TAG),
    )
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
