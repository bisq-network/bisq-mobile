package network.bisq.mobile.presentation.startup.user_agreement

import androidx.compose.runtime.Composable
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScrollScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBar

@Composable
fun UserAgreementDisplayScreen() {
    BisqScrollScaffold(
        topBar = { TopBar(title = "tac.headline".i18n()) },
    ) { UserAgreementContent() }
}
