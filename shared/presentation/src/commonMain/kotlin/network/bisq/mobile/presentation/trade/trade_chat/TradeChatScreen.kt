package network.bisq.mobile.presentation.trade.trade_chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
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
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.WarningIcon
import network.bisq.mobile.presentation.common.ui.components.layout.BisqStaticScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.common.ui.components.molecules.chat.ChatInputField
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.common.ui.components.organisms.chat.ChatMessageList
import network.bisq.mobile.presentation.common.ui.components.organisms.chat.UndoIgnoreDialog
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
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

    val selectedTrade by presenter.selectedTrade.collectAsState()
    val sortedChatMessages by presenter.sortedChatMessages.collectAsState()
    val quotedMessage by presenter.quotedMessage.collectAsState()
    val ignoredUserIds by presenter.ignoredProfileIds.collectAsState()
    val ignoreUserId by presenter.ignoreUserId.collectAsState()
    val undoIgnoreUserId by presenter.undoIgnoreUserId.collectAsState()
    val showIgnoreUserWarnBox = ignoreUserId.isNotBlank()
    val showUndoIgnoreUserWarnBox = undoIgnoreUserId.isNotBlank()
    val showChatRulesWarnBox by presenter.showChatRulesWarnBox.collectAsState()
    val readCount by presenter.readCount.collectAsState()
    val showTradeNotFoundDialog by presenter.showTradeNotFoundDialog.collectAsState()
    val showReportUserDialog by presenter.showReportUserDialog.collectAsState()
    val reportUserTradeMessage by presenter.reportUserTradeMessage.collectAsState()
    val reportUserMessage by presenter.reportUserMessage.collectAsState()
    val isSendChatMessageEnabled by presenter.isSendChatMessageEnabled.collectAsState()
    val isConfirmIgnoreUserEnabled by presenter.isConfirmIgnoreUserEnabled.collectAsState()
    val isConfirmUndoIgnoreUserEnabled by presenter.isConfirmUndoIgnoreUserEnabled.collectAsState()

    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    BisqStaticScaffold(
        // The input carries the bottom margin itself now, so the content column must not add one:
        // that padding would otherwise land between the message list and the input, where the
        // list used to run straight into it.
        padding =
            PaddingValues(
                top = BisqUIConstants.ScreenPadding,
                start = BisqUIConstants.ScreenPadding,
                end = BisqUIConstants.ScreenPadding,
                bottom = BisqUIConstants.Zero,
            ),
        bottomBar = {
            // Moved out of the content column, so the scaffold's contentPadding no longer applies
            // to it: the side and bottom insets are restored here.
            ChatInputField(
                modifier =
                    Modifier.padding(
                        start = BisqUIConstants.ScreenPadding,
                        end = BisqUIConstants.ScreenPadding,
                        bottom = BisqUIConstants.ScreenPadding,
                    ),
                quotedMessage = quotedMessage,
                placeholder = "chat.message.input.prompt".i18n(),
                onMessageSend = presenter::sendChatMessage,
                onCloseReply = { presenter.onReply(null) },
                sendEnabled = isSendChatMessageEnabled,
            )
        },
        topBar = {
            TopBar(
                title =
                    "mobile.tradeChat.title".i18n(
                        selectedTrade?.shortTradeId ?: "",
                    ),
            )
        },
    ) {
        if (readCount == -1) {
            // empty placeholder until we know the readCount
            // this helps simplify logic inside the ChatMessageList
            // for providing better UX
            Box(modifier = Modifier.weight(1f))
        } else {
            ChatMessageList(
                messages = sortedChatMessages,
                ignoredUserIds = ignoredUserIds,
                showChatRulesWarnBox = showChatRulesWarnBox,
                userProfileIconProvider = presenter::userProfileIconProvider,
                readCount = readCount,
                onAddReaction = presenter::onAddReaction,
                onRemoveReaction = presenter::onRemoveReaction,
                onReply = presenter::onReply,
                onCopy = { message ->
                    scope.launch {
                        clipboard.setClipEntry(AnnotatedString(message.textString).toClipEntry())
                    }
                },
                onIgnoreUser = presenter::showIgnoreUserPopup,
                onUndoIgnoreUser = presenter::showUndoIgnoreUserPopup,
                onReportUser = presenter::onReportUser,
                onPeerProfileClick = presenter::onPeerProfileClick,
                onOpenChatRules = presenter::onOpenChatRules,
                onDontShowAgainChatRulesWarningBox = presenter::onDontShowAgainChatRulesWarningBox,
                onUpdateReadCount = presenter::onUpdateReadCount,
                modifier = Modifier.weight(1f),
                onResendMessage = { messageId -> presenter.onResendMessage(messageId) },
                userNameProvider = { messageId -> presenter.getUserName(messageId) },
            )
        }

        reportUserTradeMessage?.let { message ->
            if (showReportUserDialog) {
                ReportUserDialog(
                    accusedUserProfile = message.senderUserProfile,
                    reportMessage = reportUserMessage,
                    onReportFailure = presenter::onReportUserError,
                    onReportSuccess = presenter::onDismissReportUserDialog,
                )
            }
        }

        if (showIgnoreUserWarnBox) {
            ConfirmationDialog(
                headline = "mobile.error.warning".i18n(),
                headlineColor = BisqTheme.colors.warning,
                headlineLeftIcon = { WarningIcon() },
                message = "mobile.chat.ignoreUserWarn".i18n(),
                confirmButtonText = "chat.ignoreUser.confirm".i18n(),
                dismissButtonText = "action.cancel".i18n(),
                verticalButtonPlacement = true,
                confirmButtonLoading = !isConfirmIgnoreUserEnabled,
                onConfirm = { presenter.onConfirmedIgnoreUser(ignoreUserId) },
                onDismiss = { presenter.onDismissIgnoreUser() },
            )
        }

        if (showUndoIgnoreUserWarnBox) {
            UndoIgnoreDialog(
                onConfirm = { presenter.onConfirmedUndoIgnoreUser(undoIgnoreUserId) },
                onDismiss = { presenter.onDismissUndoIgnoreUser() },
                confirmButtonLoading = !isConfirmUndoIgnoreUserEnabled,
            )
        }

        if (showTradeNotFoundDialog) {
            ConfirmationDialog(
                headline = "mobile.openTrades.tradeNotFoundDialog.title".i18n(),
                message = "mobile.openTrades.tradeNotFoundDialog.text".i18n(),
                confirmButtonText = "confirmation.ok".i18n(),
                dismissButtonText = EMPTY_STRING,
                onConfirm = presenter::onTradeNotFoundDialogDismiss,
            )
        }
    }
}
