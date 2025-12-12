package network.bisq.mobile.presentation.trade.trade_chat

import androidx.compose.runtime.Composable
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.UnorderedTextList
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScrollScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme

@Composable
fun ChatRulesScreen() {
    BisqScrollScaffold(
        topBar = { TopBar(title = "chat.chatRules.headline".i18n()) },
    ) {
        UnorderedTextList(
            text = "chat.chatRules.content".i18n(),
            style = { t, m ->
                BisqText.baseLight(
                    text = t,
                    modifier = m,
                    color = BisqTheme.colors.light_grey40,
                )
            },
        )
    }
}
