package network.bisq.mobile.presentation.common.ui.components.molecules.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqCheckbox
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.InfoGreenIcon
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import org.koin.compose.koinInject

@Composable
fun WebLinkConfirmationDialog(
    link: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onError: () -> Unit = {},
    forceConfirm: Boolean = false,
    headline: String? = null,
    headlineColor: Color? = null,
    headlineLeftIcon: (@Composable () -> Unit)? = null,
    message: String? = null,
    confirmButtonText: String? = null,
    dismissButtonText: String? = null,
) {
    val presenter: WebLinkConfirmationDialogPresenter = koinInject()
    val clipboard = LocalClipboard.current

    RememberPresenterLifecycle(presenter, {
        presenter.initialize(
            link = link,
            clipboard = clipboard,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            onError = onError,
            forceConfirm = forceConfirm,
        )
    })

    val uiState by presenter.uiState.collectAsState()

    if (!uiState.isDialogVisible) return

    WebLinkConfirmationDialogContent(
        link = link,
        uiState = uiState,
        onAction = presenter::onAction,
        showDontShowAgainCheckbox = !forceConfirm,
        headline = headline,
        headlineColor = headlineColor,
        headlineLeftIcon = headlineLeftIcon,
        message = message,
        confirmButtonText = confirmButtonText,
        dismissButtonText = dismissButtonText,
    )
}

@Composable
private fun WebLinkConfirmationDialogContent(
    link: String,
    uiState: WebLinkConfirmationUiState,
    onAction: (WebLinkConfirmationUiAction) -> Unit,
    showDontShowAgainCheckbox: Boolean = true,
    headline: String? = null,
    headlineColor: Color? = null,
    headlineLeftIcon: (@Composable () -> Unit)? = null,
    message: String? = null,
    confirmButtonText: String? = null,
    dismissButtonText: String? = null,
) {
    ConfirmationDialog(
        headline = headline ?: "hyperlinks.openInBrowser.attention.headline".i18n(),
        headlineColor = headlineColor ?: BisqTheme.colors.primary,
        headlineLeftIcon = headlineLeftIcon ?: { InfoGreenIcon() },
        message = message ?: "hyperlinks.openInBrowser.attention".i18n(link),
        confirmButtonText = confirmButtonText ?: "confirmation.yes".i18n(),
        dismissButtonText = dismissButtonText ?: "hyperlinks.openInBrowser.no".i18n(),
        onConfirm = { onAction(WebLinkConfirmationUiAction.OnConfirm) },
        onDismiss = { toCopy -> onAction(WebLinkConfirmationUiAction.OnDismiss(toCopy)) },
        closeButton = true,
        horizontalAlignment = Alignment.Start,
        verticalButtonPlacement = true,
        extraContent =
            if (showDontShowAgainCheckbox) {
                {
                    BisqCheckbox(
                        checked = uiState.dontShowAgain,
                        label = "action.dontShowAgain".i18n(),
                        onCheckedChange = { onAction(WebLinkConfirmationUiAction.OnDontShowAgainChange(it)) },
                    )
                }
            } else {
                null
            },
    )
}

@Preview
@Composable
private fun WebLinkConfirmationDialogContent_DefaultPreview() {
    BisqTheme.Preview {
        WebLinkConfirmationDialogContent(
            link = "https://bisq.network",
            uiState = WebLinkConfirmationUiState(isDialogVisible = true),
            onAction = {},
        )
    }
}
