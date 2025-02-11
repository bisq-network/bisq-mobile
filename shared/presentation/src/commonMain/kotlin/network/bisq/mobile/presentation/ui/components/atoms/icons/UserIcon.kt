package network.bisq.mobile.presentation.ui.components.atoms.icons


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.img_bot_image
import network.bisq.mobile.domain.PlatformImage
import org.jetbrains.compose.resources.painterResource

@Composable
fun UserIcon(platformImage: PlatformImage?, modifier: Modifier = Modifier) {
    if (platformImage == null) {
        // show default
        Image(painterResource(Res.drawable.img_bot_image), "User icon", modifier = modifier)
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.BottomEnd) {
            val painter = rememberPlatformImagePainter(platformImage)
            Image(painter = painter, contentDescription = "User icon", modifier = Modifier.fillMaxSize())
            GlowEffect()
        }
    }
}

@Composable
fun GlowEffect() {
    var scale by remember { mutableStateOf(0) }
    println("GlowEffect.size ${scale}")
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