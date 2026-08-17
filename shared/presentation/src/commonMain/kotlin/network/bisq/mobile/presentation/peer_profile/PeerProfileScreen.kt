package network.bisq.mobile.presentation.peer_profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.data.utils.createEmptyImage
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.ErrorState
import network.bisq.mobile.presentation.common.ui.components.LoadingState
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.StarRating
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ChatIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ClosedEyeIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.EyeIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.FlagIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.WarningIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBarContent
import network.bisq.mobile.presentation.common.ui.components.molecules.UserProfileIcon
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.common.ui.i18n.i18nText
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycleBackStackAware
import network.bisq.mobile.presentation.report_user.ReportUserDialog

/**
 * Peer profile screen (issue #545) — see `PeerProfilePresenter` and the design reference in
 * `presentation/design/peer_profile/PeerProfileScreenDesign.kt`.
 *
 * The report dialog is passed in as a slot rather than rendered inside [PeerProfileScreenContent]:
 * `ReportUserDialog` injects its own presenter, which would make the content composable stateful and
 * break both previews and UI tests.
 */
@ExcludeFromCoverage
@Composable
fun PeerProfileScreen(profileId: String) {
    val presenter = RememberPresenterLifecycleBackStackAware<PeerProfilePresenter>()
    val uiState by presenter.uiState.collectAsState()
    val isIgnoreActionEnabled by presenter.isIgnoreActionEnabled.collectAsState()

    LaunchedEffect(presenter, profileId) {
        presenter.initialize(profileId)
    }

    PeerProfileScreenContent(
        uiState = uiState,
        userProfileIconProvider = presenter.userProfileIconProvider,
        onAction = presenter::onAction,
        isIgnoreActionEnabled = isIgnoreActionEnabled,
        topBar = {
            TopBar(
                title =
                    if (uiState.displayName.isEmpty()) {
                        "mobile.peerProfile.title".i18n()
                    } else {
                        i18nText("mobile.peerProfile.titleWithName", uiState.displayName)
                    },
                showUserAvatar = false,
            )
        },
        reportDialog = {
            val userProfile = uiState.userProfile
            if (uiState.showReportDialog && userProfile != null) {
                ReportUserDialog(
                    accusedUserProfile = userProfile,
                    reportMessage = uiState.reportDraft,
                    onReportFailure = { reportMessage ->
                        presenter.onAction(PeerProfileUiAction.OnReportFailure(reportMessage))
                    },
                    onReportSuccess = { presenter.onAction(PeerProfileUiAction.OnReportSuccess) },
                )
            }
        },
    )
}

@Composable
internal fun PeerProfileScreenContent(
    uiState: PeerProfileUiState,
    userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage,
    onAction: (PeerProfileUiAction) -> Unit,
    isIgnoreActionEnabled: Boolean = true,
    topBar: @Composable () -> Unit = {},
    reportDialog: @Composable () -> Unit = {},
) {
    BisqScaffold(topBar = topBar) { paddingValues ->
        when {
            uiState.isOwnProfile -> PeerProfileOwnProfileGuard(paddingValues)

            uiState.isNotFound ->
                ErrorState(
                    message = "mobile.peerProfile.notFound".i18n(),
                    paddingValues = paddingValues,
                )

            // Retry only on a failed lookup — a peer the network genuinely doesn't know will
            // never resolve, so offering to try again there would just loop the user.
            uiState.isLoadFailed ->
                ErrorState(
                    message = "mobile.peerProfile.loadFailed".i18n(),
                    paddingValues = paddingValues,
                    onRetry = { onAction(PeerProfileUiAction.OnRetryLoadClick) },
                )

            uiState.isLoading || uiState.userProfile == null -> LoadingState(paddingValues)

            else -> {
                Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    if (uiState.isIgnored) {
                        PeerProfileIgnoredBanner()
                    }
                    PeerProfileBody(
                        uiState = uiState,
                        userProfile = uiState.userProfile,
                        userProfileIconProvider = userProfileIconProvider,
                        isIgnoreActionEnabled = isIgnoreActionEnabled,
                        onAction = onAction,
                    )
                }
            }
        }

        if (uiState.showIgnoreConfirmDialog) {
            ConfirmationDialog(
                headline = "mobile.peerProfile.ignoreConfirm.headline".i18n(),
                headlineColor = BisqTheme.colors.warning,
                headlineLeftIcon = { WarningIcon() },
                message = "mobile.chat.ignoreUserWarn".i18n(),
                confirmButtonText = "chat.ignoreUser.confirm".i18n(),
                dismissButtonText = "action.cancel".i18n(),
                confirmButtonLoading = !isIgnoreActionEnabled,
                verticalButtonPlacement = true,
                onConfirm = { onAction(PeerProfileUiAction.OnConfirmIgnore) },
                onDismiss = { onAction(PeerProfileUiAction.OnDismissIgnoreDialog) },
            )
        }

        reportDialog()
    }
}

@Composable
private fun PeerProfileBody(
    uiState: PeerProfileUiState,
    userProfile: UserProfileVO,
    userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage,
    isIgnoreActionEnabled: Boolean,
    onAction: (PeerProfileUiAction) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(BisqUIConstants.ScreenPadding2X),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        UserProfileIcon(
            userProfile = userProfile,
            userProfileIconProvider = userProfileIconProvider,
            size = 72.dp,
        )
        BisqGap.V1()
        BisqText.H5Regular(
            text = uiState.displayName,
            color = BisqTheme.colors.white,
            textAlign = TextAlign.Center,
        )
        BisqGap.VHalf()
        // No stars when the score is unknown: an empty five-star row reads as a real rating of zero,
        // which is the misreading this branch exists to avoid.
        if (uiState.isReputationUnknown) {
            BisqText.BaseLightGrey(
                text = "mobile.peerProfile.reputationUnavailable".i18n(),
                textAlign = TextAlign.Center,
            )
        } else {
            StarRating(rating = uiState.starRating)
            BisqGap.VHalf()
            BisqText.BaseLightGrey(
                text = i18nText("mobile.peerProfile.reputation", uiState.reputationScore),
                textAlign = TextAlign.Center,
            )
        }

        BisqGap.V2()

        if (uiState.canSendPrivateMessage) {
            PeerProfileSendPrivateMessageButton(
                isLoading = uiState.isOpeningPrivateChat,
                onAction = onAction,
            )
            BisqGap.VHalf()
        }
        PeerProfileIgnoreButton(
            isIgnored = uiState.isIgnored,
            isEnabled = isIgnoreActionEnabled,
            onAction = onAction,
        )
        BisqGap.VHalf()
        PeerProfileReportButton(onAction = onAction)
    }
}

/**
 * The primary action on this screen, so it sits above ignore/report and uses the default (filled)
 * button style. Rendered only when [PeerProfileUiState.canSendPrivateMessage] — see that field for
 * why it is absent rather than disabled when unavailable.
 */
@Composable
private fun PeerProfileSendPrivateMessageButton(
    isLoading: Boolean,
    onAction: (PeerProfileUiAction) -> Unit,
) {
    BisqButton(
        text =
            if (isLoading) {
                "mobile.privateChats.openChat.loading".i18n()
            } else {
                "mobile.privateChats.openChat".i18n()
            },
        onClick = { onAction(PeerProfileUiAction.OnSendPrivateMessageClick) },
        disabled = isLoading,
        leftIcon = { ChatIcon(modifier = Modifier.size(18.dp)) },
        fullWidth = true,
    )
}

/**
 * Styled as a neutral outline rather than a danger button: ignoring is reversible and common enough
 * that it shouldn't read as scary. The warning colour is reserved for the confirmation dialog.
 */
@Composable
private fun PeerProfileIgnoreButton(
    isIgnored: Boolean,
    isEnabled: Boolean,
    onAction: (PeerProfileUiAction) -> Unit,
) {
    BisqButton(
        text =
            if (isIgnored) {
                "user.profileCard.userActions.undoIgnore".i18n()
            } else {
                "chat.message.contextMenu.ignoreUser".i18n()
            },
        onClick = {
            onAction(
                if (isIgnored) PeerProfileUiAction.OnUndoIgnoreClick else PeerProfileUiAction.OnIgnoreClick,
            )
        },
        type = BisqButtonType.GreyOutline,
        disabled = !isEnabled,
        leftIcon = {
            if (isIgnored) {
                EyeIcon(modifier = Modifier.size(18.dp))
            } else {
                ClosedEyeIcon(modifier = Modifier.size(18.dp))
            }
        },
        fullWidth = true,
    )
}

@Composable
private fun PeerProfileReportButton(onAction: (PeerProfileUiAction) -> Unit) {
    BisqButton(
        text = "chat.message.contextMenu.reportUser".i18n(),
        onClick = { onAction(PeerProfileUiAction.OnReportClick) },
        leftIcon = { FlagIcon(modifier = Modifier.size(18.dp)) },
        fullWidth = true,
    )
}

/**
 * States the fact only — undoing lives on [PeerProfileIgnoreButton], which flips to "Undo ignore"
 * whenever this banner is showing. A second Undo here would duplicate that action while being the
 * one copy not gated on `isIgnoreActionEnabled`, so it would stay tappable during an in-flight undo.
 */
@Composable
private fun PeerProfileIgnoredBanner() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(BisqTheme.colors.warning.copy(alpha = 0.12f))
                .padding(BisqUIConstants.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WarningIcon(modifier = Modifier.size(18.dp))
        BisqText.SmallRegular(
            text = "mobile.peerProfile.ignoredBanner".i18n(),
            color = BisqTheme.colors.warning,
        )
    }
}

@Composable
private fun PeerProfileOwnProfileGuard(paddingValues: PaddingValues) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(BisqUIConstants.ScreenPadding2X),
        contentAlignment = Alignment.Center,
    ) {
        BisqText.BaseLight(
            text = "mobile.peerProfile.ownProfileGuard".i18n(),
            color = BisqTheme.colors.mid_grey20,
            textAlign = TextAlign.Center,
        )
    }
}

// ============================================================================================
// Previews
// ============================================================================================

private val previewUserProfileIconProvider: suspend (UserProfileVO) -> PlatformImage = { createEmptyImage() }

/** Stateless stand-in for the screen's real [TopBar], which needs Koin. */
@ExcludeFromCoverage
@Composable
private fun PreviewTopBar(uiState: PeerProfileUiState) {
    TopBarContent(
        title =
            if (uiState.displayName.isEmpty()) {
                "mobile.peerProfile.title".i18n()
            } else {
                i18nText("mobile.peerProfile.titleWithName", uiState.displayName)
            },
        showBackButton = true,
        showUserAvatar = false,
    )
}

@ExcludeFromCoverage
private fun previewUiState(
    displayName: String = "SatoshiFan",
    starRating: Double = 4.5,
    reputationScore: Long = 12400,
    isIgnored: Boolean = false,
) = PeerProfileUiState(
    userProfile = createMockUserProfile(displayName),
    displayName = displayName,
    starRating = starRating,
    reputationScore = reputationScore,
    isIgnored = isIgnored,
    isLoading = false,
)

@ExcludeFromCoverage
@Composable
private fun PeerProfilePreview(uiState: PeerProfileUiState) {
    BisqTheme.Preview {
        PeerProfileScreenContent(
            uiState = uiState,
            userProfileIconProvider = previewUserProfileIconProvider,
            onAction = {},
            topBar = { PreviewTopBar(uiState) },
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun PeerProfileScreen_TrustedPeerPreview() = PeerProfilePreview(previewUiState())

@ExcludeFromCoverage
@Preview
@Composable
private fun PeerProfileScreen_NewPeerPreview() =
    PeerProfilePreview(
        previewUiState(
            displayName = "NewTrader",
            starRating = 0.0,
            reputationScore = 0,
        ),
    )

@ExcludeFromCoverage
@Preview
@Composable
private fun PeerProfileScreen_IgnoredPeerPreview() =
    PeerProfilePreview(
        previewUiState(displayName = "SuspiciousUser", starRating = 1.0, isIgnored = true),
    )

@ExcludeFromCoverage
@Preview
@Composable
private fun PeerProfileScreen_LoadingPreview() = PeerProfilePreview(PeerProfileUiState(isLoading = true))

@ExcludeFromCoverage
@Preview
@Composable
private fun PeerProfileScreen_NotFoundPreview() = PeerProfilePreview(PeerProfileUiState(isLoading = false, isNotFound = true))

@ExcludeFromCoverage
@Preview
@Composable
private fun PeerProfileScreen_LoadFailedPreview() = PeerProfilePreview(PeerProfileUiState(isLoading = false, isLoadFailed = true))

@ExcludeFromCoverage
@Preview
@Composable
private fun PeerProfileScreen_IgnoreConfirmDialogPreview() = PeerProfilePreview(previewUiState().copy(showIgnoreConfirmDialog = true))

@ExcludeFromCoverage
@Preview
@Composable
private fun PeerProfileScreen_OwnProfileGuardPreview() = PeerProfilePreview(previewUiState().copy(isOwnProfile = true))
