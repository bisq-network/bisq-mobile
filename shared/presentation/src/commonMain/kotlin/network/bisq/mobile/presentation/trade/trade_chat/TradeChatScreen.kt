package network.bisq.mobile.presentation.trade.trade_chat

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.launch
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.LoadingState
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.WarningIcon
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.common.ui.components.molecules.chat.trade.TradePeerLeftMessageBox
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.common.ui.components.organisms.chat.ChatMessageList
import network.bisq.mobile.presentation.common.ui.components.organisms.chat.ChatScaffold
import network.bisq.mobile.presentation.common.ui.components.organisms.chat.UndoIgnoreDialog
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import network.bisq.mobile.presentation.common.ui.utils.toClipEntry
import network.bisq.mobile.presentation.report_user.ReportUserDialog
import org.koin.compose.koinInject

@Composable
fun TradeChatScreen(tradeId: String) {
    val presenter: TradeChatPresenter = koinInject()
    RememberPresenterLifecycle(presenter)
    LaunchedEffect(presenter, tradeId) {
        presenter.initialize(tradeId)
    }

    val uiState by presenter.uiState.collectAsState()
    val isSendChatMessageEnabled by presenter.isSendChatMessageEnabled.collectAsState()
    val isConfirmIgnoreUserEnabled by presenter.isConfirmIgnoreUserEnabled.collectAsState()
    val isConfirmUndoIgnoreUserEnabled by presenter.isConfirmUndoIgnoreUserEnabled.collectAsState()

    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    ChatScaffold(
        onMessageSend = { presenter.onAction(TradeChatUiAction.OnSendMessage(it)) },
        quotedMessage = uiState.quotedMessage,
        placeholder = "chat.message.input.prompt".i18n(),
        onCloseReply = { presenter.onAction(TradeChatUiAction.OnReply(null)) },
        sendEnabled = isSendChatMessageEnabled && uiState.selectedTrade != null,
        mentionCandidates = uiState.mentionCandidates,
        topBar = {
            TopBar(
                title =
                    "mobile.tradeChat.title".i18n(
                        uiState.selectedTrade?.shortTradeId ?: "",
                    ),
            )
        },
    ) {
        if (uiState.isLoading) {
            // Ahead of the read-count branch, mirroring PrivateChatScreen: the trade has to resolve and
            // its messages have to arrive before there is anything to render. The Box bounds
            // LoadingState, which fills its parent and would otherwise push the input field off screen.
            Box(modifier = Modifier.weight(1f)) { LoadingState() }
        } else if (uiState.readCount == -1) {
            // empty placeholder until we know the readCount
            // this helps simplify logic inside the ChatMessageList
            // for providing better UX
            Box(modifier = Modifier.weight(1f))
        } else {
            ChatMessageList(
                messages = uiState.messages,
                ignoredUserIds = uiState.ignoredProfileIds,
                showChatRulesWarnBox = uiState.showChatRulesWarnBox,
                userProfileIconProvider = presenter::userProfileIconProvider,
                readCount = uiState.readCount,
                onAddReaction = { message, reaction ->
                    presenter.onAction(TradeChatUiAction.OnAddReaction(message, reaction))
                },
                onRemoveReaction = { message, reaction ->
                    presenter.onAction(TradeChatUiAction.OnRemoveReaction(message, reaction))
                },
                onReply = { presenter.onAction(TradeChatUiAction.OnReply(it)) },
                onCopy = { message ->
                    scope.launch {
                        clipboard.setClipEntry(AnnotatedString(message.textString).toClipEntry())
                    }
                },
                onIgnoreUser = { presenter.onAction(TradeChatUiAction.OnIgnoreUserClick(it)) },
                onUndoIgnoreUser = { presenter.onAction(TradeChatUiAction.OnUndoIgnoreUserClick(it)) },
                onReportUser = { presenter.onAction(TradeChatUiAction.OnReportUserClick(it)) },
                onPeerProfileClick = { presenter.onAction(TradeChatUiAction.OnPeerProfileClick(it)) },
                onOpenChatRules = { presenter.onAction(TradeChatUiAction.OnOpenChatRules) },
                onDontShowAgainChatRulesWarningBox = {
                    presenter.onAction(TradeChatUiAction.OnDontShowAgainChatRulesWarningBox)
                },
                onUpdateReadCount = { presenter.onAction(TradeChatUiAction.OnUpdateReadCount(it)) },
                modifier = Modifier.weight(1f),
                onResendMessage = { presenter.onAction(TradeChatUiAction.OnResendMessage(it)) },
                userNameProvider = { messageId -> presenter.getUserName(messageId) },
                myProfiles = uiState.myProfiles,
                leaveMessageContent = { message, modifier -> TradePeerLeftMessageBox(message, modifier) },
            )
        }

        uiState.reportTargetMessage?.let { message ->
            ReportUserDialog(
                accusedUserProfile = message.senderUserProfile,
                reportMessage = uiState.reportDraft,
                onReportFailure = { presenter.onAction(TradeChatUiAction.OnReportFailure(it)) },
                onReportSuccess = { presenter.onAction(TradeChatUiAction.OnDismissReportDialog) },
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
                confirmButtonLoading = !isConfirmIgnoreUserEnabled,
                onConfirm = { presenter.onAction(TradeChatUiAction.OnConfirmIgnore) },
                onDismiss = { presenter.onAction(TradeChatUiAction.OnDismissIgnoreDialog) },
            )
        }

        if (uiState.undoIgnoreTargetProfileId != null) {
            UndoIgnoreDialog(
                onConfirm = { presenter.onAction(TradeChatUiAction.OnConfirmUndoIgnore) },
                onDismiss = { presenter.onAction(TradeChatUiAction.OnDismissUndoIgnoreDialog) },
                confirmButtonLoading = !isConfirmUndoIgnoreUserEnabled,
            )
        }

        if (uiState.isTradeNotFound) {
            ConfirmationDialog(
                headline = "mobile.openTrades.tradeNotFoundDialog.title".i18n(),
                message = "mobile.openTrades.tradeNotFoundDialog.text".i18n(),
                confirmButtonText = "confirmation.ok".i18n(),
                dismissButtonText = EMPTY_STRING,
                onConfirm = { presenter.onAction(TradeChatUiAction.OnTradeNotFoundDialogDismiss) },
            )
        }
    }
}
