package network.bisq.mobile.presentation.common.ui.components.molecules.dialog

import androidx.compose.material3.SnackbarDuration
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.base.SnackbarPosition
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType
import network.bisq.mobile.presentation.common.ui.utils.toClipEntry
import network.bisq.mobile.presentation.main.MainPresenter

class WebLinkConfirmationDialogPresenter(
    private val settingsServiceFacade: SettingsServiceFacade,
    private val mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {
    private val _uiState = MutableStateFlow(WebLinkConfirmationUiState())
    val uiState: StateFlow<WebLinkConfirmationUiState> = _uiState.asStateFlow()

    private var userOnConfirm: () -> Unit = {}
    private var userOnDismiss: () -> Unit = {}
    private var userOnError: () -> Unit = {}
    private var currentClipboard: Clipboard? = null
    private var activeLink: String = ""

    fun initialize(
        link: String,
        clipboard: Clipboard,
        onConfirm: () -> Unit,
        onDismiss: () -> Unit,
        onError: () -> Unit,
        forceConfirm: Boolean = false,
    ) {
        activeLink = link
        userOnConfirm = onConfirm
        userOnDismiss = onDismiss
        userOnError = onError
        currentClipboard = clipboard

        _uiState.value = WebLinkConfirmationUiState()

        val shouldShow =
            forceConfirm || settingsServiceFacade.showWebLinkConfirmation.value
        if (!shouldShow) {
            val shouldOpen = settingsServiceFacade.permitOpeningBrowser.value
            if (shouldOpen) {
                openUri(link, false)
            } else {
                copyToClipboard(link, false)
            }
            return
        }

        _uiState.update { it.copy(isDialogVisible = true) }
    }

    fun onAction(action: WebLinkConfirmationUiAction) {
        when (action) {
            is WebLinkConfirmationUiAction.OnDontShowAgainChange ->
                _uiState.update { it.copy(dontShowAgain = action.checked) }

            is WebLinkConfirmationUiAction.OnConfirm -> openUri(activeLink, true)

            is WebLinkConfirmationUiAction.OnDismiss ->
                if (!action.toCopy) {
                    userOnDismiss.invoke()
                } else {
                    copyToClipboard(activeLink, true)
                }
        }
    }

    private fun openUri(
        uri: String,
        persist: Boolean,
    ) {
        presenterScope.launch {
            if (persist) showLoading()
            try {
                if (persist) {
                    persistWebLinkDialogChoice(
                        permitOpeningBrowser = true,
                        dontShowAgain = _uiState.value.dontShowAgain,
                    )
                }
                // navigateToUrl returns false when already non-interactive (anti double-tap)
                // before attempting to open; snapshot before call to tell that apart from a real failure.
                val interactiveBeforeOpen = isInteractive.value
                if (!navigateToUrl(uri)) {
                    if (!interactiveBeforeOpen) {
                        return@launch
                    }
                    throw IllegalStateException("Failed to open URI")
                }
                userOnConfirm.invoke()
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (throwable: Throwable) {
                log.e(throwable) { "Failed to open URI from web link confirmation dialog" }
                userOnError.invoke()
                mainPresenter.showSnackbar("mobile.error.cannotOpenUrl".i18n(), SnackbarType.ERROR)
            } finally {
                if (persist) hideLoading()
            }
        }
    }

    private fun copyToClipboard(
        uri: String,
        persist: Boolean,
    ) {
        presenterScope.launch {
            if (persist) showLoading()
            try {
                currentClipboard?.setClipEntry(AnnotatedString(uri).toClipEntry())
                mainPresenter.showSnackbar("mobile.components.copyIconButton.copied".i18n())
                if (persist) {
                    persistWebLinkDialogChoice(
                        permitOpeningBrowser = false,
                        dontShowAgain = _uiState.value.dontShowAgain,
                    )
                }
                userOnDismiss.invoke()
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (throwable: Throwable) {
                log.e(throwable) { "Failed to copy URI from web link confirmation dialog" }
                userOnError.invoke()
                mainPresenter.showSnackbar("mobile.error.generic".i18n(), type = SnackbarType.ERROR)
            } finally {
                if (persist) hideLoading()
            }
        }
    }

    private fun showPersistFailureSnackbar() {
        mainPresenter.showSnackbar("mobile.error.generic".i18n(), type = SnackbarType.ERROR)
    }

    private suspend fun persistWebLinkDialogChoice(
        permitOpeningBrowser: Boolean,
        dontShowAgain: Boolean,
    ): Boolean {
        val permitResult = settingsServiceFacade.setPermitOpeningBrowser(permitOpeningBrowser)
        permitResult.onFailure { showPersistFailureSnackbar() }
        if (permitResult.isFailure) return false
        if (!dontShowAgain) return true

        val dontShowResult = settingsServiceFacade.setWebLinkDontShowAgain()
        dontShowResult.onFailure { showPersistFailureSnackbar() }
        return dontShowResult.isSuccess
    }
}
