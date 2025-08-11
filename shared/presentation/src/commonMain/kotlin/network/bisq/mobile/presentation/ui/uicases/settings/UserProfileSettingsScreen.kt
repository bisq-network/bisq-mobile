package network.bisq.mobile.presentation.ui.uicases.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.SettingsTextField
import network.bisq.mobile.presentation.ui.components.atoms.button.CopyIconButton
import network.bisq.mobile.presentation.ui.components.atoms.icons.UserIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollScaffold
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.ui.components.molecules.settings.BreadcrumbNavigation
import network.bisq.mobile.presentation.ui.components.molecules.settings.MenuItem
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

interface IUserProfileSettingsPresenter : ViewPresenter {

    val reputation: StateFlow<String>
    val lastUserActivity: StateFlow<String>
    val profileAge: StateFlow<String>
    val profileId: StateFlow<String>
    val nickname: StateFlow<String>
    val botId: StateFlow<String>
    val statement: StateFlow<String>
    val tradeTerms: StateFlow<String>

    val uniqueAvatar: StateFlow<PlatformImage?>

    val showLoading: StateFlow<Boolean>

    val connectivityStatus: StateFlow<ConnectivityService.ConnectivityStatus>

    fun onDelete()
    fun onSave()
    fun updateTradeTerms(it: String)
    fun updateStatement(it: String)

    val showDeleteProfileConfirmation: StateFlow<Boolean>
    fun setShowDeleteProfileConfirmation(value: Boolean)
}

@Composable
fun UserProfileSettingsScreen() {
    val presenter: IUserProfileSettingsPresenter = koinInject()
    val settingsPresenter: ISettingsPresenter = koinInject()
    
    val menuTree: MenuItem = settingsPresenter.menuTree()
    val menuPath = remember { mutableStateListOf(menuTree) }
    RememberPresenterLifecycle(presenter, {
        menuPath.add((menuTree as MenuItem.Parent).children[1])
    })

    val isInteractive by presenter.isInteractive.collectAsState()
    val botId by presenter.botId.collectAsState()
    val nickname by presenter.nickname.collectAsState()
    val profileId by presenter.profileId.collectAsState()
    val profileAge by presenter.profileAge.collectAsState()
    val lastUserActivity by presenter.lastUserActivity.collectAsState()
    val reputation by presenter.reputation.collectAsState()
    val statement by presenter.statement.collectAsState()
    val tradeTerms by presenter.tradeTerms.collectAsState()

    val showLoading by presenter.showLoading.collectAsState()
    val showDeleteConfirmation by presenter.showDeleteProfileConfirmation.collectAsState()

    BisqScrollScaffold(
        topBar = { TopBar("user.userProfile".i18n(), showUserAvatar = false) },
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
        snackbarHostState = presenter.getSnackState(),
        isInteractive = isInteractive,
        shouldBlurBg = showDeleteConfirmation,
    ) {
        BreadcrumbNavigation(path = menuPath) { index ->
            if (index == 0) settingsPresenter.settingsNavigateBack()
        }
        
        UserProfileScreenHeader(presenter)

        SettingsTextField(label = "mobile.settings.userProfile.labels.nickname".i18n(), value = nickname, editable = false)

        BisqGap.V1()

        // Bot ID with copy functionality
        SettingsTextField(
            label = "user.userProfile.nymId".i18n(),
            value = botId,
            editable = false,
                    trailingIcon = { CopyIconButton(value = botId) }
        )

        BisqGap.V1()

        // Profile ID with copy functionality
        SettingsTextField(
            label = "user.userProfile.profileId".i18n(),
            value = profileId,
            editable = false,
            trailingIcon = { CopyIconButton(value = profileId) }
        )

        BisqGap.V1()

        SettingsTextField(label = "user.profileCard.details.profileAge".i18n(), value = profileAge, editable = false)

        BisqGap.V1()

        SettingsTextField(label = "user.userProfile.livenessState.description".i18n(), value = lastUserActivity, editable = false)

        BisqGap.V1()

        // Reputation
        SettingsTextField(label = "user.userProfile.reputation".i18n(), value = reputation, editable = false)

        BisqGap.V1()

        // Statement
        SettingsTextField(
            label = "user.userProfile.statement".i18n(),
            value = statement,
            isTextArea = true,
            onValueChange = { newValue, isValid -> presenter.updateStatement(newValue) }
        )

        BisqGap.V1()

        // Trade Terms
        SettingsTextField(
            label = "user.userProfile.terms".i18n(),
            value = tradeTerms,
            isTextArea = true,
            onValueChange = { newValue, isValid -> presenter.updateTradeTerms(newValue) }
        )
        BisqGap.V1()
        UserProfileScreenFooter(presenter, showLoading)
    }

    if (showDeleteConfirmation) {
        ConfirmationDialog(
            headline = "mobile.settings.userProfile.deleteConfirmationDialog.headline".i18n(),
            onConfirm = presenter::onDelete,
            onDismiss = { presenter.setShowDeleteProfileConfirmation(false) }
        )
    }
}

@Composable
private fun UserProfileScreenHeader(presenter: IUserProfileSettingsPresenter) {
    val connectivityStatus by presenter.connectivityStatus.collectAsState()
    val uniqueAvatar by presenter.uniqueAvatar.collectAsState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(BisqTheme.colors.backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .padding(12.dp)
                .fillMaxWidth()
                .background(BisqTheme.colors.backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            UserIcon(
                uniqueAvatar,
                modifier = Modifier.size(72.dp),
                connectivityStatus = connectivityStatus
            )
        }
    }
}

@Composable
private fun UserProfileScreenFooter(presenter: IUserProfileSettingsPresenter, showLoading: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        // TODO uncomment when delete profile gets implemented
//        BisqButton(
//            "Delete profile",
//            onClick = { presenter.setShowDeleteProfileConfirmation(true) },
//            disabled = isLoading,
//            type = BisqButtonType.Danger,
//            modifier = Modifier.weight(1.0F),
//            padding = PaddingValues(
//                horizontal = BisqUIConstants.ScreenPadding,
//                vertical = BisqUIConstants.ScreenPaddingHalf
//            )
//        )
//        BisqGap.H1()
        BisqButton(
            "mobile.settings.userProfile.labels.save".i18n(),
            onClick = presenter::onSave,
            isLoading = showLoading,
            modifier = Modifier.weight(1.0F),
            padding = PaddingValues(
                horizontal = BisqUIConstants.ScreenPadding,
                vertical = BisqUIConstants.ScreenPaddingHalf
            )
        )
    }
}
