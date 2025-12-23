package network.bisq.mobile.node.settings.backup.presentation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.node.settings.backup.domain.NodeBackupServiceFacade
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.main.MainPresenter

class BackupPresenter(
    private val mainPresenter: MainPresenter,
    private val nodeBackupServiceFacade: NodeBackupServiceFacade,
) : BasePresenter(mainPresenter) {
    private val _uiState = MutableStateFlow(
        BackupUiState(
            showBackupDialog = false,
            showWorkingDialog = false,
            showRestorePasswordDialogForUri = null,
            errorMessage = null,
        )
    )

    val uiState = _uiState.asStateFlow()

    fun onAction(action: BackupUiActions) {
        when (action) {
            is BackupUiActions.ShowBackupDialog -> {
                _uiState.update {
                    it.copy(showBackupDialog = true)
                }
            }

            is BackupUiActions.DismissBackupDialog -> {
                _uiState.update {
                    it.copy(showBackupDialog = false)
                }
            }

            is BackupUiActions.OnBackupToFile -> {
                presenterScope.launch {
                    _uiState.update {
                        it.copy(
                            showWorkingDialog = true,
                        )
                    }
                    val t = nodeBackupServiceFacade.backupDataDir(
                        action.password
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
                            )
                        }
                        showSnackbar("mobile.resources.backup.success".i18n(), isError = false)
                    }
                }
            }

            is BackupUiActions.OnRestoreFromFileActivityResult -> {
                action.uri?.let { selectedUri ->
                    presenterScope.launch {
                        val result = nodeBackupServiceFacade.restorePrefightCheck(selectedUri)
                        if (result.errorMessage != null) {
                            onAction(BackupUiActions.ShowError(result.errorMessage))
                        } else if (result.passwordRequired) {
                            onAction(BackupUiActions.ShowRestorePasswordDialog(selectedUri))
                        } else {
                            onAction(
                                BackupUiActions.OnStartRestore(
                                    selectedUri,
                                    null
                                )
                            )
                        }
                    }
                }
            }

            is BackupUiActions.ShowError -> {
                _uiState.update {
                    it.copy(errorMessage = action.message)
                }
            }

            is BackupUiActions.ClearError -> {
                _uiState.update {
                    it.copy(errorMessage = null)
                }
            }

            is BackupUiActions.ShowRestorePasswordDialog -> {
                _uiState.update {
                    it.copy(showRestorePasswordDialogForUri = action.uri)
                }
            }

            is BackupUiActions.DismissRestorePasswordDialog -> {
                _uiState.update {
                    it.copy(showRestorePasswordDialogForUri = null)
                }
            }

            is BackupUiActions.OnStartRestore -> {
                _uiState.update {
                    it.copy(
                        showRestorePasswordDialogForUri = null,
                        errorMessage = null,
                        showBackupDialog = false,
                        showWorkingDialog = true,
                    )
                }
                presenterScope.launch {
                    val t = nodeBackupServiceFacade.restoreBackup(
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
                        showSnackbar("mobile.resources.restore.success".i18n(), isError = false)
                    }
                }
            }
        }
    }
}