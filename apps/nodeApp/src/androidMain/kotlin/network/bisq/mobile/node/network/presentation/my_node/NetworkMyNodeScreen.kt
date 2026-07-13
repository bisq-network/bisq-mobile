package network.bisq.mobile.node.network.presentation.my_node

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants

@Composable
fun NetworkMyNodeScreen() {
    BisqScaffold(
        topBar = { TopBar("mobile.networkInfo.myNode.title".i18n(), showUserAvatar = false) },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues)
                    .padding(
                        horizontal = BisqUIConstants.ScreenPadding,
                        vertical = BisqUIConstants.ScreenPaddingHalf,
                    ),
            horizontalAlignment = Alignment.Start,
        ) {
            // Content added in later steps (onion address, key ID, app version, Tor status).
        }
    }
}
