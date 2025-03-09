package network.bisq.mobile.presentation.ui.uicases.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.SettingsTextField
import network.bisq.mobile.presentation.ui.components.atoms.icons.UserIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollLayout
import network.bisq.mobile.presentation.ui.components.molecules.ConfirmationDialog
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
fun UserProfileSettingsScreen(showBackNavigation: Boolean = false) {
    val presenter: IUserProfileSettingsPresenter = koinInject()

    val botId = presenter.botId.collectAsState().value
    val nickname = presenter.nickname.collectAsState().value
    val profileId = presenter.profileId.collectAsState().value
    val profileAge = presenter.profileAge.collectAsState().value
    val lastUserActivity = presenter.lastUserActivity.collectAsState().value
    val reputation = presenter.reputation.collectAsState().value
    val statement = presenter.statement.collectAsState().value
    val tradeTerms = presenter.tradeTerms.collectAsState().value

    val showLoading = presenter.showLoading.collectAsState().value

    val showDeleteConfirmation = presenter.showDeleteProfileConfirmation.collectAsState().value

    RememberPresenterLifecycle(presenter)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Bot Icon
        UserProfileScreenHeader(presenter, showBackNavigation)

        BisqScrollLayout(
            onModifier = { modifier -> modifier.weight(1f) },
            isInteractive = presenter.isInteractive.collectAsState().value,
        ) {
            SettingsTextField(label = "Bot ID", value = botId, editable = false)

            BisqGap.V1()

            SettingsTextField(label = "Nickname", value = nickname, editable = false)

            BisqGap.V1()

            SettingsTextField(label = "Profile ID", value = profileId, editable = false)

            BisqGap.V1()

            SettingsTextField(label = "Profile age", value = profileAge, editable = false)

            BisqGap.V1()

            SettingsTextField(label = "Last user activity", value = lastUserActivity, editable = false)

            BisqGap.V1()

            // Reputation
            SettingsTextField(label = "Reputation", value = reputation, editable = false)

            BisqGap.V1()

            // Statement
            SettingsTextField(
                label = "Statement",
                value = statement,
                isTextArea = true,
                onValueChange = { newValue, isValid -> presenter.updateStatement(newValue) }
            )

            BisqGap.V1()

            // Trade Terms
            SettingsTextField(
                label = "Trade terms",
                value = tradeTerms,
                isTextArea = true,
                onValueChange = { newValue, isValid -> presenter.updateTradeTerms(newValue) }
            )
            BisqGap.V1()
            UserProfileScreenFooter(presenter, showLoading)
        }
    }

    if (showDeleteConfirmation) {
        ConfirmationDialog(
            message = "Are you sure want to delete your profile",
            onConfirm = presenter::onDelete,
            onDismiss = { presenter.setShowDeleteProfileConfirmation(false) }
        )
    }
}

@Composable
private fun UserProfileScreenHeader(presenter: IUserProfileSettingsPresenter, showBackNavigation: Boolean) {
    val connectivityStatus = presenter.connectivityStatus.collectAsState().value
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(BisqTheme.colors.backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        // Back Button if showBackNavigation is true
        if (showBackNavigation) {
            IconButton(
                onClick = { presenter.getRootNavController().popBackStack() },
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.CenterStart) // Align to the top-left
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = BisqTheme.colors.light1
                )
            }
        }

        Box(
            modifier = Modifier
                .size(80.dp)
                .padding(12.dp)
                .fillMaxWidth()
                .background(BisqTheme.colors.backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            UserIcon(
                presenter.uniqueAvatar.value,
                modifier = Modifier.size(72.dp),
                connectivityStatus = connectivityStatus
            )
        }
    }
}

@Composable
private fun UserProfileScreenFooter(presenter: IUserProfileSettingsPresenter, showLoading: Boolean) {
    val isLoading = presenter.showLoading.collectAsState().value

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
            "Save",
            onClick = presenter::onSave,
            isLoading = isLoading,
            modifier = Modifier.weight(1.0F),
            padding = PaddingValues(
                horizontal = BisqUIConstants.ScreenPadding,
                vertical = BisqUIConstants.ScreenPaddingHalf
            )
        )
    }
}