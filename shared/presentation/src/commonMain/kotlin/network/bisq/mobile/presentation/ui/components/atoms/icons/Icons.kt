package network.bisq.mobile.presentation.ui.components.atoms.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.add_custom_green
import bisqapps.shared.presentation.generated.resources.check_circle
import bisqapps.shared.presentation.generated.resources.exchange_h_arrow
import bisqapps.shared.presentation.generated.resources.exchange_v_arrow
import bisqapps.shared.presentation.generated.resources.field_add_white
import bisqapps.shared.presentation.generated.resources.icon_add
import bisqapps.shared.presentation.generated.resources.icon_app_link
import bisqapps.shared.presentation.generated.resources.icon_arrow_down
import bisqapps.shared.presentation.generated.resources.icon_arrow_down_dark
import bisqapps.shared.presentation.generated.resources.icon_arrow_right
import bisqapps.shared.presentation.generated.resources.icon_bell
import bisqapps.shared.presentation.generated.resources.icon_chat_outlined
import bisqapps.shared.presentation.generated.resources.icon_closed_eye
import bisqapps.shared.presentation.generated.resources.icon_copy
import bisqapps.shared.presentation.generated.resources.icon_exclamation_red
import bisqapps.shared.presentation.generated.resources.icon_eye
import bisqapps.shared.presentation.generated.resources.icon_flag
import bisqapps.shared.presentation.generated.resources.icon_flash_light
import bisqapps.shared.presentation.generated.resources.icon_gallery
import bisqapps.shared.presentation.generated.resources.icon_info
import bisqapps.shared.presentation.generated.resources.icon_info_green
import bisqapps.shared.presentation.generated.resources.icon_info_green_filled
import bisqapps.shared.presentation.generated.resources.icon_language_grey
import bisqapps.shared.presentation.generated.resources.icon_paste
import bisqapps.shared.presentation.generated.resources.icon_qr
import bisqapps.shared.presentation.generated.resources.icon_question_mark
import bisqapps.shared.presentation.generated.resources.icon_reply
import bisqapps.shared.presentation.generated.resources.icon_scan_qr
import bisqapps.shared.presentation.generated.resources.icon_search_dimmed
import bisqapps.shared.presentation.generated.resources.icon_send
import bisqapps.shared.presentation.generated.resources.icon_sort
import bisqapps.shared.presentation.generated.resources.icon_sort_green
import bisqapps.shared.presentation.generated.resources.icon_star_green
import bisqapps.shared.presentation.generated.resources.icon_star_grey_hollow
import bisqapps.shared.presentation.generated.resources.icon_star_half_green
import bisqapps.shared.presentation.generated.resources.icon_warning
import bisqapps.shared.presentation.generated.resources.icon_warning_filled
import bisqapps.shared.presentation.generated.resources.icon_warning_grey
import bisqapps.shared.presentation.generated.resources.icon_warning_light_grey
import bisqapps.shared.presentation.generated.resources.icon_warning_white
import bisqapps.shared.presentation.generated.resources.icon_web_link
import bisqapps.shared.presentation.generated.resources.leave_chat_green
import bisqapps.shared.presentation.generated.resources.remove_offer
import bisqapps.shared.presentation.generated.resources.up_arrow
import network.bisq.mobile.domain.PlatformImage
import org.jetbrains.compose.resources.painterResource

expect fun getPlatformImagePainter(platformImage: PlatformImage): Painter

@Composable
fun CloseIcon(modifier: Modifier = Modifier.size(24.dp), color: Color = Color.White) {
    Icon(
        Icons.Filled.Close,
        "close",
        modifier = modifier,
        tint = color
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
fun EyeIcon(modifier: Modifier = Modifier.size(24.dp)) {
    Image(painterResource(Res.drawable.icon_eye), "Eye icon", modifier = modifier)
}

@Composable
fun ClosedEyeIcon(modifier: Modifier = Modifier.size(24.dp)) {
    Image(painterResource(Res.drawable.icon_closed_eye), "Closed eye icon", modifier = modifier)
}

@Composable
fun AddSquareIcon(modifier: Modifier = Modifier.size(16.dp)) {
    Image(painterResource(Res.drawable.add_custom_green), "Square Add icon", modifier = modifier)
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
fun ArrowDownIconDark(modifier: Modifier = Modifier.size(24.dp)) {
    Image(painterResource(Res.drawable.icon_arrow_down_dark), "Down arrow icon", modifier = modifier)
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
fun CheckCircleIcon(modifier: Modifier = Modifier.size(24.dp)) {
    Image(painterResource(Res.drawable.check_circle), "Check circle icon", modifier = modifier)
}

@Composable
fun RemoveOfferIcon(modifier: Modifier = Modifier.size(20.dp)) {
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
fun InfoGreenFilledIcon(modifier: Modifier = Modifier.size(24.dp)) {
    Image(painterResource(Res.drawable.icon_info_green_filled), "Green filled info icon", modifier = modifier)
}

@Composable
fun InfoGreenIcon(modifier: Modifier = Modifier.size(24.dp)) {
    Image(painterResource(Res.drawable.icon_info_green), "Green info icon", modifier = modifier)
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
fun ScanQrIcon(modifier: Modifier = Modifier) {
    Image(painterResource(Res.drawable.icon_scan_qr), "Scan icon", modifier = modifier)
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
fun GreenSortIcon(modifier: Modifier = Modifier) {
    Image(painterResource(Res.drawable.icon_sort_green), "Sort icon", modifier = modifier)
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

@Composable
fun WarningIconFilled(modifier: Modifier = Modifier.size(24.dp)) {
    Image(painterResource(Res.drawable.icon_warning_filled), "Filled Warning icon", modifier = modifier)
}

@Composable
fun WarningIconLightGrey(modifier: Modifier = Modifier.size(24.dp)) {
    Image(painterResource(Res.drawable.icon_warning_light_grey), "Warning icon", modifier = modifier)
}

@Composable
fun WarningIconGrey(modifier: Modifier = Modifier.size(24.dp)) {
    Image(painterResource(Res.drawable.icon_warning_grey), "Warning icon", modifier = modifier)
}

@Composable
fun WarningIconWhite(modifier: Modifier = Modifier.size(24.dp)) {
    Image(painterResource(Res.drawable.icon_warning_white), "Warning icon", modifier = modifier)
}

@Composable
fun LeaveChatIcon(modifier: Modifier = Modifier.size(24.dp)) {
    Image(painterResource(Res.drawable.leave_chat_green), "Leave chat icon", modifier = modifier)
}

@Composable
fun AppLinkIcon(modifier: Modifier = Modifier.size(24.dp)) {
    Image(painterResource(Res.drawable.icon_app_link), "App link icon", modifier = modifier)
}

@Composable
fun WebLinkIcon(modifier: Modifier = Modifier.size(24.dp)) {
    Image(painterResource(Res.drawable.icon_web_link), "Web link icon", modifier = modifier)
}
