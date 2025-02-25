package network.bisq.mobile.presentation.ui.components.atoms.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.*
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.exchange_h_arrow
import bisqapps.shared.presentation.generated.resources.exchange_v_arrow
import bisqapps.shared.presentation.generated.resources.icon_add_filled_green
import bisqapps.shared.presentation.generated.resources.icon_arrow_down
import bisqapps.shared.presentation.generated.resources.icon_bell
import bisqapps.shared.presentation.generated.resources.icon_chat_outlined
import bisqapps.shared.presentation.generated.resources.icon_closed_eye
import bisqapps.shared.presentation.generated.resources.icon_copy
import bisqapps.shared.presentation.generated.resources.icon_flag
import bisqapps.shared.presentation.generated.resources.icon_flash_light
import bisqapps.shared.presentation.generated.resources.icon_gallery
import bisqapps.shared.presentation.generated.resources.icon_info
import bisqapps.shared.presentation.generated.resources.icon_language_grey
import bisqapps.shared.presentation.generated.resources.icon_qr
import bisqapps.shared.presentation.generated.resources.icon_question_mark
import bisqapps.shared.presentation.generated.resources.icon_reply
import bisqapps.shared.presentation.generated.resources.icon_search_dimmed
import bisqapps.shared.presentation.generated.resources.icon_send
import bisqapps.shared.presentation.generated.resources.icon_sort
import bisqapps.shared.presentation.generated.resources.icon_star_green
import bisqapps.shared.presentation.generated.resources.icon_star_grey_hollow
import bisqapps.shared.presentation.generated.resources.icon_star_half_green
import bisqapps.shared.presentation.generated.resources.remove_offer
import bisqapps.shared.presentation.generated.resources.up_arrow
import network.bisq.mobile.domain.PlatformImage
import org.jetbrains.compose.resources.painterResource

expect fun rememberPlatformImagePainter(platformImage: PlatformImage): Painter

@Composable
fun CloseIcon(modifier: Modifier = Modifier.size(24.dp)) {
    Icon(
        Icons.Filled.Close,
        "close",
        modifier = modifier,
        tint = Color.White
    )
//    Icon(
//        Icons.Default.Close,
//        tint = Color.White,
//        contentDescription = null
//    )
}

@Composable
fun ExclamationRedIcon(modifier: Modifier = Modifier.size(24.dp)) {
    Image(painterResource(Res.drawable.icon_exclamation_red), "Exclamation red icon", modifier = modifier)
}

@Composable
fun ClosedEyeIcon(modifier: Modifier = Modifier.size(24.dp)) {
    Image(painterResource(Res.drawable.icon_closed_eye), "Closed eye icon", modifier = modifier)
}

@Composable
fun AddIcon(modifier: Modifier = Modifier.size(16.dp)) {
    Image(painterResource(Res.drawable.icon_add), "Add icon", modifier = modifier)
}

@Composable
fun AddCircleIcon(modifier: Modifier = Modifier.size(16.dp)) {
    Image(painterResource(Res.drawable.field_add_white), "Add circle icon", modifier = modifier)
}

@Composable
fun ArrowDownIcon(modifier: Modifier = Modifier.size(12.dp)) {
    Image(painterResource(Res.drawable.icon_arrow_down), "Down arrow icon", modifier = modifier)
}

@Composable
fun ArrowRightIcon(modifier: Modifier = Modifier.size(12.dp)) {
    Image(painterResource(Res.drawable.icon_arrow_right), "Right arrow icon", modifier = modifier)
}

@Composable
fun BellIcon(modifier: Modifier = Modifier.size(30.dp)) {
    Image(painterResource(Res.drawable.icon_bell), "Bell icon", modifier = modifier)
}

@Composable
fun ChatIcon(modifier: Modifier = Modifier.size(24.dp)) {
    Image(painterResource(Res.drawable.icon_chat_outlined), "Chat icon", modifier = modifier)
}

@Composable
fun RemoveOfferIcon(modifier: Modifier = Modifier) {
    Image(painterResource(Res.drawable.remove_offer), "Remove offer icon", modifier = modifier)
}

@Composable
fun CopyIcon(modifier: Modifier = Modifier) {
    Image(painterResource(Res.drawable.icon_copy), "Copy icon", modifier = modifier)
}

@Composable
fun FlagIcon(modifier: Modifier = Modifier.size(24.dp)) {
    Image(painterResource(Res.drawable.icon_flag), "Flag icon", modifier = modifier)
}

@Composable
fun FlashLightIcon(modifier: Modifier = Modifier.size(24.dp)) {
    Image(painterResource(Res.drawable.icon_flash_light), "Flash light icon", modifier = modifier)
}

@Composable
fun LanguageIcon(modifier: Modifier = Modifier.size(16.dp)) {
    Image(painterResource(Res.drawable.icon_language_grey), "Language icon", modifier = modifier)
}

@Composable
fun InfoIcon(modifier: Modifier = Modifier.size(16.dp)) {
    Image(painterResource(Res.drawable.icon_info), "Info icon", modifier = modifier)
}

@Composable
fun GalleryIcon(modifier: Modifier = Modifier.size(24.dp)) {
    Image(painterResource(Res.drawable.icon_gallery), "Gallery icon", modifier = modifier)
}

@Composable
fun PasteIcon(modifier: Modifier = Modifier) {
    Image(painterResource(Res.drawable.icon_paste), "Paste icon", modifier = modifier)
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
fun ReplyIcon(modifier: Modifier = Modifier.size(24.dp)) {
    Image(painterResource(Res.drawable.icon_reply), "Reply icon", modifier = modifier)
}

@Composable
fun ScanIcon(modifier: Modifier = Modifier) {
    Image(painterResource(Res.drawable.icon_qr), "Scan icon", modifier = modifier)
}

@Composable
fun SendIcon(modifier: Modifier = Modifier.size(24.dp)) {
    Image(painterResource(Res.drawable.icon_send), "Send icon", modifier = modifier)
}

@Composable
fun SearchIcon(modifier: Modifier = Modifier.size(16.dp)) {
    Image(painterResource(Res.drawable.icon_search_dimmed), "Search icon", modifier = modifier)
}

@Composable
fun SortIcon(modifier: Modifier = Modifier) {
    Image(painterResource(Res.drawable.icon_sort), "Sort icon", modifier = modifier)
}

@Composable
fun StarEmptyIcon(modifier: Modifier = Modifier.size(16.dp)) {
    // TODO: Import right resource for this
    Image(painterResource(Res.drawable.icon_star_grey_hollow), "Empty star icon", modifier = modifier)
}

@Composable
fun StarHalfFilledIcon(modifier: Modifier = Modifier.size(16.dp)) {
    Image(painterResource(Res.drawable.icon_star_half_green), "Half filled star icon", modifier = modifier)
}

@Composable
fun StarFillIcon(modifier: Modifier = Modifier.size(16.dp)) {
    Image(painterResource(Res.drawable.icon_star_green), "Filled star icon", modifier = modifier)
}

@Composable
fun UpIcon(modifier: Modifier = Modifier.size(30.dp)) {
    Image(painterResource(Res.drawable.up_arrow), "Up icon", modifier = modifier)
}

@Composable
fun WarningIcon(modifier: Modifier = Modifier.size(24.dp)) {
    Image(painterResource(Res.drawable.icon_warning), "Warning icon", modifier = modifier)
}
