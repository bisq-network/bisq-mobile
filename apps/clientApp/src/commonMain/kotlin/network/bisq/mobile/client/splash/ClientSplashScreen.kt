package network.bisq.mobile.client.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqProgressBar
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.BisqLogoGrey
import network.bisq.mobile.presentation.common.ui.components.layout.BisqStaticScaffold
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import network.bisq.mobile.presentation.startup.splash.SplashDialogs
import network.bisq.mobile.presentation.startup.splash.SplashPresenter
import org.koin.compose.koinInject

@Composable
fun ClientSplashScreen(route: NavRoute.Splash = NavRoute.Splash()) {
    val presenter: SplashPresenter = koinInject()
    remember(route) { presenter.applyRoute(route) }
    RememberPresenterLifecycle(presenter)

    val uiState by presenter.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        BisqStaticScaffold(
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                BisqLogoGrey(modifier = Modifier.size(155.dp))
            }
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BisqText.BaseLight(
                    text = uiState.appNameAndVersion,
                    color = BisqTheme.colors.mid_grey20,
                    modifier = Modifier.padding(bottom = 20.dp),
                )

                BisqProgressBar(uiState.progress)

                BisqText.BaseRegularGrey(
                    text = uiState.status,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        SplashDialogs(
            uiState = uiState,
            onAction = presenter::onAction,
        )
    }
}
