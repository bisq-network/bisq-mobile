package network.bisq.mobile.presentation.settings.user_profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
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

    val scrollThreshold = 150f
    val transitionProgress = (scrollState.value / scrollThreshold).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = transitionProgress,
        animationSpec = tween(durationMillis = 200),
        label = "profileTransition",
    )

    BisqScrollScaffold(
        topBar = {
            Column {
                TopBar("user.userProfile".i18n(), showUserAvatar = false)
                uiState.selectedUserProfile?.let { profile ->
                    AnimatedTopBarProfile(
                        profile = profile,
                        imageProvider = presenter::getUserProfileIcon,
                        transitionProgress = animatedProgress,
                    )
                }
            }
        },
        horizontalAlignment = Alignment.Start,
        snackbarHostState = presenter.getSnackState(),
        isInteractive = isInteractive,
        shouldBlurBg = uiState.shouldBlurBg,
        scrollState = scrollState,
    ) {
        uiState.selectedUserProfile?.let { profile ->
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
                        presenter.onAction(UserProfileUiAction.OnUserProfileSelect(it))
                    },
                    disabled = !isInteractive || uiState.userProfiles.isEmpty(),
                    modifier = Modifier.weight(1f),
                )

                Box(Modifier.padding(bottom = BisqUIConstants.userProfileIconButtonPadding)) {
                    // to align button with text field
                    BisqIconButton(
                        onClick = {
                            presenter.onAction(UserProfileUiAction.OnCreateProfilePress)
                        },
                        disabled = !isInteractive,
                        modifier =
                            Modifier
                                .size(BisqUIConstants.userProfileIconButtonSize)
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
                onValueChange = { newValue, _ ->
                    presenter.onAction(
                        UserProfileUiAction.OnStatementChange(
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
                onValueChange = { newValue, _ ->
                    presenter.onAction(
                        UserProfileUiAction.OnTermsChange(
                            newValue,
                        ),
                    )
                },
            )
            BisqGap.V1()
            UserProfileScreenFooter(
                onSavePress = { presenter.onAction(UserProfileUiAction.OnSavePress) },
                onDeletePress = { presenter.onAction(UserProfileUiAction.OnDeletePress) },
                isBusy = uiState.isBusy,
            )
        }
    }

    uiState.showDeleteConfirmationForProfile?.let { profile ->
        WarningConfirmationDialog(
            message = "mobile.settings.userProfile.deleteConfirmationDialog.message".i18n(profile.nickName),
            onConfirm = { presenter.onAction(UserProfileUiAction.OnDeleteConfirm) },
            onDismiss = { presenter.onAction(UserProfileUiAction.OnDeleteConfirmationDismiss) },
        )
    }

    if (uiState.showDeleteErrorDialog) {
        WarningConfirmationDialog(
            message = "user.userProfile.deleteProfile.cannotDelete".i18n(),
            dismissButtonText = "",
            onDismiss = { presenter.onAction(UserProfileUiAction.OnDeleteErrorDialogDismiss) },
            onConfirm = { presenter.onAction(UserProfileUiAction.OnDeleteErrorDialogDismiss) },
        )
    }
}

@Composable
private fun AnimatedTopBarProfile(
    profile: UserProfileVO,
    imageProvider: suspend (UserProfileVO) -> PlatformImage,
    transitionProgress: Float,
) {
    val iconSize by animateDpAsState(
        targetValue = if (transitionProgress < 0.5f) 80.dp else 50.dp,
        animationSpec = tween(durationMillis = 200),
        label = "iconSize",
    )

    val showNickname = transitionProgress > 0.6f

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = BisqUIConstants.ScreenPadding,
                    vertical = BisqUIConstants.ScreenPaddingHalf,
                ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserProfileIcon(
            profile,
            imageProvider,
            iconSize,
        )
        AnimatedVisibility(
            visible = showNickname,
            enter = fadeIn(animationSpec = tween(200)) + expandHorizontally(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200)) + shrinkHorizontally(animationSpec = tween(200)),
        ) {
            Row {
                BisqGap.H1()
                BisqText.H3Regular(
                    text = profile.nickName,
                    color = BisqTheme.colors.light_grey10,
                )
            }
        }
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
