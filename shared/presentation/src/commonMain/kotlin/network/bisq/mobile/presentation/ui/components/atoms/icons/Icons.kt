package network.bisq.mobile.presentation.ui.components.atoms.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.*
import network.bisq.mobile.domain.PlatformImage
import org.jetbrains.compose.resources.painterResource

expect fun rememberPlatformImagePainter(platformImage: PlatformImage): Painter

@Composable
fun BellIcon(modifier: Modifier = Modifier.size(30.dp)) {
    Image(painterResource(Res.drawable.icon_bell), "Bell icon", modifier = modifier)
}

@Composable
fun ChatIcon(modifier: Modifier = Modifier) {
    Image(painterResource(Res.drawable.icon_chat_outlined), "Chat icon", modifier = modifier)
}

@Composable
fun CopyIcon(modifier: Modifier = Modifier) {
    Image(painterResource(Res.drawable.icon_copy), "Copy icon", modifier = modifier)
}

@Composable
fun SwapHArrowIcon(modifier: Modifier = Modifier.size(16.dp)) {
    Image(painterResource(Res.drawable.exchange_h_arrow), "Swap horizontal icon", modifier = modifier)
}

@Composable
fun SwapVArrowIcon(modifier: Modifier = Modifier.size(16.dp)) {
    Image(painterResource(Res.drawable.exchange_v_arrow), "Swap vertical icon", modifier = modifier)
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
fun StarEmptyIcon(modifier: Modifier = Modifier.size(16.dp)) {
    // TODO: Import right resource for this
    Image(painterResource(Res.drawable.icon_star), "Empty star icon", modifier = modifier)
}

@Composable
fun StarFillIcon(modifier: Modifier = Modifier.size(16.dp)) {
    Image(painterResource(Res.drawable.icon_star), "Filled star icon", modifier = modifier)
}

@Composable
fun UpIcon(modifier: Modifier = Modifier.size(30.dp)) {
    Image(painterResource(Res.drawable.up_arrow), "Up icon", modifier = modifier)
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