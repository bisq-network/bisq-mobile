package network.bisq.mobile.presentation.community.public_chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import network.bisq.mobile.data.replicated.chat.common.createMockCommonPublicChatMessage
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.data.utils.createEmptyImage
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.LoadingState
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.WarningIcon
import network.bisq.mobile.presentation.common.ui.components.molecules.chat.ChatInputField
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.common.ui.components.molecules.inputfield.BisqSearchField
import network.bisq.mobile.presentation.common.ui.components.organisms.chat.ChatMessageList
import network.bisq.mobile.presentation.common.ui.components.organisms.chat.UndoIgnoreDialog
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.toClipEntry

/**
 * A public chat thread, parameterized by domain through its presenter — Discussions in the hub's
 * segment, Support in its own pushed screen.
 *
 * **A layout, not a scaffold, and it owns no top bar.** The hub mounts a segment body inside its own
 * `BisqScaffold` → `Column` → `Box(weight(1f))`, and that scaffold already carries `imePadding()`; a
 * nested scaffold here would apply the window insets and the ime padding twice and reserve a
 * bottom-bar height inside a weighted box. Whoever mounts this owns the chrome: the hub mounts it
 * bare, and `SupportChannelScreen` gives it a `BisqScaffold` with only a top bar — not `ChatScaffold`,
 * which owns a `ChatInputBottomBar` and would leave the screen with two composers, since the
 * `ChatInputField` below is this layout's own.
 *
 * @param reportDialog a slot rather than rendered here, since `ReportUserDialog` injects its own
 *   presenter — same reason `PrivateChatScreen` does it.
 */
@Composable
fun PublicChatThreadContent(
    uiState: PublicChatUiState,
    onAction: (PublicChatUiAction) -> Unit,
    userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage,
    userNameProvider: suspend (String) -> String,
    isSendChatMessageEnabled: Boolean,
    modifier: Modifier = Modifier,
    isIgnoreActionEnabled: Boolean = true,
    reportDialog: @Composable () -> Unit = {},
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        when {
            // Terminal, not a slow load: an older node exposes none of the endpoints, so there is
            // nothing to wait for.
            !uiState.isSupported -> CenteredHint("mobile.community.chat.notAvailable".i18n())

            uiState.isLoading -> Box(modifier = Modifier.weight(1f)) { LoadingState() }

            // Same contract as the private and trade threads: suppress the list until the read count
            // is known, so the unread divider does not jump once it resolves.
            uiState.readCount == -1 -> Box(modifier = Modifier.weight(1f))

            else -> {
                // Persistent rather than behind a toggle: it costs one row and removes a state field,
                // an action and a branch — `isSearching` is derived from the query itself.
                BisqSearchField(
                    value = uiState.searchQuery,
                    onValueChange = { onAction(PublicChatUiAction.OnSearchQueryChange(it)) },
                    placeholder = "mobile.community.chat.search.prompt".i18n(),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (uiState.isSearching) {
                    BisqText.SmallLightGrey(
                        text = "mobile.community.chat.search.matches".i18n(uiState.searchMatchCount),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = BisqUIConstants.ScreenPadding, vertical = BisqUIConstants.ScreenPaddingQuarter),
                    )
                }

                if (uiState.messages.isEmpty()) {
                    CenteredHint(
                        if (uiState.isSearching) {
                            "mobile.community.chat.search.noMatches".i18n()
                        } else {
                            "mobile.community.chat.empty".i18n()
                        },
                    )
                } else {
                    // Keyed on the search, so entering or leaving it discards the remembered scroll
                    // position rather than restoring one from the other list.
                    key(uiState.isSearching) {
                        ChatMessageList(
                            messages = uiState.messages,
                            ignoredUserIds = uiState.ignoredProfileIds,
                            showChatRulesWarnBox = uiState.showChatRulesWarnBox,
                            readCount = uiState.readCount,
                            userProfileIconProvider = { userProfileIconProvider },
                            // A public message carries no delivery status, so the resend affordance
                            // that depends on one is never rendered.
                            onResendMessage = {},
                            userNameProvider = userNameProvider,
                            onPeerProfileClick = { onAction(PublicChatUiAction.OnPeerProfileClick(it)) },
                            modifier = Modifier.weight(1f).padding(horizontal = BisqUIConstants.ScreenPadding),
                            onAddReaction = { message, reaction ->
                                onAction(PublicChatUiAction.OnAddReaction(message, reaction))
                            },
                            onRemoveReaction = { message, reaction ->
                                onAction(PublicChatUiAction.OnRemoveReaction(message, reaction))
                            },
                            onReply = { onAction(PublicChatUiAction.OnReply(it)) },
                            onCopy = { message ->
                                scope.launch {
                                    clipboard.setClipEntry(AnnotatedString(message.textString).toClipEntry())
                                }
                            },
                            onIgnoreUser = { onAction(PublicChatUiAction.OnIgnoreUserClick(it)) },
                            onUndoIgnoreUser = { onAction(PublicChatUiAction.OnUndoIgnoreUserClick(it)) },
                            onReportUser = { onAction(PublicChatUiAction.OnReportUserClick(it)) },
                            onOpenChatRules = { onAction(PublicChatUiAction.OnOpenChatRules) },
                            onDontShowAgainChatRulesWarningBox = {
                                onAction(PublicChatUiAction.OnDontShowAgainChatRulesWarningBox)
                            },
                            onUpdateReadCount = { onAction(PublicChatUiAction.OnUpdateReadCount(it)) },
                            onEditMessage = { onAction(PublicChatUiAction.OnEditMessage(it)) },
                            onDeleteMessage = { onAction(PublicChatUiAction.OnDeleteMessageClick(it)) },
                            myProfiles = uiState.myProfiles,
                            // A public channel never emits LEAVE, and any wording here would be a lie.
                            leaveMessageContent = { _, _ -> },
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
                    onMessageSend = { onAction(PublicChatUiAction.OnSendMessage(it)) },
                    onCloseReply = { onAction(PublicChatUiAction.OnReply(null)) },
                    sendEnabled = isSendChatMessageEnabled,
                    editingMessageId = uiState.editingMessageId,
                    editingInitialText = uiState.editingInitialText,
                    onCancelEdit = { onAction(PublicChatUiAction.OnCancelEdit) },
                    mentionCandidates = uiState.mentionCandidates,
                )
            }
        }

        reportDialog()

        if (uiState.deleteTargetMessageId != null) {
            ConfirmationDialog(
                headline = "mobile.error.warning".i18n(),
                headlineColor = BisqTheme.colors.warning,
                headlineLeftIcon = { WarningIcon() },
                message = "bisqEasy.offerbook.chatMessage.deleteMessage.confirmation".i18n(),
                confirmButtonText = "action.delete".i18n(),
                dismissButtonText = "action.cancel".i18n(),
                verticalButtonPlacement = true,
                onConfirm = { onAction(PublicChatUiAction.OnConfirmDelete) },
                onDismiss = { onAction(PublicChatUiAction.OnDismissDeleteDialog) },
            )
        }

        if (uiState.ignoreTargetProfileId != null) {
            ConfirmationDialog(
                headline = "mobile.error.warning".i18n(),
                headlineColor = BisqTheme.colors.warning,
                headlineLeftIcon = { WarningIcon() },
                message = "mobile.chat.ignoreUserWarn".i18n(),
                confirmButtonText = "chat.ignoreUser.confirm".i18n(),
                dismissButtonText = "action.cancel".i18n(),
                verticalButtonPlacement = true,
                confirmButtonLoading = !isIgnoreActionEnabled,
                onConfirm = { onAction(PublicChatUiAction.OnConfirmIgnore) },
                onDismiss = { onAction(PublicChatUiAction.OnDismissIgnoreDialog) },
            )
        }

        if (uiState.undoIgnoreTargetProfileId != null) {
            UndoIgnoreDialog(
                onConfirm = { onAction(PublicChatUiAction.OnConfirmUndoIgnore) },
                onDismiss = { onAction(PublicChatUiAction.OnDismissUndoIgnoreDialog) },
                confirmButtonLoading = !isIgnoreActionEnabled,
            )
        }
    }
}

@Composable
private fun ColumnScope.CenteredHint(text: String) {
    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
        BisqText.BaseRegularGrey(
            text = text,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = BisqUIConstants.ScreenPadding),
        )
    }
}

/**
 * Rendered against SUPPORT rather than Discussions on purpose: the domain is what `SupportChannelScreen`
 * varies, and a preview of the default would not exercise it.
 */
@ExcludeFromCoverage
@Preview(heightDp = 700)
@Composable
private fun PublicChatThreadContent_SupportPreview() {
    BisqTheme.Preview {
        val me = createMockUserProfile("Bob")
        val peer = createMockUserProfile("Alice")
        PublicChatThreadContent(
            uiState =
                PublicChatUiState(
                    channelId = "support.support",
                    isLoading = false,
                    readCount = 2,
                    messages =
                        listOf(
                            createMockCommonPublicChatMessage(
                                id = "m2",
                                text = "Check the trade guide, it covers that step.",
                                senderUserProfile = peer,
                                myUserProfile = me,
                                date = 1234567890000L,
                            ),
                            createMockCommonPublicChatMessage(
                                id = "m1",
                                text = "How do I confirm the fiat payment?",
                                senderUserProfile = me,
                                myUserProfile = me,
                                date = 1234567880000L,
                            ),
                        ),
                    mentionCandidates = listOf(peer, me),
                    myProfiles = listOf(me),
                ),
            onAction = {},
            userProfileIconProvider = { createEmptyImage() },
            userNameProvider = { it },
            isSendChatMessageEnabled = true,
        )
    }
}
