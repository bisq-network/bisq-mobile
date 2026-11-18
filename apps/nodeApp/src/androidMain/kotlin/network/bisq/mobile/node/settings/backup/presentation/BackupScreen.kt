package network.bisq.mobile.node.settings.backup.presentation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.node.settings.backup.domain.BackupPasswordPolicy
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqPasswordTextField
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ExclamationRedIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScrollScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.BisqDialog
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.WarningConfirmationDialog
import network.bisq.mobile.presentation.common.ui.security.SecureScreenEffect
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import org.koin.compose.koinInject

@Composable
fun BackupScreen() {
    val presenter: BackupPresenter = koinInject()
    RememberPresenterLifecycle(presenter)
    SecureScreenEffect()

    val uiState by presenter.uiState.collectAsState()

    BisqScrollScaffold(
        topBar = { TopBar("mobile.more.backupAndRestore".i18n(), showUserAvatar = false) },
        horizontalAlignment = Alignment.Start,
    ) {
        BisqText.SmallLight(
            text = "mobile.resources.backup.info".i18n(),
            color = BisqTheme.colors.mid_grey20,
            modifier =
                Modifier
                    .padding(
                        vertical = BisqUIConstants.ScreenPaddingHalf,
                        horizontal = BisqUIConstants.ScreenPadding2X,
                    ),
        )
        BisqButton(
            text = "mobile.resources.backup.button".i18n(),
            onClick = { presenter.onAction(BackupUiAction.ShowBackupDialog) },
            type = BisqButtonType.Outline,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = BisqUIConstants.ScreenPaddingHalf,
                        horizontal = BisqUIConstants.ScreenPadding2X,
                    ),
        )

        BisqGap.V2()

        BisqText.SmallLight(
            text = "mobile.resources.restore.info".i18n(),
            color = BisqTheme.colors.mid_grey20,
            modifier =
                Modifier
                    .padding(
                        vertical = BisqUIConstants.ScreenPaddingHalf,
                        horizontal = BisqUIConstants.ScreenPadding2X,
                    ),
        )

        RestoreBackupButton(presenter::onAction)

        if (uiState.showBackupDialog) {
            BackupPasswordDialog(
                form = uiState.passwordForm,
                isPasswordRequired = uiState.isPasswordRequired,
                showNoPasswordConfirmation = uiState.showNoPasswordConfirmation,
                onAction = presenter::onAction,
            )
        }

        uiState.errorMessage?.let { ErrorDialog(it) { presenter.onAction(BackupUiAction.ClearError) } }

        if (uiState.showWorkingDialog) {
            WorkingDialog()
        }

        uiState.showRestorePasswordDialogForUri?.let { uri ->
            RestorePasswordDialog(
                onPassword = { password ->
                    presenter.onAction(BackupUiAction.OnStartRestore(uri, password))
                },
                onDismissOverlay = {
                    presenter.onAction(BackupUiAction.DismissRestorePasswordDialog)
                },
            )
        }
    }
}

@Composable
private fun BackupPasswordDialog(
    form: BackupPasswordFormState,
    isPasswordRequired: Boolean,
    showNoPasswordConfirmation: Boolean,
    onAction: (BackupUiAction) -> Unit,
) {
    val encryptAndBackup =
        if (isPasswordRequired || form.isEncrypting) {
            "mobile.resources.backup.password.button.encryptAndBackup".i18n()
        } else {
            "mobile.resources.backup.password.button.backup".i18n()
        }

    BisqDialog(
        horizontalAlignment = Alignment.CenterHorizontally,
        marginTop = BisqUIConstants.ScreenPadding,
        onDismissRequest = { onAction(BackupUiAction.DismissBackupDialog) },
    ) {
        BisqText.H4Regular(
            "mobile.resources.backup.password.headline".i18n(),
            color = BisqTheme.colors.primary,
        )
        BisqGap.V2()
        BisqText.BaseLight(
            if (isPasswordRequired) {
                "mobile.resources.backup.password.info.required".i18n(BackupPasswordPolicy.MIN_LENGTH)
            } else {
                "mobile.resources.backup.password.info".i18n()
            },
        )
        BisqGap.V2()
        BisqPasswordTextField(
            value = form.password,
            label = "mobile.resources.backup.password".i18n(),
            onValueChange = { onAction(BackupUiAction.OnBackupPasswordChange(it)) },
            isError = form.validationError != null,
            bottomMessage = form.validationError,
        )
        BisqGap.V1()
        BisqPasswordTextField(
            value = form.confirmation,
            label = "mobile.resources.backup.password.confirm".i18n(),
            onValueChange = { onAction(BackupUiAction.OnBackupPasswordConfirmationChange(it)) },
            isError = form.validationError != null,
            bottomMessage = form.validationError,
        )
        BisqGap.V2()
        Column {
            BisqButton(
                text = encryptAndBackup,
                onClick = { onAction(BackupUiAction.OnBackupClick) },
                disabled = !form.canSubmit,
                fullWidth = true,
                modifier = Modifier.semantics { contentDescription = encryptAndBackup },
            )
            BisqGap.VHalf()
            BisqButton(
                text = "action.cancel".i18n(),
                type = BisqButtonType.Grey,
                onClick = { onAction(BackupUiAction.DismissBackupDialog) },
                fullWidth = true,
                modifier = Modifier.semantics { contentDescription = "action.cancel".i18n() },
            )
        }
    }

    if (showNoPasswordConfirmation) {
        WarningConfirmationDialog(
            headline = "popup.headline.warning".i18n(),
            message = "mobile.resources.backup.noPassword.confirmation".i18n(),
            confirmButtonText = "confirmation.yes".i18n(),
            dismissButtonText = "action.cancel".i18n(),
            verticalButtonPlacement = true,
            onConfirm = { onAction(BackupUiAction.ConfirmBackupWithoutPassword) },
            onDismiss = { onAction(BackupUiAction.DismissBackupWithoutPasswordConfirmation) },
        )
    }
}

@Composable
private fun RestorePasswordDialog(
    onPassword: (String) -> Unit,
    onDismissOverlay: () -> Unit,
) {
    var password: String by remember { mutableStateOf("") }

    BisqDialog(
        horizontalAlignment = Alignment.CenterHorizontally,
        marginTop = BisqUIConstants.ScreenPadding,
        onDismissRequest = { onDismissOverlay() },
    ) {
        BisqText.H4Regular("mobile.resources.restore.password.headline".i18n(), color = BisqTheme.colors.primary)
        BisqGap.V2()
        BisqText.BaseLight("mobile.resources.restore.password.info".i18n())
        BisqGap.V2()
        BisqPasswordTextField(
            value = password,
            label = "mobile.resources.restore.password".i18n(),
            onValueChange = { newValue -> password = newValue },
        )
        BisqGap.V2()
        Column {
            BisqButton(
                text = "mobile.resources.restore.password.button".i18n(),
                onClick = { onPassword(password) },
                disabled = password.isEmpty(),
                fullWidth = true,
                modifier = Modifier.semantics { contentDescription = "mobile.resources.restore.password.button".i18n() },
            )
            BisqGap.VHalf()
            BisqButton(
                text = "action.cancel".i18n(),
                type = BisqButtonType.Grey,
                onClick = { onDismissOverlay() },
                fullWidth = true,
                modifier = Modifier.semantics { contentDescription = "action.cancel".i18n() },
            )
        }
    }
}

@Composable
private fun WorkingDialog() {
    BisqDialog(
        horizontalAlignment = Alignment.CenterHorizontally,
        marginTop = BisqUIConstants.ScreenPadding,
        onDismissRequest = { /* non-dismissable while restoring */ },
    ) {
        CircularProgressIndicator()
    }
}

private class OpenDocumentWithPersist : ActivityResultContract<Array<String>, Uri?>() {
    override fun createIntent(
        context: Context,
        input: Array<String>,
    ): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = if (input.isNotEmpty()) input.first() else "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, input)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?,
    ): Uri? = if (resultCode == Activity.RESULT_OK) intent?.data else null
}

@Composable
private fun RestoreBackupButton(onAction: (BackupUiAction) -> Unit) {
    val launcher =
        rememberLauncherForActivityResult(
            contract = OpenDocumentWithPersist(),
            onResult = { onAction(BackupUiAction.OnRestoreFromFileActivityResult(it)) },
        )

    BisqButton(
        text = "mobile.resources.restore.button".i18n(),
        // Wildcard MIME type for maximum compatibility
        onClick = {
            launcher.launch(
                arrayOf(
                    "application/zip",
                    "application/octet-stream",
                    "*/*",
                ),
            )
        },
        type = BisqButtonType.Outline,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = BisqUIConstants.ScreenPaddingHalf,
                    horizontal = BisqUIConstants.ScreenPadding2X,
                ),
    )
}

@Composable
private fun ErrorDialog(
    errorMessage: String,
    onDismissRequest: () -> Unit = {},
) {
    BisqDialog(
        horizontalAlignment = Alignment.Start,
        onDismissRequest = onDismissRequest,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExclamationRedIcon()
            BisqGap.HQuarter()
            BisqText.H4Regular("mobile.genericError.headline".i18n())
        }

        BisqGap.V1()

        BisqText.BaseLight(errorMessage)
    }
}

@Preview
@Composable
private fun BackupPasswordDialogPreview() {
    BisqTheme.Preview {
        BackupPasswordDialog(
            form = BackupPasswordFormState(),
            isPasswordRequired = true,
            showNoPasswordConfirmation = false,
            onAction = {},
        )
    }
}
