package network.bisq.mobile.presentation.settings.user_profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.ViewPresenter
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqSelect
import network.bisq.mobile.presentation.common.ui.components.atoms.SettingsTextField
import network.bisq.mobile.presentation.common.ui.components.atoms.button.BisqIconButton
import network.bisq.mobile.presentation.common.ui.components.atoms.button.CopyIconButton
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.AddIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScrollScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.common.ui.components.molecules.UserProfileIcon
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.WarningConfirmationDialog
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import org.koin.compose.koinInject

interface IUserProfilePresenter : ViewPresenter {
    val uiState: StateFlow<UserProfileUiState>

    fun onAction(action: UserProfileUiAction)

    suspend fun getUserProfileIcon(userProfile: UserProfileVO): PlatformImage
}

@Composable
fun UserProfileScreen() {
    val presenter: IUserProfilePresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val isInteractive by presenter.isInteractive.collectAsState()
    val uiState by presenter.uiState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.selectedUserProfile?.id) {
        scrollState.animateScrollTo(0)
    }

    BisqScrollScaffold(
        topBar = { TopBar("user.userProfile".i18n(), showUserAvatar = false) },
        horizontalAlignment = Alignment.Start,
        snackbarHostState = presenter.getSnackState(),
        isInteractive = isInteractive,
        shouldBlurBg = uiState.shouldBlurBg,
        scrollState = scrollState,
    ) {
        uiState.selectedUserProfile?.let { profile ->
            UserProfileScreenHeader(profile, presenter::getUserProfileIcon)

            BisqGap.V1()

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
            ) {
                BisqSelect(
                    label = "user.bondedRoles.userProfile.select".i18n(),
                    options = uiState.userProfiles,
                    optionKey = { it.id },
                    optionLabel = { it.nickName },
                    selectedKey = profile.id,
                    searchable = true,
                    onSelect = {
                        presenter.onAction(UserProfileUiAction.OnUserProfileSelected(it))
                    },
                    disabled = !isInteractive || uiState.userProfiles.isEmpty(),
                    modifier = Modifier.weight(1f),
                )

                Box(Modifier.padding(bottom = 2.dp)) {
                    // to align button with text field
                    BisqIconButton(
                        onClick = {
                            presenter.onAction(UserProfileUiAction.OnCreateProfilePressed)
                        },
                        disabled = !isInteractive,
                        modifier =
                            Modifier
                                .size(46.dp)
                                .background(
                                    BisqTheme.colors.primary,
                                    RoundedCornerShape(BisqUIConstants.BorderRadius),
                                ),
                    ) {
                        AddIcon()
                    }
                }
            }

            BisqGap.V1()

            SettingsTextField(
                label = "mobile.settings.userProfile.labels.nickname".i18n(),
                value = profile.nickName,
                editable = false,
            )

            BisqGap.V1()

            // Bot ID with copy functionality
            SettingsTextField(
                label = "user.userProfile.nymId".i18n(),
                value = profile.nym,
                editable = false,
                trailingIcon = { CopyIconButton(value = profile.nym) },
            )

            BisqGap.V1()

            // Profile ID with copy functionality
            SettingsTextField(
                label = "user.userProfile.profileId".i18n(),
                value = profile.id,
                editable = false,
                trailingIcon = { CopyIconButton(value = profile.id) },
            )

            BisqGap.V1()

            SettingsTextField(
                label = "user.profileCard.details.profileAge".i18n(),
                value = uiState.profileAge,
                editable = false,
            )

            BisqGap.V1()

            SettingsTextField(
                label = "user.userProfile.livenessState.description".i18n(),
                value = uiState.lastUserActivity,
                editable = false,
            )

            BisqGap.V1()

            // Reputation
            SettingsTextField(
                label = "user.userProfile.reputation".i18n(),
                value = uiState.reputation,
                editable = false,
            )

            BisqGap.V1()

            // Statement
            SettingsTextField(
                label = "user.userProfile.statement".i18n(),
                value = uiState.statementDraft,
                isTextArea = true,
                onValueChange = { newValue, isValid ->
                    presenter.onAction(
                        UserProfileUiAction.OnStatementChanged(
                            newValue,
                        ),
                    )
                },
            )

            BisqGap.V1()

            // Trade Terms
            SettingsTextField(
                label = "user.userProfile.terms".i18n(),
                value = uiState.termsDraft,
                isTextArea = true,
                onValueChange = { newValue, isValid ->
                    presenter.onAction(
                        UserProfileUiAction.OnTermsChanged(
                            newValue,
                        ),
                    )
                },
            )
            BisqGap.V1()
            UserProfileScreenFooter(
                onSavePress = { presenter.onAction(UserProfileUiAction.OnSavePressed(profile.id, uiState)) },
                onDeletePress = { presenter.onAction(UserProfileUiAction.OnDeletePressed(profile)) },
                isBusy = uiState.isBusy,
            )
        }
    }

    uiState.showDeleteConfirmationForProfile?.let { profile ->
        WarningConfirmationDialog(
            message = "mobile.settings.userProfile.deleteConfirmationDialog.message".i18n(profile.nickName),
            onConfirm = { presenter.onAction(UserProfileUiAction.OnDeleteConfirmed(profile)) },
            onDismiss = { presenter.onAction(UserProfileUiAction.OnDeleteConfirmationDismissed) },
        )
    }

    if (uiState.showDeleteErrorDialog) {
        WarningConfirmationDialog(
            message = "user.userProfile.deleteProfile.cannotDelete".i18n(),
            dismissButtonText = "",
            onDismiss = { presenter.onAction(UserProfileUiAction.OnDeleteErrorDialogDismissed) },
            onConfirm = { presenter.onAction(UserProfileUiAction.OnDeleteErrorDialogDismissed) },
        )
    }
}

@Composable
private fun UserProfileScreenHeader(
    profile: UserProfileVO,
    imageProvider: suspend (UserProfileVO) -> PlatformImage,
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        UserProfileIcon(
            profile,
            imageProvider,
            90.dp,
        )
    }
}

@Composable
private fun UserProfileScreenFooter(
    onSavePress: () -> Unit,
    onDeletePress: () -> Unit,
    isBusy: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
    ) {
        BisqButton(
            text = "mobile.settings.userProfile.labels.save".i18n(),
            onClick = onSavePress,
            isLoading = isBusy,
            modifier = Modifier.weight(1.0F),
            padding =
                PaddingValues(
                    horizontal = BisqUIConstants.ScreenPadding,
                    vertical = BisqUIConstants.ScreenPaddingHalf,
                ),
        )
        BisqButton(
            text = "mobile.action.delete".i18n(),
            onClick = onDeletePress,
            isLoading = isBusy,
            modifier = Modifier.weight(1.0F),
            padding =
                PaddingValues(
                    horizontal = BisqUIConstants.ScreenPadding,
                    vertical = BisqUIConstants.ScreenPaddingHalf,
                ),
            type = BisqButtonType.WarningOutline,
        )
    }
}
