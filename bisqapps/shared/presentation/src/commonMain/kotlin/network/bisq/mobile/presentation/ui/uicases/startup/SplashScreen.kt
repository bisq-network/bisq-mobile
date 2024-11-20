package network.bisq.mobile.presentation.ui.uicases.startup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavHostController
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.presentation.ui.components.atoms.BisqProgressBar
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.icons.BisqLogo
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticScaffold
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named


@Composable
fun SplashScreen(
) {
    val strings = LocalStrings.current
    val navController: NavHostController = koinInject(named("RootNavController"))
    val presenter: SplashPresenter = koinInject { parametersOf(navController) }

    LaunchedEffect(Unit) {
        presenter.onViewAttached()
    }

    BisqStaticScaffold {
        BisqLogo()

        Column {
            BisqProgressBar(presenter.progress.collectAsState().value)

            // TODO: Get this from presenter
            val networkType = strings.splash_bootstrapState_network_TOR

            BisqText.baseRegular(
                text = presenter.state.collectAsState().value,
                color = BisqTheme.colors.secondaryHover,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
