package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.presentation.ui.components.atoms.icons.getPlatformImagePainter

@Composable
fun UserProfileIcon(
    userProfile: UserProfileVO,
    userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage,
    size: Dp = 50.dp
) {
    val userProfileIcon by produceState<PlatformImage?>(initialValue = null, key1 = userProfile) {
        // Run on IO thread to avoid blocking main
        value = withContext(IODispatcher) {
            userProfileIconProvider.invoke(userProfile)
        }
    }

    if (userProfileIcon != null) {
        Image(
            getPlatformImagePainter(userProfileIcon!!), "",
            modifier = Modifier.size(size)
        )
    } else {
        // Just a placeholder so that we occupy the same space in case the icon is not ready.
        Box(
            modifier = Modifier.size(size),
        )

        // Icon creation is that fast that it does not make sense to show a CircularProgressIndicator
        /*
        CircularProgressIndicator(
            color = BisqTheme.colors.primary,
            modifier = Modifier.size(20.dp),
            strokeWidth = 1.dp,
        )
        */
    }
}
