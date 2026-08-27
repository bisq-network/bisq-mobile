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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqTextFieldV0
import network.bisq.mobile.presentation.common.ui.components.atoms.StarRating
import network.bisq.mobile.presentation.common.ui.components.atoms.debouncedClickable
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ChatIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ClosedEyeIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.EyeIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.FlagIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.WarningIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.atoms.slider.BisqSlider
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBarContent
import network.bisq.mobile.presentation.common.ui.components.molecules.UserProfileIcon
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.BisqDialog
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.common.ui.i18n.i18nText
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycleBackStackAware
import network.bisq.mobile.presentation.community.contacts.ContactTagPill
import network.bisq.mobile.presentation.community.contacts.ContactTrustScoreIndicator
import network.bisq.mobile.presentation.report_user.ReportUserDialog
import kotlin.math.roundToInt

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
    val isContactActionEnabled by presenter.isContactActionEnabled.collectAsState()

    LaunchedEffect(presenter, profileId) {
        presenter.initialize(profileId)
    }

    PeerProfileScreenContent(
        uiState = uiState,
        userProfileIconProvider = presenter.userProfileIconProvider,
        onAction = presenter::onAction,
        isIgnoreActionEnabled = isIgnoreActionEnabled,
        isContactActionEnabled = isContactActionEnabled,
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
    isContactActionEnabled: Boolean = true,
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
                        isContactActionEnabled = isContactActionEnabled,
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

        val draft = uiState.contactDraft
        if (uiState.showEditContactDetailsDialog && draft != null) {
            EditContactDetailsDialog(draft = draft, onAction = onAction)
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
    isContactActionEnabled: Boolean,
    onAction: (PeerProfileUiAction) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(BisqUIConstants.ScreenPadding2X),
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
        if (uiState.showContactAction) {
            BisqGap.VHalf()
            PeerProfileContactButton(
                isContact = uiState.isContact,
                isEnabled = isContactActionEnabled,
                onAction = onAction,
            )
        }
        val contactDetails = uiState.contactDetails
        if (uiState.showContactAction && uiState.isContact && contactDetails != null) {
            BisqGap.V2()
            ContactDetailsSection(
                details = contactDetails,
                onEditClick = { onAction(PeerProfileUiAction.OnEditContactDetailsClick) },
            )
        }
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

/**
 * Add/remove this peer from My Contacts. Same neutral outline as the ignore button:
 * both are reversible relationship toggles. Rendered only while the Contacts feature is live
 * (see [PeerProfileUiState.showContactAction]).
 */
@Composable
private fun PeerProfileContactButton(
    isContact: Boolean,
    isEnabled: Boolean,
    onAction: (PeerProfileUiAction) -> Unit,
) {
    BisqButton(
        text =
            if (isContact) {
                "mobile.peerProfile.contacts.remove".i18n()
            } else {
                "mobile.peerProfile.contacts.add".i18n()
            },
        onClick = {
            onAction(
                if (isContact) PeerProfileUiAction.OnRemoveContactClick else PeerProfileUiAction.OnAddContactClick,
            )
        },
        type = BisqButtonType.GreyOutline,
        disabled = !isEnabled,
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
 * Private annotations about this contact (#1238): tag, first line of notes, and the user's own
 * trust score — deliberately placed BELOW the action stack, far from the network-wide star
 * reputation at the top, because the two must never read as the same signal. Muted card styling
 * marks it as "your notes", not app-native profile data. The whole card opens the edit dialog;
 * the trailing Edit label is the discoverability affordance.
 */
@Composable
private fun ContactDetailsSection(
    details: ContactDetailsUiState,
    onEditClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .background(BisqTheme.colors.dark_grey40)
                .debouncedClickable(onClick = onEditClick)
                .padding(BisqUIConstants.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BisqText.SmallRegularGrey(text = "mobile.peerProfile.contactDetails.title".i18n())
            BisqText.SmallMedium(text = "action.edit".i18n(), color = BisqTheme.colors.primary)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Same bounds as ContactCard: weight(fill = false) caps the tag at whatever is left
            // after the trust indicator's fixed footprint, so a long tag can never push it out.
            if (details.tag.isNotBlank()) {
                ContactTagPill(
                    tag = details.tag,
                    modifier = Modifier.weight(1f, fill = false).padding(end = BisqUIConstants.ScreenPaddingHalf),
                )
            } else {
                BisqText.SmallRegularGrey(
                    text = "mobile.peerProfile.contactDetails.noTag".i18n(),
                    modifier = Modifier.weight(1f, fill = false).padding(end = BisqUIConstants.ScreenPaddingHalf),
                )
            }
            ContactTrustScoreIndicator(trustScore = details.trustScore)
        }
        BisqText.StyledText(
            text =
                details.notes
                    .lineSequence()
                    .firstOrNull { it.isNotBlank() }
                    ?: "mobile.peerProfile.contactDetails.noNotes".i18n(),
            style = BisqTheme.typography.smallRegular,
            color = if (details.notes.isBlank()) BisqTheme.colors.mid_grey20 else BisqTheme.colors.light_grey20,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * One atomic edit surface for all three contact annotations (tag / trust / notes) with a single
 * Save/Cancel pair — no partial-save states. Field limits are enforced upstream in the presenter
 * (tag 30, notes 600, trust 0..1 per the bisq2 core contract).
 */
@Composable
private fun EditContactDetailsDialog(
    draft: ContactDetailsUiState,
    onAction: (PeerProfileUiAction) -> Unit,
) {
    BisqDialog(
        onDismissRequest = { onAction(PeerProfileUiAction.OnDismissEditContactDetailsDialog) },
        stickyBottomContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding)) {
                BisqButton(
                    text = "action.cancel".i18n(),
                    type = BisqButtonType.GreyOutline,
                    onClick = { onAction(PeerProfileUiAction.OnDismissEditContactDetailsDialog) },
                    modifier = Modifier.weight(1f),
                )
                BisqButton(
                    text = "action.save".i18n(),
                    onClick = { onAction(PeerProfileUiAction.OnSaveContactDetailsClick) },
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) {
        BisqText.H5Regular(text = "mobile.peerProfile.contactDetails.editTitle".i18n())
        BisqGap.V1()
        BisqTextFieldV0(
            value = draft.tag,
            onValueChange = { onAction(PeerProfileUiAction.OnContactTagChanged(it)) },
            label = "mobile.peerProfile.contactDetails.tagLabel".i18n(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        BisqText.XSmallRegularGrey(text = "${draft.tag.length}/30")
        BisqGap.V1()
        BisqText.SmallRegularGrey(text = "mobile.peerProfile.contactDetails.trustLabel".i18n())
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
        ) {
            BisqSlider(
                value = draft.trustScore.toFloat(),
                onValueChange = { onAction(PeerProfileUiAction.OnContactTrustScoreChanged(it.toDouble())) },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f),
            )
            BisqText.SmallRegular(text = "${(draft.trustScore * 100).roundToInt()}%")
        }
        BisqGap.V1()
        BisqTextFieldV0(
            value = draft.notes,
            onValueChange = { onAction(PeerProfileUiAction.OnContactNotesChanged(it)) },
            label = "mobile.peerProfile.contactDetails.notesLabel".i18n(),
            minLines = 3,
            maxLines = 6,
            modifier = Modifier.fillMaxWidth(),
        )
        BisqText.XSmallRegularGrey(text = "${draft.notes.length}/600")
    }
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
