package network.bisq.mobile.presentation.ui.uicases.startup

import androidx.compose.runtime.Composable
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollScaffold
import network.bisq.mobile.presentation.ui.components.molecules.TopBar

@Composable
fun UserAgreementDisplayScreen() {
    BisqScrollScaffold(
        topBar = { TopBar(title = "tac.headline".i18n()) },
    ) { UserAgreementContent() }
}
