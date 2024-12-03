package network.bisq.mobile.presentation.ui.components.atoms.icons

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.*
import network.bisq.mobile.domain.PlatformImage
import org.jetbrains.compose.resources.painterResource

expect fun rememberPlatformImagePainter(platformImage: PlatformImage): Painter

@Composable
fun BellIcon(modifier: Modifier = Modifier) {
    Image(painterResource(Res.drawable.icon_bell), "Bell icon", modifier = modifier)
}

@Composable
fun CopyIcon(modifier: Modifier = Modifier) {
    Image(painterResource(Res.drawable.icon_copy), "Copy icon", modifier = modifier)
}

@Composable
fun QuestionIcon(modifier: Modifier = Modifier) {
    Image(painterResource(Res.drawable.icon_question_mark), "Question icon", modifier = modifier)
}

@Composable
fun ScanIcon(modifier: Modifier = Modifier) {
    Image(painterResource(Res.drawable.icon_qr), "Scan icon", modifier = modifier)
}

@Composable
fun SortIcon(modifier: Modifier = Modifier) {
    Image(painterResource(Res.drawable.icon_sort), "Sort icon", modifier = modifier)
}

@Composable
fun UserIcon(platformImage: PlatformImage?, modifier: Modifier = Modifier) {
    if (platformImage == null) {
        // show default
        Image(painterResource(Res.drawable.img_bot_image), "User icon", modifier = modifier)
    } else {
        val painter = rememberPlatformImagePainter(platformImage)
        Image(painter = painter, contentDescription = "User icon", modifier = modifier)
    }
}