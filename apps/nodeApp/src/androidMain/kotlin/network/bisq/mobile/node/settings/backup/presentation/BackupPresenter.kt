package network.bisq.mobile.node.settings.backup.presentation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.node.settings.backup.domain.BackupPasswordPolicy
import network.bisq.mobile.node.settings.backup.domain.NodeBackupServiceFacade
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.main.MainPresenter

class BackupPresenter(
    private val mainPresenter: MainPresenter,
    private val nodeBackupServiceFacade: NodeBackupServiceFacade,
    // Release builds never produce a plaintext backup; debug builds may, after a warning.
    private val isPasswordRequired: Boolean,
) : BasePresenter(mainPresenter) {
    private val _uiState =
        MutableStateFlow(
            BackupUiState(
                showBackupDialog = false,
                showWorkingDialog = false,
                showRestorePasswordDialogForUri = null,
                errorMessage = null,
                passwordForm = emptyPasswordForm(),
                isPasswordRequired = isPasswordRequired,
            ),
        )

    val uiState = _uiState.asStateFlow()

    fun onAction(action: BackupUiAction) {
        when (action) {
            is BackupUiAction.ShowBackupDialog -> {
                _uiState.update {
                    it.copy(
                        showBackupDialog = true,
                        passwordForm = emptyPasswordForm(),
                        showNoPasswordConfirmation = false,
                    )
                }
            }

            is BackupUiAction.DismissBackupDialog -> {
                _uiState.update {
                    it.copy(
                        showBackupDialog = false,
                        passwordForm = emptyPasswordForm(),
                        showNoPasswordConfirmation = false,
                    )
                }
            }

            is BackupUiAction.OnBackupPasswordChange -> {
                updatePasswordForm(password = action.password)
            }

            is BackupUiAction.OnBackupPasswordConfirmationChange -> {
                updatePasswordForm(confirmation = action.confirmation)
            }

            is BackupUiAction.OnBackupClick -> {
                val form = _uiState.value.passwordForm
                if (!form.canSubmit) return
                if (form.isEncrypting) {
                    startBackup(form.password)
                } else if (!isPasswordRequired) {
                    _uiState.update { it.copy(showNoPasswordConfirmation = true) }
                }
            }

            is BackupUiAction.ConfirmBackupWithoutPassword -> {
                if (isPasswordRequired) return
                _uiState.update { it.copy(showNoPasswordConfirmation = false) }
                startBackup(null)
            }

            is BackupUiAction.DismissBackupWithoutPasswordConfirmation -> {
                _uiState.update { it.copy(showNoPasswordConfirmation = false) }
            }

            is BackupUiAction.OnRestoreFromFileActivityResult -> {
                action.uri?.let { selectedUri ->
                    presenterScope.launch {
                        val result = nodeBackupServiceFacade.restorePrefightCheck(selectedUri)
                        if (result.errorMessage != null) {
                            onAction(BackupUiAction.ShowError(result.errorMessage))
                        } else if (result.passwordRequired) {
                            onAction(BackupUiAction.ShowRestorePasswordDialog(selectedUri))
                        } else {
                            onAction(
                                BackupUiAction.OnStartRestore(
                                    selectedUri,
                                    null,
                                ),
                            )
                        }
                    }
                }
            }

            is BackupUiAction.ShowError -> {
                _uiState.update {
                    it.copy(errorMessage = action.message)
                }
            }

            is BackupUiAction.ClearError -> {
                _uiState.update {
                    it.copy(errorMessage = null)
                }
            }

            is BackupUiAction.ShowRestorePasswordDialog -> {
                _uiState.update {
                    it.copy(showRestorePasswordDialogForUri = action.uri)
                }
            }

            is BackupUiAction.DismissRestorePasswordDialog -> {
                _uiState.update {
                    it.copy(showRestorePasswordDialogForUri = null)
                }
            }

            is BackupUiAction.OnStartRestore -> {
                _uiState.update {
                    it.copy(
                        showRestorePasswordDialogForUri = null,
                        errorMessage = null,
                        showBackupDialog = false,
                        showWorkingDialog = true,
                    )
                }
                presenterScope.launch {
                    val t =
                        nodeBackupServiceFacade
                            .restoreBackup(
                                action.uri,
                                action.password,
                                mainPresenter.view,
                            ).await()
                    if (t != null) {
                        _uiState.update {
                            it.copy(
                                errorMessage = t.message ?: t.toString().take(20),
                                showWorkingDialog = false,
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(showWorkingDialog = false)
                        }
                        showSnackbar("mobile.resources.restore.success".i18n())
                    }
                }
            }
        }
    }

    private fun updatePasswordForm(
        password: String = _uiState.value.passwordForm.password,
        confirmation: String = _uiState.value.passwordForm.confirmation,
    ) {
        val isEmpty = password.isEmpty() && confirmation.isEmpty()
        val violation = BackupPasswordPolicy.findViolation(password)
        val validationError =
            when {
                isEmpty -> null
                violation != null -> violationMessage(violation)
                confirmation.isNotEmpty() && !isMatching(password, confirmation) -> "validation.password.notMatching".i18n()
                else -> null
            }
        val canSubmit =
            if (isEmpty) {
                !isPasswordRequired
            } else {
                violation == null && isMatching(password, confirmation)
            }
        _uiState.update {
            it.copy(
                passwordForm =
                    BackupPasswordFormState(
                        password = password,
                        confirmation = confirmation,
                        validationError = validationError,
                        canSubmit = canSubmit,
                    ),
            )
        }
    }

    private fun startBackup(password: String?) {
        presenterScope.launch {
            _uiState.update {
                it.copy(
                    showWorkingDialog = true,
                )
            }
            val t =
                nodeBackupServiceFacade
                    .backupDataDir(
                        password,
                    ).await()
            if (t != null) {
                _uiState.update {
                    it.copy(
                        errorMessage = t.message ?: t.toString().take(20),
                        showWorkingDialog = false,
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        showBackupDialog = false,
                        showWorkingDialog = false,
                        passwordForm = emptyPasswordForm(),
                    )
                }
                showSnackbar("mobile.resources.backup.success".i18n())
            }
        }
    }

    // Compared as they will be used, so a stray space in only one field is no mismatch.
    private fun isMatching(
        password: String,
        confirmation: String,
    ) = BackupPasswordPolicy.normalize(password) == BackupPasswordPolicy.normalize(confirmation)

    private fun emptyPasswordForm() = BackupPasswordFormState(canSubmit = !isPasswordRequired)

    private fun violationMessage(violation: BackupPasswordPolicy.Violation): String =
        when (violation) {
            BackupPasswordPolicy.Violation.TOO_SHORT ->
                "mobile.resources.backup.password.tooShort".i18n(BackupPasswordPolicy.MIN_LENGTH)
            BackupPasswordPolicy.Violation.TOO_LONG ->
                "mobile.resources.backup.password.tooLong".i18n(BackupPasswordPolicy.MAX_LENGTH)
            BackupPasswordPolicy.Violation.TOO_FEW_DISTINCT_CHARACTERS ->
                "mobile.resources.backup.password.tooFewDistinctCharacters".i18n(BackupPasswordPolicy.MIN_DISTINCT_CHARACTERS)
        }
}
