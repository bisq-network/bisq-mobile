package network.bisq.mobile.presentation.ui.uicases.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollScaffold
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject


@Composable
fun ReputationScreen() {
    val presenter: ReputationPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val isInteractive by presenter.isInteractive.collectAsState()

    BisqScrollScaffold(
        topBar = { TopBar("mobile.more.reputation".i18n(), showUserAvatar = false) },
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
        snackbarHostState = presenter.getSnackState(),
        isInteractive = isInteractive,
    ) {

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "TODO ReputationScreen")
        }
    }
}
