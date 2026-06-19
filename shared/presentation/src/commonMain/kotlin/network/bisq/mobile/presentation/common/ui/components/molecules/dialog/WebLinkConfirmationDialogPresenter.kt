package network.bisq.mobile.presentation.common.ui.components.molecules.dialog

import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType
import network.bisq.mobile.presentation.common.ui.utils.toClipEntry
import network.bisq.mobile.presentation.main.MainPresenter

class WebLinkConfirmationDialogPresenter(
    private val settingsServiceFacade: SettingsServiceFacade,
    private val mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {
    private val _uiState = MutableStateFlow(WebLinkConfirmationUiState())
    val uiState: StateFlow<WebLinkConfirmationUiState> = _uiState.asStateFlow()

    private val _isOpenUriEnabled = MutableStateFlow(true)
    val isOpenUriEnabled: StateFlow<Boolean> = _isOpenUriEnabled.asStateFlow()

    private val _isCopyToClipboardEnabled = MutableStateFlow(true)
    val isCopyToClipboardEnabled: StateFlow<Boolean> = _isCopyToClipboardEnabled.asStateFlow()

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
        _isOpenUriEnabled.value = true
        _isCopyToClipboardEnabled.value = true

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
        guardedSuspendAction(
            _isOpenUriEnabled,
            "openUri",
            showLoadingOverlay = persist,
            reEnableGuardOnComplete = false,
        ) {
            try {
                if (persist) {
                    persistWebLinkDialogChoice(
                        permitOpeningBrowser = true,
                        dontShowAgain = _uiState.value.dontShowAgain,
                    )
                }
                if (!navigateToUrlAwait(uri)) {
                    userOnError.invoke()
                    _isOpenUriEnabled.value = true
                } else {
                    userOnConfirm.invoke()
                }
            } catch (cancellationException: CancellationException) {
                _isOpenUriEnabled.value = true
                throw cancellationException
            } catch (throwable: Throwable) {
                log.e(throwable) { "Failed to open URI from web link confirmation dialog" }
                userOnError.invoke()
                mainPresenter.showSnackbar("mobile.error.cannotOpenUrl".i18n(), SnackbarType.ERROR)
                _isOpenUriEnabled.value = true
            }
        }
    }

    private fun copyToClipboard(
        uri: String,
        persist: Boolean,
    ) {
        guardedSuspendAction(
            _isCopyToClipboardEnabled,
            "copyToClipboard",
            showLoadingOverlay = persist,
            reEnableGuardOnComplete = false,
        ) {
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
                _isCopyToClipboardEnabled.value = true
                throw cancellationException
            } catch (throwable: Throwable) {
                log.e(throwable) { "Failed to copy URI from web link confirmation dialog" }
                userOnError.invoke()
                mainPresenter.showSnackbar("mobile.error.generic".i18n(), SnackbarType.ERROR)
                _isCopyToClipboardEnabled.value = true
            }
        }
    }

    private fun showPersistFailureSnackbar() {
        mainPresenter.showSnackbar("mobile.error.generic".i18n(), SnackbarType.ERROR)
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
