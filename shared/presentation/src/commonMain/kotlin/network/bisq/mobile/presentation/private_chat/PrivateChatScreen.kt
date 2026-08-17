package network.bisq.mobile.presentation.private_chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import network.bisq.mobile.data.replicated.chat.two_party.createMockTwoPartyPrivateChatMessage
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.data.utils.createEmptyImage
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.LoadingState
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.WarningIcon
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBarContent
import network.bisq.mobile.presentation.common.ui.components.molecules.chat.ChatInputField
import network.bisq.mobile.presentation.common.ui.components.molecules.chat.private_messages.LeaveChatIconButton
import network.bisq.mobile.presentation.common.ui.components.molecules.chat.private_messages.PrivateChatPeerHeader
import network.bisq.mobile.presentation.common.ui.components.molecules.chat.private_messages.PrivateChatPeerLeftMessageBox
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.common.ui.components.organisms.chat.ChatMessageList
import network.bisq.mobile.presentation.common.ui.components.organisms.chat.UndoIgnoreDialog
import network.bisq.mobile.presentation.common.ui.i18n.i18nText
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycleBackStackAware
import network.bisq.mobile.presentation.common.ui.utils.toClipEntry
import network.bisq.mobile.presentation.report_user.ReportUserDialog

/**
 * Private chat (DM) thread with one peer (issue #590).
 *
 * Uses the back-stack-aware lifecycle because this screen is deep-linkable from a notification and
 * sits above the peer profile: reloading the channel on every reveal would flash the loading state.
 *
 * The report dialog is a slot rather than being rendered inside [PrivateChatScreenContent], since it
 * injects its own presenter — same reason as `PeerProfileScreen`.
 */
@ExcludeFromCoverage
@Composable
fun PrivateChatScreen(channelId: String) {
    val presenter = RememberPresenterLifecycleBackStackAware<PrivateChatPresenter>()
    val uiState by presenter.uiState.collectAsState()
    val isSendChatMessageEnabled by presenter.isSendChatMessageEnabled.collectAsState()
    val isLeaveChatEnabled by presenter.isLeaveChatEnabled.collectAsState()
    val isConfirmIgnoreUserEnabled by presenter.isConfirmIgnoreUserEnabled.collectAsState()
    val isConfirmUndoIgnoreUserEnabled by presenter.isConfirmUndoIgnoreUserEnabled.collectAsState()

    LaunchedEffect(presenter, channelId) {
        presenter.initialize(channelId)
    }

    PrivateChatScreenContent(
        uiState = uiState,
        onAction = presenter::onAction,
        userProfileIconProvider = presenter.userProfileIconProvider,
        userNameProvider = { profileId -> presenter.getUserName(profileId) },
        isSendChatMessageEnabled = isSendChatMessageEnabled,
        isLeaveChatEnabled = isLeaveChatEnabled,
        isConfirmIgnoreUserEnabled = isConfirmIgnoreUserEnabled,
        isConfirmUndoIgnoreUserEnabled = isConfirmUndoIgnoreUserEnabled,
        topBar = {
            TopBar(
                // Gated on the peer, like the header below: peerName is "" until loadPeer resolves,
                // and the format string renders a trailing-space "Chat with " for that whole window —
                // permanently so on the not-found path, where the peer never arrives.
                title =
                    if (uiState.peerUserProfile != null) {
                        i18nText("mobile.privateChats.peer.header", uiState.peerName)
                    } else {
                        EMPTY_STRING
                    },
                showUserAvatar = false,
                extraActions = {
                    LeaveChatIconButton(onClick = { presenter.onAction(PrivateChatUiAction.OnLeaveChatClick) })
                },
            )
        },
        reportDialog = {
            val target = uiState.reportTargetProfile
            if (uiState.showReportDialog && target != null) {
                ReportUserDialog(
                    accusedUserProfile = target,
                    reportMessage = uiState.reportDraft,
                    onReportFailure = { reportMessage ->
                        presenter.onAction(PrivateChatUiAction.OnReportFailure(reportMessage))
                    },
                    onReportSuccess = { presenter.onAction(PrivateChatUiAction.OnDismissReportDialog) },
                )
            }
        },
    )
}

@Composable
internal fun PrivateChatScreenContent(
    uiState: PrivateChatUiState,
    onAction: (PrivateChatUiAction) -> Unit,
    userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage,
    userNameProvider: suspend (String) -> String,
    isSendChatMessageEnabled: Boolean = true,
    isLeaveChatEnabled: Boolean = true,
    isConfirmIgnoreUserEnabled: Boolean = true,
    isConfirmUndoIgnoreUserEnabled: Boolean = true,
    topBar: @Composable () -> Unit = {},
    reportDialog: @Composable () -> Unit = {},
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    // The peer header carries its own background and is meant to read as a section head, so it runs
    // edge to edge — hence a scaffold that adds no content padding of its own. The message list and
    // the input field ask for the opposite and get the inset back individually: the list's LazyColumn
    // has no horizontal contentPadding, so without it the bubbles would touch the screen edge.
    BisqScaffold(topBar = topBar) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            uiState.peerUserProfile?.let { peer ->
                PrivateChatPeerHeader(
                    peerUserProfile = peer,
                    peerName = uiState.peerName,
                    peerStarRating = uiState.peerStarRating,
                    userProfileIconProvider = userProfileIconProvider,
                    onClick = { onAction(PrivateChatUiAction.OnPeerHeaderClick) },
                )
            }

            when {
                // Ahead of the read-count branch: while the channel is being resolved both hold, and
                // that is the wait worth showing — awaitChannel allows up to CHANNEL_WAIT_TIMEOUT_MS.
                // The Box bounds it: LoadingState fills its parent, which in a Column would take the
                // whole height and push the input field off screen.
                uiState.isLoading -> Box(modifier = Modifier.weight(1f)) { LoadingState() }

                // Same contract as TradeChatScreen: suppress the list until the read count is known,
                // so the unread divider does not jump once it resolves. Left blank rather than given
                // a spinner of its own — by here the channel has resolved and this lasts a frame or
                // two.
                uiState.readCount == -1 -> Box(modifier = Modifier.weight(1f))

                uiState.messages.isEmpty() -> EmptyConversationHint()

                else -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    ChatMessageList(
                        messages = uiState.messages,
                        ignoredUserIds = uiState.ignoredProfileIds,
                        showChatRulesWarnBox = uiState.showChatRulesWarnBox,
                        readCount = uiState.readCount,
                        userProfileIconProvider = { userProfileIconProvider },
                        // No-op: a DM never carries a delivery status, so the resend affordance that
                        // depends on one is never rendered — see PrivateChatPresenter.observeMessages.
                        onResendMessage = {},
                        userNameProvider = userNameProvider,
                        onPeerProfileClick = { onAction(PrivateChatUiAction.OnPeerProfileClick(it)) },
                        modifier = Modifier.weight(1f).padding(horizontal = BisqUIConstants.ScreenPadding),
                        onAddReaction = { message, reaction ->
                            onAction(PrivateChatUiAction.OnAddReaction(message, reaction))
                        },
                        onRemoveReaction = { message, reaction ->
                            onAction(PrivateChatUiAction.OnRemoveReaction(message, reaction))
                        },
                        onReply = { onAction(PrivateChatUiAction.OnReply(it)) },
                        onCopy = { message ->
                            scope.launch {
                                clipboard.setClipEntry(AnnotatedString(message.textString).toClipEntry())
                            }
                        },
                        onIgnoreUser = { onAction(PrivateChatUiAction.OnIgnoreUserClick(it)) },
                        onUndoIgnoreUser = { onAction(PrivateChatUiAction.OnUndoIgnoreUserClick(it)) },
                        onReportUser = { onAction(PrivateChatUiAction.OnReportUserClick(it)) },
                        onOpenChatRules = { onAction(PrivateChatUiAction.OnOpenChatRules) },
                        onDontShowAgainChatRulesWarningBox = {
                            onAction(PrivateChatUiAction.OnDontShowAgainChatRulesWarningBox)
                        },
                        onUpdateReadCount = { onAction(PrivateChatUiAction.OnUpdateReadCount(it)) },
                        // "has left the trade" would be wrong here.
                        leaveMessageContent = { message, modifier -> PrivateChatPeerLeftMessageBox(message, modifier) },
                    )
                }
            }

            ChatInputField(
                modifier =
                    Modifier.padding(
                        horizontal = BisqUIConstants.ScreenPadding,
                        vertical = BisqUIConstants.ScreenPaddingHalf,
                    ),
                quotedMessage = uiState.quotedMessage,
                placeholder = "chat.message.input.prompt".i18n(),
                onMessageSend = { onAction(PrivateChatUiAction.OnSendMessage(it)) },
                onCloseReply = { onAction(PrivateChatUiAction.OnReply(null)) },
                sendEnabled = isSendChatMessageEnabled,
            )

            reportDialog()

            if (uiState.showLeaveConfirmDialog) {
                ConfirmationDialog(
                    headline = "mobile.privateChats.chat.leaveChat".i18n(),
                    headlineColor = BisqTheme.colors.warning,
                    headlineLeftIcon = { WarningIcon() },
                    message = "mobile.privateChats.chat.leaveConfirm".i18n(uiState.peerName),
                    confirmButtonText = "mobile.privateChats.chat.leaveConfirm.confirm".i18n(),
                    dismissButtonText = "action.cancel".i18n(),
                    verticalButtonPlacement = true,
                    confirmButtonLoading = !isLeaveChatEnabled,
                    onConfirm = { onAction(PrivateChatUiAction.OnConfirmLeave) },
                    onDismiss = { onAction(PrivateChatUiAction.OnDismissLeaveDialog) },
                )
            }

            if (uiState.ignoreUserId.isNotBlank()) {
                ConfirmationDialog(
                    headline = "mobile.error.warning".i18n(),
                    headlineColor = BisqTheme.colors.warning,
                    headlineLeftIcon = { WarningIcon() },
                    message = "mobile.chat.ignoreUserWarn".i18n(),
                    confirmButtonText = "chat.ignoreUser.confirm".i18n(),
                    dismissButtonText = "action.cancel".i18n(),
                    verticalButtonPlacement = true,
                    confirmButtonLoading = !isConfirmIgnoreUserEnabled,
                    onConfirm = { onAction(PrivateChatUiAction.OnConfirmIgnore) },
                    onDismiss = { onAction(PrivateChatUiAction.OnDismissIgnoreDialog) },
                )
            }

            if (uiState.undoIgnoreUserId.isNotBlank()) {
                UndoIgnoreDialog(
                    onConfirm = { onAction(PrivateChatUiAction.OnConfirmUndoIgnore) },
                    onDismiss = { onAction(PrivateChatUiAction.OnDismissUndoIgnoreDialog) },
                    confirmButtonLoading = !isConfirmUndoIgnoreUserEnabled,
                )
            }

            if (uiState.isChannelNotFound) {
                ConfirmationDialog(
                    headline = "mobile.error.warning".i18n(),
                    message = "mobile.privateChats.chat.notFound".i18n(),
                    confirmButtonText = "confirmation.ok".i18n(),
                    dismissButtonText = EMPTY_STRING,
                    onConfirm = { onAction(PrivateChatUiAction.OnChannelNotFoundDialogDismiss) },
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.EmptyConversationHint() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(BisqUIConstants.ScreenPadding2X),
        contentAlignment = Alignment.Center,
    ) {
        BisqText.BaseLight(
            text = "mobile.privateChats.chat.emptyHint".i18n(),
            color = BisqTheme.colors.mid_grey20,
            textAlign = TextAlign.Center,
        )
    }
}

// ============================================================================================
// Previews
// ============================================================================================

private val previewUserProfileIconProvider: suspend (UserProfileVO) -> PlatformImage = { createEmptyImage() }

@ExcludeFromCoverage
@Composable
private fun PreviewTopBar(uiState: PrivateChatUiState) {
    TopBarContent(
        title = "mobile.privateChats.peer.header".i18n(uiState.peerName),
        showBackButton = true,
        showUserAvatar = false,
    )
}

private fun previewUiState(
    peerName: String = "SatoshiFan",
    readCount: Int = 0,
) = PrivateChatUiState(
    channelId = "discussion.a-b",
    peerUserProfile = createMockUserProfile(peerName),
    peerName = peerName,
    peerStarRating = 4.5,
    readCount = readCount,
    isLoading = false,
)

@ExcludeFromCoverage
@Composable
private fun PrivateChatPreview(uiState: PrivateChatUiState) {
    BisqTheme.Preview {
        PrivateChatScreenContent(
            uiState = uiState,
            onAction = {},
            userProfileIconProvider = previewUserProfileIconProvider,
            userNameProvider = { it },
            topBar = { PreviewTopBar(uiState) },
        )
    }
}

private const val PREVIEW_MY_NAME = "Bob"

/**
 * `isMyMessage` is derived from the profile ids, so [PREVIEW_MY_NAME] as the sender yields an own
 * message and any other name a peer one.
 */
private fun previewMessage(
    id: String,
    senderName: String,
    text: String,
) = createMockTwoPartyPrivateChatMessage(
    id = id,
    text = text,
    senderUserProfile = createMockUserProfile(senderName),
    myUserProfile = createMockUserProfile(PREVIEW_MY_NAME),
)

/** Newest first: `ChatMessageList` renders with `reverseLayout = true`, so index 0 sits at the bottom. */
private val previewConversation by lazy {
    listOf(
        previewMessage("msg-4", PREVIEW_MY_NAME, "Perfect, thanks!"),
        previewMessage("msg-3", "SatoshiFan", "Sure. I usually take a couple of hours to answer, but I always do."),
        previewMessage("msg-2", PREVIEW_MY_NAME, "Hi! I saw your offer, can I ask you something about it first?"),
        previewMessage("msg-1", "SatoshiFan", "Hey"),
    )
}

@ExcludeFromCoverage
@Preview
@Composable
private fun PrivateChatScreen_EmptyPreview() = PrivateChatPreview(previewUiState())

/**
 * All three fields have to be overridden: `previewUiState` hands back a resolved channel, and the
 * real loading state has no peer yet either — that is why the header is missing while it lasts.
 */
@ExcludeFromCoverage
@Preview
@Composable
private fun PrivateChatScreen_LoadingPreview() = PrivateChatPreview(previewUiState().copy(isLoading = true, peerUserProfile = null, readCount = -1))

/**
 * `heightDp` because the list's `LazyColumn` fills its parent — without a bounded height it collapses
 * and the preview renders blank. `readCount` matches the message count so no unread divider appears.
 */
@ExcludeFromCoverage
@Preview(heightDp = 700)
@Composable
private fun PrivateChatScreen_ConversationPreview() =
    PrivateChatPreview(
        previewUiState(readCount = previewConversation.size).copy(messages = previewConversation),
    )

@ExcludeFromCoverage
@Preview
@Composable
private fun PrivateChatScreen_LeaveConfirmDialogPreview() = PrivateChatPreview(previewUiState().copy(showLeaveConfirmDialog = true))

@ExcludeFromCoverage
@Preview
@Composable
private fun PrivateChatScreen_ChannelNotFoundPreview() = PrivateChatPreview(previewUiState().copy(isChannelNotFound = true))
