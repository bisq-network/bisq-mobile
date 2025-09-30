package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter

import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.icons.getPlatformImagePainter

@Composable
fun UserProfileIcon(
    userProfile: UserProfileVO,
    userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage,
    size: Dp = 50.dp
) {
    val userProfileIcon by produceState<PlatformImage?>(initialValue = null, key1 = userProfile) {
        // The UserProfileServiceFacade will run with IODispatcher context, thus no need to wrap it here as well
        value = userProfileIconProvider.invoke(userProfile)
    }

    val painter = remember(userProfileIcon) {
        userProfileIcon?.let { getPlatformImagePainter(it) } ?: ColorPainter(Color.Transparent)
    }

    Image(
        painter = painter,
        contentDescription = "mobile.createProfile.iconGenerated".i18n(),
        modifier = Modifier.size(size)
    )
}
