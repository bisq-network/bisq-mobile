package network.bisq.mobile.presentation.ui.uicases.startup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqProgressBar
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.icons.BisqLogo
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticScaffold
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import org.koin.compose.koinInject

@Composable
fun SplashScreen(
) {
    val presenter: SplashPresenter = koinInject()

    RememberPresenterLifecycle(presenter)

    BisqStaticScaffold(verticalArrangement = Arrangement.SpaceBetween) {
        BisqLogo()

        Column {
            BisqProgressBar(presenter.progress.collectAsState().value)

            // TODO: Get this from presenter
            val networkType = "splash.bootstrapState.network.TOR".i18n()

            BisqText.baseRegularGrey(
                text = presenter.state.collectAsState().value,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
