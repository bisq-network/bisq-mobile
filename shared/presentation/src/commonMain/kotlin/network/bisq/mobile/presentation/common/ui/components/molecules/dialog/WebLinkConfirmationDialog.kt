package network.bisq.mobile.presentation.common.ui.components.molecules.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.launch
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqCheckbox
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.InfoGreenIcon
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import network.bisq.mobile.presentation.common.ui.utils.toClipEntry
import network.bisq.mobile.presentation.main.MainPresenter
import org.koin.compose.koinInject

@Composable
fun WebLinkConfirmationDialog(
    link: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    headline: String = "hyperlinks.openInBrowser.attention.headline".i18n(),
    headlineColor: Color = BisqTheme.colors.primary,
    headlineLeftIcon: (@Composable () -> Unit)? = { InfoGreenIcon() },
    message: String = "hyperlinks.openInBrowser.attention".i18n(link),
    confirmButtonText: String = "confirmation.yes".i18n(),
    dismissButtonText: String = "hyperlinks.openInBrowser.no".i18n(),
) {
    val inPreview = LocalInspectionMode.current
    val isTest = LocalIsTest.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val settingsServiceFacade: SettingsServiceFacade? = if (!inPreview && !isTest) koinInject() else null
    val mainPresenter: MainPresenter? = if (!inPreview && !isTest) koinInject() else null

    var showConfirmation by remember { mutableStateOf(true) }
    var ready by remember { mutableStateOf(false) }
    var dontShowAgain by remember { mutableStateOf(false) }
    var confirmLoading by remember { mutableStateOf(false) }
    var dismissLoading by remember { mutableStateOf(false) }

    LaunchedEffect(link) {
        val shouldShow = settingsServiceFacade?.shouldShowWebLinkConfirmation() ?: true
        if (!shouldShow) {
            val shouldOpen = settingsServiceFacade.shouldPermitOpeningBrowser.value
            if (shouldOpen) {
                uriHandler.openUri(link)
                onConfirm.invoke()
            } else {
                copyLinkWithUserFeedback(clipboard, link, mainPresenter)
                onDismiss.invoke()
            }
            return@LaunchedEffect
        }
        showConfirmation = true
        ready = true
    }

    if (!ready || !showConfirmation) {
        return
    }

    ConfirmationDialog(
        headline = headline,
        headlineColor = headlineColor,
        headlineLeftIcon = headlineLeftIcon,
        message = message,
        confirmButtonText = confirmButtonText,
        dismissButtonText = dismissButtonText,
        confirmButtonLoading = confirmLoading,
        dismissButtonLoading = dismissLoading,
        onConfirm = {
            scope.launch {
                confirmLoading = true
                settingsServiceFacade?.setPermitOpeningBrowser(true)?.onFailure {
                    showPersistFailureSnackbar(mainPresenter)
                    confirmLoading = false
                }
                if (dontShowAgain) {
                    settingsServiceFacade?.setWebLinkDontShowAgain()?.onFailure {
                        showPersistFailureSnackbar(mainPresenter)
                        confirmLoading = false
                    }
                }
                uriHandler.openUri(link)
                onConfirm.invoke()
                confirmLoading = false
            }
        },
        onDismiss = { toCopy ->
            if (toCopy) {
                scope.launch {
                    dismissLoading = true
                    settingsServiceFacade?.setPermitOpeningBrowser(false)?.onFailure {
                        showPersistFailureSnackbar(mainPresenter)
                        dismissLoading = false
                    }
                    if (dontShowAgain) {
                        settingsServiceFacade?.setWebLinkDontShowAgain()?.onFailure {
                            showPersistFailureSnackbar(mainPresenter)
                            dismissLoading = false
                        }
                    }
                    copyLinkWithUserFeedback(clipboard, link, mainPresenter)
                    onDismiss()
                    dismissLoading = false
                }
            } else {
                onDismiss()
            }
        },
        closeButton = true,
        horizontalAlignment = Alignment.Start,
        verticalButtonPlacement = true,
        extraContent = {
            BisqCheckbox(
                checked = dontShowAgain,
                label = "action.dontShowAgain".i18n(),
                disabled = confirmLoading || dismissLoading,
                onCheckedChange = { dontShowAgain = it },
            )
        },
    )
}

private fun showPersistFailureSnackbar(mainPresenter: MainPresenter?) {
    mainPresenter?.showSnackbar("mobile.error.generic".i18n())
}

private suspend fun copyLinkWithUserFeedback(
    clipboard: Clipboard,
    link: String,
    mainPresenter: MainPresenter?,
) {
    clipboard.setClipEntry(AnnotatedString(link).toClipEntry())
    mainPresenter?.showSnackbar("mobile.components.copyIconButton.copied".i18n())
}
