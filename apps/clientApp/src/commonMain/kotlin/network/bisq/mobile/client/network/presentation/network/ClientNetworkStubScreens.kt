package network.bisq.mobile.client.network.presentation.network

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

// Temporary placeholders wired to the overview's sub-page cards. Replaced by the real
// Connections and My Connection screens in the next Network Info increments.

@Composable
fun ClientNetworkConnectionsScreen() {
    NetworkComingSoon(titleKey = "mobile.networkInfo.connections.title")
}

@Composable
fun ClientNetworkMyConnectionScreen() {
    NetworkComingSoon(titleKey = "mobile.networkInfo.connect.myConnection.title")
}

@Composable
private fun NetworkComingSoon(titleKey: String) {
    BisqScaffold(topBar = { TopBar(titleKey.i18n(), showUserAvatar = false) }) { paddingValues ->
        NetworkComingSoonContent(modifier = Modifier.padding(paddingValues))
    }
}

@Composable
private fun NetworkComingSoonContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BisqText.BaseRegular(
            text = "mobile.networkInfo.connect.comingSoon".i18n(),
            color = BisqTheme.colors.mid_grey30,
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun NetworkComingSoonContentPreview() {
    BisqTheme.Preview {
        NetworkComingSoonContent()
    }
}
