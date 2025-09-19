package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.trade_chat

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.launch
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.icons.WarningIcon
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticScaffold
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.components.molecules.chat.ChatInputField
import network.bisq.mobile.presentation.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.ui.components.organisms.chat.ChatMessageList
import network.bisq.mobile.presentation.ui.components.organisms.chat.UndoIgnoreDialog
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.helpers.toClipEntry
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.koin.compose.koinInject

@Composable
fun TradeChatScreen() {
    val presenter: TradeChatPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val isInteractive by presenter.isInteractive.collectAsState()
    val selectedTrade by presenter.selectedTrade.collectAsState()
    val sortedChatMessages by presenter.sortedChatMessages.collectAsState()
    val quotedMessage by presenter.quotedMessage.collectAsState()
    val userAvatarMap by presenter.avatarMap.collectAsState()
    val ignoredUserIds by presenter.ignoredProfileIds.collectAsState()
    val ignoreUserId by presenter.ignoreUserId.collectAsState()
    val undoIgnoreUserId by presenter.undoIgnoreUserId.collectAsState()
    val showIgnoreUserWarnBox = ignoreUserId.isNotBlank()
    val showUndoIgnoreUserWarnBox = undoIgnoreUserId.isNotBlank()
    val showChatRulesWarnBox by presenter.showChatRulesWarnBox.collectAsState()
    val readCount by presenter.readCount.collectAsState()

    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    BisqStaticScaffold(
        topBar = {
            TopBar(
                title = "mobile.tradeChat.title".i18n(
                    selectedTrade?.shortTradeId ?: ""
                )
            )
        },
        isInteractive = isInteractive,
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
                avatarMap = userAvatarMap,
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
                onOpenChatRules = presenter::onOpenChatRules,
                onDontShowAgainChatRulesWarningBox = presenter::onDontShowAgainChatRulesWarningBox,
                onUpdateReadCount = presenter::onUpdateReadCount,
                modifier = Modifier.weight(1f),
            )
        }
        ChatInputField(
            quotedMessage = quotedMessage,
            placeholder = "chat.message.input.prompt".i18n(),
            onMessageSent = presenter::sendChatMessage,
            onCloseReply = { presenter.onReply(null) }
        )

        if (showIgnoreUserWarnBox) {
            ConfirmationDialog(
                headline = "mobile.error.warning".i18n(),
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
