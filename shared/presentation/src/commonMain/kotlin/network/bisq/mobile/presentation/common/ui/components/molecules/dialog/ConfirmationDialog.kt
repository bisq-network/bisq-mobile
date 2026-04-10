package network.bisq.mobile.presentation.common.ui.components.molecules.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqCheckbox
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.button.CloseIconButton
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.InfoGreenIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.WarningIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap.BisqGapHFill
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants

@Composable
fun ConfirmationDialog(
    onConfirm: () -> Unit,
    headline: String = "mobile.confirmation.areYouSure".i18n(),
    headlineColor: Color = BisqTheme.colors.white,
    headlineLeftIcon: (@Composable () -> Unit)? = null,
    message: String = "",
    confirmButtonText: String = "confirmation.yes".i18n(),
    dismissButtonText: String = "confirmation.no".i18n(),
    confirmButtonLoading: Boolean = false,
    dismissButtonLoading: Boolean = false,
    closeButton: Boolean = false,
    marginTop: Dp = BisqUIConstants.ScreenPadding8X,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    verticalButtonPlacement: Boolean = false,
    dismissOnClickOutside: Boolean = true,
    extraContent: (@Composable () -> Unit)? = null,
    onDismiss: (Boolean) -> Unit = {}, // true on dismiss button click; false on bg click dismiss
) {
    BisqDialog(
        dismissOnClickOutside = dismissOnClickOutside,
        horizontalAlignment = horizontalAlignment,
        marginTop = marginTop,
        onDismissRequest = { onDismiss(false) },
    ) {
        if (headline.isNotEmpty()) {
            if (headlineLeftIcon == null && !closeButton) {
                BisqText.H6Regular(headline, color = headlineColor)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    headlineLeftIcon?.invoke()
                    BisqGap.H1()
                    BisqText.H6Regular(headline, color = headlineColor)
                    BisqGapHFill()
                    if (closeButton) {
                        CloseIconButton(
                            disabled = confirmButtonLoading || dismissButtonLoading,
                            onClick = { onDismiss(false) },
                        )
                    }
                }
            }
            BisqGap.V2()
        }
        if (message.isNotEmpty()) {
            BisqText.BaseLight(message)
            BisqGap.V2()
        }
        extraContent?.invoke()
        if (extraContent != null) {
            BisqGap.V2()
        }
        if (verticalButtonPlacement) {
            Column {
                if (confirmButtonText.isNotBlank()) {
                    BisqButton(
                        text = confirmButtonText,
                        onClick = onConfirm,
                        fullWidth = true,
                        isLoading = confirmButtonLoading,
                        disabled = dismissButtonLoading,
                        modifier = Modifier.semantics { contentDescription = "dialog_confirm_yes" },
                    )
                }
                if (confirmButtonText.isNotBlank() && dismissButtonText.isNotBlank()) {
                    BisqGap.VHalf()
                }
                if (dismissButtonText.isNotBlank()) {
                    BisqButton(
                        text = dismissButtonText,
                        type = BisqButtonType.Grey,
                        onClick = { onDismiss(true) },
                        fullWidth = true,
                        isLoading = dismissButtonLoading,
                        disabled = confirmButtonLoading,
                        modifier = Modifier.semantics { contentDescription = "dialog_confirm_no" },
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
            ) {
                if (confirmButtonText.isNotBlank()) {
                    BisqButton(
                        modifier =
                            Modifier
                                .weight(1.0F)
                                .fillMaxHeight()
                                .semantics { contentDescription = "dialog_confirm_yes" },
                        text = confirmButtonText,
                        isLoading = confirmButtonLoading,
                        disabled = dismissButtonLoading,
                        onClick = onConfirm,
                    )
                }
                if (dismissButtonText.isNotBlank()) {
                    BisqButton(
                        modifier =
                            Modifier
                                .weight(1.0F)
                                .fillMaxHeight()
                                .semantics { contentDescription = "dialog_confirm_no" },
                        text = dismissButtonText,
                        type = BisqButtonType.Grey,
                        isLoading = dismissButtonLoading,
                        disabled = confirmButtonLoading,
                        onClick = { onDismiss(true) },
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun ConfirmationDialog_DefaultPreview() {
    BisqTheme.Preview {
        ConfirmationDialog(
            headline = "Are you absolutely sure?",
            message = "This action is irreversible and will permanently do the thing you're about to do. Please think twice.",
            confirmButtonText = "Yes, I'm Sure",
            dismissButtonText = "Cancel",
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun ConfirmationDialog_Default_single_buttonPreview() {
    BisqTheme.Preview {
        ConfirmationDialog(
            headline = "Unrecoverable error",
            headlineColor = BisqTheme.colors.warning,
            message = "You need to close and restart the app",
            confirmButtonText = "Close",
            dismissButtonText = "",
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun ConfirmationDialog_WarningPreview() {
    BisqTheme.Preview {
        ConfirmationDialog(
            headline = "mobile.error.warning".i18n(),
            headlineColor = BisqTheme.colors.warning,
            headlineLeftIcon = { WarningIcon() },
            message = "mobile.chat.ignoreUserWarn".i18n(),
            confirmButtonText = "chat.ignoreUser.confirm".i18n(),
            dismissButtonText = "action.cancel".i18n(),
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun ConfirmationDialog_VerticalButtonsPreview() {
    BisqTheme.Preview {
        ConfirmationDialog(
            headline = "Vertical Button Layout",
            message = "This dialog shows the buttons stacked vertically, which is useful for longer button text or narrower screens.",
            confirmButtonText = "Confirm This Action",
            dismissButtonText = "Go Back",
            verticalButtonPlacement = true, // Key change
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun ConfirmationDialog_ExtraContent_WebLinkPreview() {
    val link = "https://bisq.network/"
    BisqTheme.Preview {
        ConfirmationDialog(
            headline = "hyperlinks.openInBrowser.attention.headline".i18n(),
            headlineColor = BisqTheme.colors.primary,
            headlineLeftIcon = { InfoGreenIcon() },
            message = "hyperlinks.openInBrowser.attention".i18n(link),
            confirmButtonText = "confirmation.yes".i18n(),
            dismissButtonText = "hyperlinks.openInBrowser.no".i18n(),
            onConfirm = {},
            onDismiss = {},
            closeButton = true,
            horizontalAlignment = Alignment.Start,
            verticalButtonPlacement = true,
            extraContent = {
                BisqCheckbox(
                    checked = true,
                    label = "action.dontShowAgain".i18n(),
                    onCheckedChange = {},
                )
            },
        )
    }
}
