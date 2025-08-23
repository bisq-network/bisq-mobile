package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.trade_chat

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.buildAnnotatedString
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.icons.WarningIcon
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticScaffold
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.components.molecules.chat.ChatInputField
import network.bisq.mobile.presentation.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.ui.components.organisms.chat.ChatMessageList
import network.bisq.mobile.presentation.ui.components.organisms.chat.UndoIgnoreDialog
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.koin.compose.koinInject

@Composable
fun TradeChatScreen() {
    val presenter: TradeChatPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val scrollState = rememberLazyListState()
    val scope = rememberCoroutineScope() // TODO: How scopes are to be used?
    val isInteractive by presenter.isInteractive.collectAsState()
    val selectedTrade by presenter.selectedTrade.collectAsState()
    val chatMessages by presenter.chatMessages.collectAsState()
    val quotedMessage by presenter.quotedMessage.collectAsState()
    val sortedChatMessages = chatMessages.sortedBy { it.date }
    val userAvatarMap by presenter.avatarMap.collectAsState()
    val ignoredUserIds by presenter.ignoredUserIds.collectAsState()
    val ignoreUserId by presenter.ignoreUserId.collectAsState()
    val undoIgnoreUserId by presenter.undoIgnoreUserId.collectAsState()
    val showIgnoreUserWarnBox = ignoreUserId.isNotBlank()
    val showUndoIgnoreUserWarnBox = undoIgnoreUserId.isNotBlank()

    val clipboard = LocalClipboardManager.current

    BisqStaticScaffold(
        topBar = { TopBar(title = "mobile.tradeChat.title".i18n(selectedTrade?.shortTradeId ?: ""))},
        isInteractive = isInteractive,
    ) {

        ChatMessageList(
            messages = sortedChatMessages,
            ignoredUserIds = ignoredUserIds,
            presenter = presenter,
            modifier = Modifier.weight(1f),
            scrollState = scrollState,
            avatarMap = userAvatarMap,
            onAddReaction = presenter::onAddReaction,
            onRemoveReaction = presenter::onRemoveReaction,
            onReply = presenter::onReply,
            onCopy = { message -> clipboard.setText(buildAnnotatedString { append(message.textString) }) },
            onIgnoreUser = presenter::showIgnoreUserPopup,
            onUndoIgnoreUser = presenter::showUndoIgnoreUserPopup,
            onReportUser = presenter::onReportUser,
        )
        ChatInputField(
            quotedMessage = quotedMessage,
            placeholder = "chat.message.input.prompt".i18n(),
            onMessageSent = { text ->
                presenter.sendChatMessage(text, scope, scrollState)
            },
            onCloseReply = { presenter.onReply(null) }
        )

        if (showIgnoreUserWarnBox) {
            ConfirmationDialog(
                headline = "error.warning".i18n(),
                headlineColor = BisqTheme.colors.warning,
                headlineLeftIcon = { WarningIcon() },
                message = "mobile.chat.ignoreUserWarn".i18n(),
                confirmButtonText = "chat.ignoreUser.confirm".i18n(),
                dismissButtonText = "action.cancel".i18n(),
                verticalButtonPlacement = true,
                onConfirm = { presenter.onConfirmedIgnoreUser(ignoreUserId) },
                onDismiss = { presenter.onDismissIgnoreUser() }
            )
        }

        if (showUndoIgnoreUserWarnBox) {
            UndoIgnoreDialog(
                onConfirm = { presenter.onConfirmedUndoIgnoreUser(undoIgnoreUserId) },
                onDismiss = { presenter.onDismissUndoIgnoreUser() }
            )
        }
    }
}
