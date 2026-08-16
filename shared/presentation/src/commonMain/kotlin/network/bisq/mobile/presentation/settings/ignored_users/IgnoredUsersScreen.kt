package network.bisq.mobile.presentation.settings.ignored_users

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.data.utils.createEmptyImage
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.ErrorState
import network.bisq.mobile.presentation.common.ui.components.LoadingState
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.debouncedClickable
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.WarningIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScaffold
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScrollLayout
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBarContent
import network.bisq.mobile.presentation.common.ui.components.molecules.UserProfileIcon
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import org.koin.compose.koinInject

@Composable
fun IgnoredUsersScreen() {
    val presenter: IgnoredUsersPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val uiState by presenter.uiState.collectAsState()

    IgnoredUsersContent(
        uiState = uiState,
        userProfileIconProvider = presenter.userProfileIconProvider,
        onAction = presenter::onAction,
        topBar = { TopBar("mobile.settings.ignoredUsers".i18n()) },
    )
}

@Composable
internal fun IgnoredUsersContent(
    uiState: IgnoredUsersUiState,
    userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage,
    onAction: (IgnoredUsersUiAction) -> Unit,
    topBar: @Composable () -> Unit = {},
) {
    BisqScaffold(topBar = topBar) { paddingValues ->
        when {
            uiState.isLoading -> LoadingState(paddingValues)

            uiState.isLoadFailed ->
                ErrorState(
                    message = "mobile.settings.ignoredUsers.loadFailed".i18n(),
                    paddingValues = paddingValues,
                    onRetry = { onAction(IgnoredUsersUiAction.OnRetryLoadClick) },
                )

            uiState.ignoredUsers.isEmpty() ->
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    BisqText.BaseRegular(
                        text = "mobile.settings.ignoredUsers.empty".i18n(),
                        color = BisqTheme.colors.mid_grey20,
                    )
                }

            else ->
                BisqScrollLayout(
                    scaffoldPadding = paddingValues,
                    verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
                ) {
                    uiState.ignoredUsers.forEach { userProfile ->
                        IgnoredUserItem(
                            userProfile = userProfile,
                            userProfileIconProvider = userProfileIconProvider,
                            onUnblock = { onAction(IgnoredUsersUiAction.OnUnblockClick(userProfile.id)) },
                            onOpenProfile = { onAction(IgnoredUsersUiAction.OnPeerProfileClick(userProfile.id)) },
                        )
                    }
                }
        }

        if (uiState.unblockUserId != null) {
            ConfirmationDialog(
                headline = "mobile.error.warning".i18n(),
                headlineColor = BisqTheme.colors.warning,
                headlineLeftIcon = { WarningIcon() },
                message = "mobile.chat.undoIgnoreUserWarn".i18n(),
                confirmButtonText = "user.profileCard.userActions.undoIgnore".i18n(),
                dismissButtonText = "action.cancel".i18n(),
                verticalButtonPlacement = true,
                confirmButtonLoading = !uiState.isUnblockConfirmEnabled,
                onConfirm = { onAction(IgnoredUsersUiAction.OnConfirmUnblock) },
                onDismiss = { onAction(IgnoredUsersUiAction.OnDismissUnblockDialog) },
            )
        }
    }
}

@Composable
private fun IgnoredUserItem(
    userProfile: UserProfileVO,
    userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage,
    onUnblock: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth().padding(
                horizontal = BisqUIConstants.ScreenPaddingHalf,
                vertical = BisqUIConstants.ScreenPaddingHalf,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar + name open the peer's profile; the Unblock button stays outside the tap target.
        Row(
            modifier = Modifier.weight(1f).debouncedClickable(role = Role.Button, onClick = onOpenProfile),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserProfileIcon(userProfile, userProfileIconProvider, 40.dp)
            BisqGap.HHalf()

            BisqText.BaseRegular(
                text = userProfile.userName,
                modifier = Modifier.weight(1f),
            )
        }

        BisqGap.H1()

        BisqButton(
            text = "mobile.settings.ignoredUsers.unblock".i18n(),
            type = BisqButtonType.GreyOutline,
            onClick = onUnblock,
        )
    }
}

@ExcludeFromCoverage
@Composable
private fun IgnoredUsersPreviewTopBar() {
    TopBarContent(
        title = "mobile.settings.ignoredUsers".i18n(),
        showBackButton = true,
        showUserAvatar = false,
    )
}

@ExcludeFromCoverage
@Composable
private fun IgnoredUsersPreview(uiState: IgnoredUsersUiState) {
    BisqTheme.Preview {
        IgnoredUsersContent(
            uiState = uiState,
            userProfileIconProvider = { createEmptyImage() },
            onAction = {},
            topBar = { IgnoredUsersPreviewTopBar() },
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Ignored peers — list")
@Composable
private fun IgnoredUsersScreenListPreview() =
    IgnoredUsersPreview(
        IgnoredUsersUiState(
            ignoredUsers = listOf(createMockUserProfile("Satoshi"), createMockUserProfile("Hal")),
            isLoading = false,
        ),
    )

@ExcludeFromCoverage
@Preview(name = "Ignored peers — empty")
@Composable
private fun IgnoredUsersScreenEmptyPreview() = IgnoredUsersPreview(IgnoredUsersUiState(isLoading = false))

@ExcludeFromCoverage
@Preview(name = "Ignored peers — loading")
@Composable
private fun IgnoredUsersScreenLoadingPreview() = IgnoredUsersPreview(IgnoredUsersUiState(isLoading = true))

@ExcludeFromCoverage
@Preview(name = "Ignored peers — load failed")
@Composable
private fun IgnoredUsersScreenLoadFailedPreview() = IgnoredUsersPreview(IgnoredUsersUiState(isLoading = false, isLoadFailed = true))
