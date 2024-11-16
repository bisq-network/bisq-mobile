package network.bisq.mobile.presentation.ui.uicases.startup

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import androidx.navigation.NavHostController

import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.presentation.ui.components.atoms.BisqProgressBar
import network.bisq.mobile.presentation.ui.components.atoms.icons.BisqLogo
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticLayout
import network.bisq.mobile.presentation.ui.theme.*
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named

interface ISplashPresenter {
    fun startLoading(onProgressUpdate: (Float) -> Unit)
}

@Composable
fun SplashScreen(
) {
    val strings = LocalStrings.current
    val navController: NavHostController = koinInject(named("RootNavController"))
    val presenter: ISplashPresenter = koinInject { parametersOf(navController) }
    
    var currentProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        presenter.startLoading { progress ->
            currentProgress = progress
        }
    }

    BisqStaticLayout {
        BisqLogo()

        Column {
            BisqProgressBar(progress = currentProgress)

            // TODO: Get this from presenter
            val networkType = strings.splash_bootstrapState_network_TOR

            BisqText.baseRegular(
                text = strings.splash_bootstrapState_BOOTSTRAP_TO_NETWORK(networkType),
                color = BisqTheme.colors.secondaryHover,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                )
        }
    }
}
