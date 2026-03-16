package network.bisq.mobile.presentation.common.ui.components.atoms.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.add_custom_green
import bisqapps.shared.presentation.generated.resources.check_circle
import bisqapps.shared.presentation.generated.resources.delivery_status_connecting
import bisqapps.shared.presentation.generated.resources.delivery_status_mailbox
import bisqapps.shared.presentation.generated.resources.delivery_status_received
import bisqapps.shared.presentation.generated.resources.delivery_status_sent
import bisqapps.shared.presentation.generated.resources.delivery_status_try_again
import bisqapps.shared.presentation.generated.resources.delivery_status_undelivered
import bisqapps.shared.presentation.generated.resources.exchange_h_arrow
import bisqapps.shared.presentation.generated.resources.exchange_v_arrow
import bisqapps.shared.presentation.generated.resources.field_add_white
import bisqapps.shared.presentation.generated.resources.icon_add
import bisqapps.shared.presentation.generated.resources.icon_app_link
import bisqapps.shared.presentation.generated.resources.icon_arrow_down
import bisqapps.shared.presentation.generated.resources.icon_arrow_down_dark
import bisqapps.shared.presentation.generated.resources.icon_arrow_right
import bisqapps.shared.presentation.generated.resources.icon_bell
import bisqapps.shared.presentation.generated.resources.icon_chat
import bisqapps.shared.presentation.generated.resources.icon_chat_outlined
import bisqapps.shared.presentation.generated.resources.icon_closed_eye
import bisqapps.shared.presentation.generated.resources.icon_copy
import bisqapps.shared.presentation.generated.resources.icon_exclamation_red
import bisqapps.shared.presentation.generated.resources.icon_expand_all
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
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

expect fun getPlatformImagePainter(platformImage: PlatformImage): Painter

/**
 * Helper function to render icons that use Compose Multiplatform Resources.
 *
 * In test environments (when LocalIsTest is true), renders a fallback Material icon
 * instead of loading the resource, avoiding Robolectric resource loading issues.
 */
@Composable
private fun BisqResourceIcon(
    resource: DrawableResource,
    contentDescription: String,
    modifier: Modifier = Modifier,
    colorFilter: ColorFilter? = null,
) {
    val isTest = LocalIsTest.current
    if (isTest) {
        Icon(Icons.Default.Warning, contentDescription, modifier)
    } else {
        Image(
            painter = painterResource(resource),
            contentDescription = contentDescription,
            modifier = modifier,
            colorFilter = colorFilter,
        )
    }
}

@ExcludeFromCoverage
@Composable
fun CloseIcon(
    modifier: Modifier = Modifier.size(24.dp),
    color: Color = Color.White,
) {
    Icon(
        Icons.Filled.Close,
        "close",
        modifier = modifier,
        tint = color,
    )
}

@ExcludeFromCoverage
@Composable
fun SaveIcon(
    modifier: Modifier = Modifier.size(24.dp),
    color: Color = Color.White,
) {
    Icon(
        Icons.Filled.Check,
        "save",
        modifier = modifier,
        tint = color,
    )
}

@ExcludeFromCoverage
@Composable
fun ExclamationRedIcon(modifier: Modifier = Modifier.size(24.dp)) {
    BisqResourceIcon(Res.drawable.icon_exclamation_red, "Exclamation red icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun EyeIcon(modifier: Modifier = Modifier.size(24.dp)) {
    BisqResourceIcon(Res.drawable.icon_eye, "Eye icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun ClosedEyeIcon(modifier: Modifier = Modifier.size(24.dp)) {
    BisqResourceIcon(Res.drawable.icon_closed_eye, "Closed eye icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun AddSquareIcon(modifier: Modifier = Modifier.size(16.dp)) {
    BisqResourceIcon(Res.drawable.add_custom_green, "Square Add icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun AddIcon(modifier: Modifier = Modifier.size(16.dp)) {
    BisqResourceIcon(Res.drawable.icon_add, "Add icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun AddCircleIcon(modifier: Modifier = Modifier.size(16.dp)) {
    BisqResourceIcon(Res.drawable.field_add_white, "Add circle icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun ArrowDownIcon(modifier: Modifier = Modifier.size(12.dp)) {
    BisqResourceIcon(Res.drawable.icon_arrow_down, "Down arrow icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun ExpandAllIcon(modifier: Modifier = Modifier.size(20.dp)) {
    BisqResourceIcon(Res.drawable.icon_expand_all, "Expand all icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun ArrowDownIconDark(modifier: Modifier = Modifier.size(24.dp)) {
    BisqResourceIcon(Res.drawable.icon_arrow_down_dark, "Down arrow icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun ArrowRightIcon(modifier: Modifier = Modifier.size(12.dp)) {
    BisqResourceIcon(Res.drawable.icon_arrow_right, "Right arrow icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun BellIcon(modifier: Modifier = Modifier.size(30.dp)) {
    BisqResourceIcon(Res.drawable.icon_bell, "Bell icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun ChatIconOutlined(modifier: Modifier = Modifier.size(24.dp)) {
    BisqResourceIcon(Res.drawable.icon_chat_outlined, "Chat icon outlined", modifier)
}

@ExcludeFromCoverage
@Composable
fun ChatIcon(modifier: Modifier = Modifier.size(24.dp)) {
    BisqResourceIcon(Res.drawable.icon_chat, "Chat icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun CheckCircleIcon(modifier: Modifier = Modifier.size(24.dp)) {
    BisqResourceIcon(Res.drawable.check_circle, "Check circle icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun RemoveOfferIcon(modifier: Modifier = Modifier.size(20.dp)) {
    BisqResourceIcon(Res.drawable.remove_offer, "Remove offer icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun CopyIcon(modifier: Modifier = Modifier) {
    BisqResourceIcon(
        resource = Res.drawable.icon_copy,
        contentDescription = "Copy icon",
        modifier = modifier,
    )
}

@ExcludeFromCoverage
@Composable
fun FlagIcon(modifier: Modifier = Modifier.size(24.dp)) {
    BisqResourceIcon(Res.drawable.icon_flag, "Flag icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun FlashLightIcon(modifier: Modifier = Modifier.size(24.dp)) {
    BisqResourceIcon(Res.drawable.icon_flash_light, "Flash light icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun LanguageIcon(modifier: Modifier = Modifier.size(16.dp)) {
    BisqResourceIcon(Res.drawable.icon_language_grey, "Language icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun InfoIcon(modifier: Modifier = Modifier.size(16.dp)) {
    BisqResourceIcon(Res.drawable.icon_info, "Info icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun InfoGreenFilledIcon(modifier: Modifier = Modifier.size(24.dp)) {
    BisqResourceIcon(Res.drawable.icon_info_green_filled, "Green filled info icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun InfoGreenIcon(modifier: Modifier = Modifier.size(24.dp)) {
    BisqResourceIcon(Res.drawable.icon_info_green, "Green info icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun GalleryIcon(modifier: Modifier = Modifier.size(24.dp)) {
    BisqResourceIcon(Res.drawable.icon_gallery, "Gallery icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun PasteIcon(modifier: Modifier = Modifier) {
    BisqResourceIcon(
        resource = Res.drawable.icon_paste,
        contentDescription = "Paste icon",
        modifier = modifier,
    )
}

@ExcludeFromCoverage
@Composable
fun SwapHArrowIcon(modifier: Modifier = Modifier.size(16.dp)) {
    BisqResourceIcon(Res.drawable.exchange_h_arrow, "Swap horizontal icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun SwapVArrowIcon(modifier: Modifier = Modifier.size(16.dp)) {
    BisqResourceIcon(Res.drawable.exchange_v_arrow, "Swap vertical icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun QuestionIcon(modifier: Modifier = Modifier) {
    BisqResourceIcon(Res.drawable.icon_question_mark, "Question icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun ReplyIcon(modifier: Modifier = Modifier.size(24.dp)) {
    BisqResourceIcon(Res.drawable.icon_reply, "Reply icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun ScanIcon(modifier: Modifier = Modifier) {
    BisqResourceIcon(Res.drawable.icon_qr, "Scan icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun ScanQrIcon(modifier: Modifier = Modifier) {
    BisqResourceIcon(
        resource = Res.drawable.icon_scan_qr,
        contentDescription = "Scan icon",
        modifier = modifier,
    )
}

@ExcludeFromCoverage
@Composable
fun SendIcon(modifier: Modifier = Modifier.size(24.dp)) {
    BisqResourceIcon(Res.drawable.icon_send, "Send icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun SearchIcon(modifier: Modifier = Modifier.size(16.dp)) {
    BisqResourceIcon(Res.drawable.icon_search_dimmed, "Search icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun SortIcon(modifier: Modifier = Modifier) {
    BisqResourceIcon(Res.drawable.icon_sort, "Sort icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun GreenSortIcon(modifier: Modifier = Modifier) {
    BisqResourceIcon(Res.drawable.icon_sort_green, "Sort icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun StarEmptyIcon(modifier: Modifier = Modifier.size(16.dp)) {
    // TODO: Import right resource for this
    BisqResourceIcon(Res.drawable.icon_star_grey_hollow, "Empty star icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun StarHalfFilledIcon(modifier: Modifier = Modifier.size(16.dp)) {
    BisqResourceIcon(Res.drawable.icon_star_half_green, "Half filled star icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun StarFillIcon(modifier: Modifier = Modifier.size(16.dp)) {
    BisqResourceIcon(Res.drawable.icon_star_green, "Filled star icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun UpIcon(modifier: Modifier = Modifier.size(30.dp)) {
    BisqResourceIcon(Res.drawable.up_arrow, "Up icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun WarningIcon(modifier: Modifier = Modifier.size(24.dp)) {
    BisqResourceIcon(Res.drawable.icon_warning, "Warning icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun WarningIconFilled(modifier: Modifier = Modifier.size(24.dp)) {
    BisqResourceIcon(Res.drawable.icon_warning_filled, "Filled Warning icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun WarningIconLightGrey(modifier: Modifier = Modifier.size(24.dp)) {
    BisqResourceIcon(Res.drawable.icon_warning_light_grey, "Warning icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun WarningIconGrey(modifier: Modifier = Modifier.size(24.dp)) {
    BisqResourceIcon(Res.drawable.icon_warning_grey, "Warning icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun WarningIconWhite(modifier: Modifier = Modifier.size(24.dp)) {
    BisqResourceIcon(Res.drawable.icon_warning_white, "Warning icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun LeaveChatIcon(modifier: Modifier = Modifier.size(24.dp)) {
    BisqResourceIcon(Res.drawable.leave_chat_green, "Leave chat icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun AppLinkIcon(modifier: Modifier = Modifier.size(24.dp)) {
    BisqResourceIcon(Res.drawable.icon_app_link, "App link icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun WebLinkIcon(modifier: Modifier = Modifier.size(24.dp)) {
    BisqResourceIcon(Res.drawable.icon_web_link, "Web link icon", modifier)
}

@ExcludeFromCoverage
@Composable
fun DeliveryStatusConnecting(
    modifier: Modifier = Modifier.size(14.dp),
    colorFilter: ColorFilter? = null,
) {
    BisqResourceIcon(
        resource = Res.drawable.delivery_status_connecting,
        contentDescription = "Connecting",
        modifier = modifier,
        colorFilter = colorFilter,
    )
}

@ExcludeFromCoverage
@Composable
fun DeliveryStatusSent(
    modifier: Modifier = Modifier.size(12.dp),
    colorFilter: ColorFilter? = null,
) {
    BisqResourceIcon(
        resource = Res.drawable.delivery_status_sent,
        contentDescription = "Sent",
        modifier = modifier,
        colorFilter = colorFilter,
    )
}

@ExcludeFromCoverage
@Composable
fun DeliveryStatusMailbox(
    modifier: Modifier = Modifier.size(12.dp),
    colorFilter: ColorFilter? = null,
) {
    BisqResourceIcon(
        resource = Res.drawable.delivery_status_mailbox,
        contentDescription = "Mailbox",
        modifier = modifier,
        colorFilter = colorFilter,
    )
}

@ExcludeFromCoverage
@Composable
fun DeliveryStatusReceived(
    modifier: Modifier = Modifier.size(12.dp),
    colorFilter: ColorFilter? = null,
) {
    BisqResourceIcon(
        resource = Res.drawable.delivery_status_received,
        contentDescription = "Received",
        modifier = modifier,
        colorFilter = colorFilter,
    )
}

@ExcludeFromCoverage
@Composable
fun DeliveryStatusUndelivered(
    modifier: Modifier = Modifier.size(12.dp),
    colorFilter: ColorFilter? = null,
) {
    BisqResourceIcon(
        resource = Res.drawable.delivery_status_undelivered,
        contentDescription = "Undelivered",
        modifier = modifier,
        colorFilter = colorFilter,
    )
}

@ExcludeFromCoverage
@Composable
fun DeliveryStatusTrySendingAgain(
    modifier: Modifier = Modifier.size(12.dp),
    colorFilter: ColorFilter? = null,
) {
    BisqResourceIcon(
        resource = Res.drawable.delivery_status_try_again,
        contentDescription = "Try again",
        modifier = modifier,
        colorFilter = colorFilter,
    )
}
