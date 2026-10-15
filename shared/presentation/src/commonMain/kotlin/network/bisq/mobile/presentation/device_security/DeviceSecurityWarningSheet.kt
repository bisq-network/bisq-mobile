package network.bisq.mobile.presentation.device_security

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.model.PlatformType
import network.bisq.mobile.domain.service.device_security.DeviceSecurityWording
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.common.ui.components.molecules.bottom_sheet.BisqBottomSheet
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants

/** Resolved copy for one combination of app, platform, wording and origin. */
data class DeviceSecurityCopy(
    val title: String,
    val changedLead: String?,
    val body: String,
    val gotIt: String,
    val dontShowAgain: String?,
    val suppressedNote: String?,
)

/**
 * Node and Connect differ in the body only, Android and iOS only in rooted / jailbroken. The node
 * app exists on Android only. From Settings, once the user has decided, the "Don't show again"
 * button is replaced by a note on how to undo the decision.
 */
fun deviceSecurityCopy(uiState: DeviceSecurityUiState): DeviceSecurityCopy {
    val isIos = uiState.platform == PlatformType.IOS
    val body =
        when {
            uiState.app == DeviceSecurityAppVariant.NODE -> "mobile.deviceSecurity.body.node.rooted"
            isIos -> "mobile.deviceSecurity.body.connect.jailbroken"
            else -> "mobile.deviceSecurity.body.connect.rooted"
        }.i18n()
    val decided = uiState.origin == DeviceSecurityOrigin.SETTINGS && uiState.isSuppressed
    return DeviceSecurityCopy(
        title = (if (isIos) "mobile.deviceSecurity.title.jailbroken" else "mobile.deviceSecurity.title.rooted").i18n(),
        changedLead =
            if (uiState.wording == DeviceSecurityWording.CHANGED_SINCE_SETUP) {
                "mobile.deviceSecurity.changedLead".i18n()
            } else {
                null
            },
        body = body,
        gotIt = "mobile.deviceSecurity.gotIt".i18n(),
        dontShowAgain = if (decided) null else "mobile.deviceSecurity.dontShowAgain".i18n(),
        suppressedNote = if (decided) "mobile.deviceSecurity.suppressedNote".i18n() else null,
    )
}

/** Swipe, scrim and back all route to [DeviceSecurityUiAction.OnDismiss], like "Got it". */
@Composable
fun DeviceSecurityWarningSheet(
    uiState: DeviceSecurityUiState,
    onAction: (DeviceSecurityUiAction) -> Unit,
) {
    if (!uiState.isSheetVisible) return
    BisqBottomSheet(onDismissRequest = { onAction(DeviceSecurityUiAction.OnDismiss) }) {
        DeviceSecurityWarningContent(
            copy = deviceSecurityCopy(uiState),
            onGotIt = { onAction(DeviceSecurityUiAction.OnDismiss) },
            onDontShowAgain = { onAction(DeviceSecurityUiAction.OnDontShowAgain) },
        )
    }
}

/**
 * The body scrolls and the buttons stay below it, so long translations and large font scales never
 * push the buttons off screen. The warning colour is used for the icon only, never for text.
 */
@Composable
fun DeviceSecurityWarningContent(
    copy: DeviceSecurityCopy,
    onGotIt: () -> Unit,
    onDontShowAgain: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = BisqUIConstants.ScreenPadding2X)
                .testTag("device_security_sheet"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The drag handle has no space below it, so the badge needs its own.
        BisqGap.V2()

        Box(
            modifier =
                Modifier
                    .size(BisqUIConstants.ScreenPadding4X)
                    .clip(CircleShape)
                    .background(BisqTheme.colors.warning.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = BisqTheme.colors.warning,
                // The triangle's visual mass sits below the middle of its box, so lift it slightly to
                // look centred inside the circle.
                modifier =
                    Modifier
                        .size(BisqUIConstants.ScreenPadding2X)
                        .offset(y = (-1).dp),
            )
        }

        BisqGap.V2()

        BisqText.H5Light(
            text = copy.title,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .semantics { heading() }
                    .testTag("device_security_title"),
        )

        BisqGap.V1()

        Column(
            modifier =
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .testTag("device_security_body"),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (copy.changedLead != null) {
                BisqText.BaseRegular(
                    text = copy.changedLead,
                    textAlign = TextAlign.Center,
                )
                BisqGap.V1()
            }
            BisqText.BaseLightGrey(
                text = copy.body,
                textAlign = TextAlign.Center,
            )
        }

        BisqGap.V3()

        BisqButton(
            text = copy.gotIt,
            onClick = onGotIt,
            fullWidth = true,
            modifier =
                Modifier
                    .heightIn(min = BisqUIConstants.ScreenPadding4X)
                    .testTag("device_security_got_it"),
        )

        if (copy.dontShowAgain != null) {
            BisqGap.V1()
            BisqButton(
                text = copy.dontShowAgain,
                onClick = onDontShowAgain,
                type = BisqButtonType.Clear,
                modifier =
                    Modifier
                        .heightIn(min = BisqUIConstants.ScreenPadding4X)
                        .testTag("device_security_dont_show_again"),
            )
        }

        if (copy.suppressedNote != null) {
            BisqGap.V1()
            BisqText.SmallLightGrey(
                text = copy.suppressedNote,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("device_security_suppressed_note"),
            )
        }

        BisqGap.V1()
    }
}

/**
 * Shown at the top of Settings while the device is detected as rooted, whatever the "Don't show
 * again" decision. A clean device gets no section: the heuristics are evadable, so the app never
 * claims a device is secure.
 *
 * A regular Settings section (H4 header, content, divider) holding one tappable line. The only
 * emphasis is the warning colour on the short status text and its glyph, so it reads as a hint
 * rather than a card competing with the settings below. The divider is emitted only while the
 * section is visible, so a clean device keeps the original Settings layout.
 */
@Composable
fun DeviceSecuritySettingsRow(
    uiState: DeviceSecurityUiState,
    onAction: (DeviceSecurityUiAction) -> Unit,
) {
    if (!uiState.isRowVisible) return
    val subtitle =
        if (uiState.platform == PlatformType.IOS) {
            "mobile.deviceSecurity.settings.subtitle.jailbroken"
        } else {
            "mobile.deviceSecurity.settings.subtitle.rooted"
        }.i18n()
    BisqText.H4Light("mobile.deviceSecurity.settings.label".i18n())
    BisqGap.V1()
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .clickable(role = Role.Button) { onAction(DeviceSecurityUiAction.OnOpenFromSettings) }
                .heightIn(min = BisqUIConstants.ScreenPadding4X)
                .testTag("device_security_settings_row"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = BisqTheme.colors.warning,
            modifier = Modifier.size(BisqUIConstants.ScreenPadding),
        )
        BisqGap.HHalf()
        BisqText.SmallMedium(
            text = subtitle,
            color = BisqTheme.colors.warning,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = BisqTheme.colors.mid_grey20,
        )
    }
    BisqHDivider()
}

@Preview
@Composable
private fun DeviceSecurityWarningContent_ConnectAndroidPreview() {
    BisqTheme.Preview {
        DeviceSecurityWarningContent(
            copy = deviceSecurityCopy(DeviceSecurityUiState(isSheetVisible = true)),
            onGotIt = {},
            onDontShowAgain = {},
        )
    }
}

@Preview
@Composable
private fun DeviceSecurityWarningContent_NodeChangedPreview() {
    BisqTheme.Preview {
        DeviceSecurityWarningContent(
            copy =
                deviceSecurityCopy(
                    DeviceSecurityUiState(
                        isSheetVisible = true,
                        app = DeviceSecurityAppVariant.NODE,
                        wording = DeviceSecurityWording.CHANGED_SINCE_SETUP,
                    ),
                ),
            onGotIt = {},
            onDontShowAgain = {},
        )
    }
}

@Preview
@Composable
private fun DeviceSecurityWarningContent_FromSettingsSuppressedPreview() {
    BisqTheme.Preview {
        DeviceSecurityWarningContent(
            copy =
                deviceSecurityCopy(
                    DeviceSecurityUiState(
                        isSheetVisible = true,
                        origin = DeviceSecurityOrigin.SETTINGS,
                        isSuppressed = true,
                    ),
                ),
            onGotIt = {},
            onDontShowAgain = {},
        )
    }
}

@Preview
@Composable
private fun DeviceSecuritySettingsRow_IosPreview() {
    BisqTheme.Preview {
        DeviceSecuritySettingsRow(
            uiState = DeviceSecurityUiState(isRowVisible = true, platform = PlatformType.IOS),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun DeviceSecuritySettingsRow_AboveLanguageHeaderPreview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding2X)) {
            DeviceSecuritySettingsRow(
                uiState = DeviceSecurityUiState(isRowVisible = true),
                onAction = {},
            )
            BisqText.H4Light("Language")
            BisqGap.V1()
            BisqText.BaseLightGrey("Language selection continues here...")
        }
    }
}
