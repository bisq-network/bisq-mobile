package network.bisq.mobile.presentation.ui.uicases.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.utils.getLogger
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.BisqLinks
import network.bisq.mobile.presentation.ui.components.RestoreBackup
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.button.LinkButton
import network.bisq.mobile.presentation.ui.components.atoms.icons.AppLinkIcon
import network.bisq.mobile.presentation.ui.components.atoms.icons.WebLinkIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollScaffold
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.components.molecules.dialog.BisqDialog
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject


@Composable
fun ResourcesScreen() {
    val presenter: ResourcesPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val isInteractive by presenter.isInteractive.collectAsState()
    val versionInfo by presenter.versionInfo.collectAsState()
    val deviceInfo by presenter.deviceInfo.collectAsState()
    val showBackupAndRestore by presenter.showBackupAndRestore.collectAsState()
    val showBackupOverlay by presenter.showBackupOverlay.collectAsState()
    val dividerModifier = Modifier.padding(top = BisqUIConstants.ScreenPaddingHalf, bottom = BisqUIConstants.ScreenPadding)

    BisqScrollScaffold(
        topBar = { TopBar("mobile.more.resources".i18n(), showUserAvatar = false) },
        horizontalAlignment = Alignment.Start,
        isInteractive = isInteractive,
    ) {
        BisqText.h3Light("support.resources.guides.headline".i18n(), color = BisqTheme.colors.light_grey50)
        AppLinkButton(
            "support.resources.guides.tradeGuide".i18n(),
            onClick = { presenter.onOpenTradeGuide() }
        )
        AppLinkButton(
            "support.resources.guides.chatRules".i18n(),
            onClick = { presenter.onOpenChatRules() }
        )
        AppLinkButton(
            "support.resources.guides.walletGuide".i18n(),
            onClick = { presenter.onOpenWalletGuide() }
        )

        if (showBackupAndRestore) {
            BisqHDivider(modifier = dividerModifier)
            BisqGap.V1()
            BisqText.h3Light("mobile.resources.backupAndRestore.headline".i18n(), color = BisqTheme.colors.light_grey50)
            BisqText.smallLight(
                text = "mobile.resources.backup.info".i18n(),
                color = BisqTheme.colors.mid_grey20,
                modifier = Modifier
                    .padding(vertical = BisqUIConstants.ScreenPaddingHalf, horizontal = BisqUIConstants.ScreenPadding2X)
            )
            BisqButton(
                text = "mobile.resources.backup.button".i18n(),
                onClick = { presenter.onBackupDataDir() },
                type = BisqButtonType.Outline,
                modifier = Modifier.fillMaxWidth()
                    .padding(vertical = BisqUIConstants.ScreenPaddingHalf, horizontal = BisqUIConstants.ScreenPadding2X)
            )

            BisqGap.V1()
            BisqText.smallLight(
                text = "mobile.resources.restore.info".i18n(),
                color = BisqTheme.colors.mid_grey20,
                modifier = Modifier
                    .padding(vertical = BisqUIConstants.ScreenPaddingHalf, horizontal = BisqUIConstants.ScreenPadding2X)
            )
            RestoreBackup(
                onRestoreBackup = presenter::onRestoreDataDir
            )
        }

        BisqHDivider(modifier = dividerModifier)
        BisqGap.V1()
        BisqText.h3Light("support.resources.resources.headline".i18n(), color = BisqTheme.colors.light_grey50)
        ResourceWeblink(
            "support.resources.resources.webpage".i18n(),
            link = BisqLinks.WEBPAGE,
            onClick = { presenter.onOpenWebUrl(BisqLinks.WEBPAGE) }
        )
        ResourceWeblink(
            "support.resources.resources.dao".i18n(),
            link = BisqLinks.DAO,
            onClick = { presenter.onOpenWebUrl(BisqLinks.DAO) }
        )
        ResourceWeblink(
            "support.resources.resources.sourceCode".i18n(),
            link = BisqLinks.BISQ_MOBILE_GH,
            onClick = { presenter.onOpenWebUrl(BisqLinks.BISQ_MOBILE_GH) }
        )
        ResourceWeblink(
            "support.resources.resources.community".i18n(),
            link = BisqLinks.MATRIX,
            onClick = { presenter.onOpenWebUrl(BisqLinks.MATRIX) }
        )

        BisqHDivider(modifier = dividerModifier)
        BisqGap.V1()
        BisqText.h3Light("mobile.resources.version.headline".i18n(), color = BisqTheme.colors.light_grey50)
        BisqText.baseLight(
            text = versionInfo,
            color = BisqTheme.colors.mid_grey20,
            modifier = Modifier
                .padding(vertical = BisqUIConstants.ScreenPaddingHalf, horizontal = BisqUIConstants.ScreenPadding2X)
        )

        BisqHDivider(modifier = dividerModifier)
        BisqGap.V1()
        BisqText.h3Light("mobile.resources.deviceInfo.headline".i18n(), color = BisqTheme.colors.light_grey50)
        BisqText.baseLight(
            text = deviceInfo,
            color = BisqTheme.colors.mid_grey20,
            modifier = Modifier
                .padding(vertical = BisqUIConstants.ScreenPaddingHalf, horizontal = BisqUIConstants.ScreenPadding2X)
        )

        BisqHDivider(modifier = dividerModifier)
        BisqGap.V1()
        BisqText.h3Light("support.resources.legal.headline".i18n(), color = BisqTheme.colors.light_grey50)
        AppLinkButton(
            "support.resources.legal.tac".i18n(),
            onClick = { presenter.onOpenTac() }
        )
        ResourceWeblink(
            "support.resources.legal.license".i18n(),
            link = BisqLinks.LICENSE,
            onClick = { presenter.onOpenWebUrl(BisqLinks.LICENSE) }
        )

        if (showBackupOverlay) {
            BackupPasswordOverlay(
                onBackupDataDir = { password -> presenter.onBackupDataDir(password) },
                onDismissBackupOverlay = presenter::onDismissBackupOverlay,
            )
        }
    }
}

@Composable
fun BackupPasswordOverlay(
    onBackupDataDir: (String?) -> Unit,
    onDismissBackupOverlay: () -> Unit,
) {
    var password: String by remember { mutableStateOf("") }
    var confirmedPassword: String by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var arePasswordsValidOrEmpty by remember { mutableStateOf(true) }
    var showNoPasswordConfirm by remember { mutableStateOf(false) }

    val encryptAndBackup by remember {
        derivedStateOf {
            if (password.isBlank())
                "mobile.resources.backup.password.button.backup".i18n()
            else
                "mobile.resources.backup.password.button.encryptAndBackup".i18n()
        }
    }
    LaunchedEffect(password, confirmedPassword) {
        validationError = when {
            password.isBlank() && confirmedPassword.isBlank() -> null
            password.length < 8 -> "validation.password.tooShort".i18n()
            confirmedPassword.isNotBlank() && confirmedPassword != password -> "validation.password.notMatching".i18n()
            else -> null
        }
        arePasswordsValidOrEmpty =
            password.isBlank() && confirmedPassword.isBlank() ||
                    (password.isNotBlank() && confirmedPassword == password && password.length >= 8)

    }

    BisqDialog(
        horizontalAlignment = Alignment.CenterHorizontally,
        marginTop = BisqUIConstants.ScreenPadding,
        onDismissRequest = { onDismissBackupOverlay() }
    ) {
        BisqText.h4Regular("mobile.resources.backup.password.headline".i18n(), color = BisqTheme.colors.primary)
        BisqGap.V2()
        BisqText.baseLight("mobile.resources.backup.password.info".i18n())
        BisqGap.V2()
        BisqTextField(
            value = password,
            label = "mobile.resources.backup.password".i18n(),
            onValueChange = { newValue, isValid ->
                password = newValue
            },
            isPasswordField = true,
            validation = { validationError }
        )
        BisqGap.V1()
        BisqTextField(
            value = confirmedPassword,
            label = "mobile.resources.backup.password.confirm".i18n(),
            onValueChange = { newValue, isValid ->
                confirmedPassword = newValue
            },

            isPasswordField = true,
            validation = { validationError }
        )
        BisqGap.V2()
        Column {
            BisqButton(
                text = encryptAndBackup,
                onClick = {
                    if (password.isBlank()) {
                        showNoPasswordConfirm = true
                    } else {
                        onBackupDataDir(password)
                    }
                },
                disabled = !arePasswordsValidOrEmpty,
                fullWidth = true,
                modifier = Modifier.semantics { contentDescription = encryptAndBackup },
            )
            BisqGap.VHalf()
            BisqButton(
                text = "action.cancel".i18n(),
                type = BisqButtonType.Grey,
                onClick = { onDismissBackupOverlay() },
                fullWidth = true,
                modifier = Modifier.semantics { contentDescription = "action.cancel".i18n() },
            )
        }
    }

    if (showNoPasswordConfirm) {
        network.bisq.mobile.presentation.ui.components.molecules.dialog.WarningConfirmationDialog(
            headline = "popup.headline.warning".i18n(),
            message = "mobile.resources.backup.noPassword.confirmation".i18n(),
            confirmButtonText = "confirmation.yes".i18n(),
            dismissButtonText = "action.cancel".i18n(),
            verticalButtonPlacement = true,
            onConfirm = {
                showNoPasswordConfirm = false
                onBackupDataDir("")
            },
            onDismiss = { showNoPasswordConfirm = false }
        )
    }
}

@Composable
fun ResourceWeblink(
    text: String,
    link: String,
    onClick: (() -> Unit)? = null,
) {
    LinkButton(
        text,
        link = link,
        onClick = onClick,
        leftIcon = { WebLinkIcon(modifier = Modifier.size(16.dp).alpha(0.5f)) },
        color = BisqTheme.colors.mid_grey20,
        padding = PaddingValues(
            horizontal = BisqUIConstants.ScreenPadding2X,
            vertical = BisqUIConstants.ScreenPaddingHalf
        ),
    )
}

@Composable
fun AppLinkButton(
    text: String,
    onClick: (() -> Unit)? = null,
) {
    BisqButton(
        text,
        leftIcon = { AppLinkIcon(modifier = Modifier.size(16.dp).alpha(0.4f)) },
        color = BisqTheme.colors.mid_grey20,
        type = BisqButtonType.Underline,
        onClick = { onClick?.invoke() },
    )
}