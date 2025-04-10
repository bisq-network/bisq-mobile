package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.trade_chat

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.buildAnnotatedString
import kotlinx.coroutines.launch
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticScaffold
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.components.molecules.chat.ChatInputField
import network.bisq.mobile.presentation.ui.components.organisms.chat.ChatMessageList
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import org.koin.compose.koinInject

@Composable
fun TradeChatScreen() {
    val presenter: TradeChatPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    var chatText by remember { mutableStateOf("") }
    val scrollState = rememberLazyListState()
    val scope = rememberCoroutineScope() // TODO: How scopes are to be used?
    val selectedTrade by presenter.selectedTrade.collectAsState()
    val chatMessages by presenter.chatMessages.collectAsState()
    val quotedMessage by presenter.quotedMessage.collectAsState()
    val sortedChatMessages = chatMessages.sortedBy { it.date }

    /*   var quotedMessage by remember {
           mutableStateOf<BisqEasyOpenTradeMessageModel?>(null)
       }*/

    val clipboard = LocalClipboardManager.current

    BisqStaticScaffold(
        topBar = { TopBar(title = "Chat - " + selectedTrade?.shortTradeId) },
    ) {
        ChatMessageList(
            messages = sortedChatMessages,
            presenter = presenter,
            modifier = Modifier.weight(1f),
            scrollState = scrollState,
            onAddReaction = { message, reaction -> presenter.onAddReaction(message, reaction) },
            onRemoveReaction = { message, reaction -> presenter.onRemoveReaction(message, reaction) },
            onReply = { message -> presenter.onReply(message) },
            onCopy = { message -> clipboard.setText(buildAnnotatedString { append(message.textString) }) },
            onIgnoreUser = { message -> presenter.onIgnoreUser(message) },
            onReportUser = { message -> presenter.onReportUser(message) },
        )
        ChatInputField(
            //value = chatText,
            quotedMessage = quotedMessage,
            placeholder = "chat.message.input.prompt".i18n(),
            // onValueChanged = { chatText = it },
            onMessageSent = { text ->
                chatText = ""
                presenter.sendChatMessage(text)
                scope.launch { scrollState.animateScrollToItem(Int.MAX_VALUE) }
            },
            onCloseReply = { presenter.onReply(null) }
        )
    }
}
