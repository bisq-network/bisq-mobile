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
    private val _uiState =
        MutableStateFlow(
            BackupUiState(
                showBackupDialog = false,
                showWorkingDialog = false,
                showRestorePasswordDialogForUri = null,
                errorMessage = null,
            ),
        )

    val uiState = _uiState.asStateFlow()

    fun onAction(action: BackupUiAction) {
        when (action) {
            is BackupUiAction.ShowBackupDialog -> {
                _uiState.update {
                    it.copy(showBackupDialog = true)
                }
            }

            is BackupUiAction.DismissBackupDialog -> {
                _uiState.update {
                    it.copy(showBackupDialog = false)
                }
            }

            is BackupUiAction.OnBackupToFile -> {
                presenterScope.launch {
                    _uiState.update {
                        it.copy(
                            showWorkingDialog = true,
                        )
                    }
                    val t =
                        nodeBackupServiceFacade
                            .backupDataDir(
                                action.password,
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
                        showSnackbar("mobile.resources.restore.success".i18n(), isError = false)
                    }
                }
            }
        }
    }
}
