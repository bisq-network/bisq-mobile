package network.bisq.mobile.presentation.ui.screens

import BisqProgressBar
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.ExperimentalResourceApi

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import network.bisq.mobile.presentation.ui.navigation.Routes
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.bisq_logo
import cafe.adriel.lyricist.LocalStrings
import cafe.adriel.lyricist.rememberStrings
import network.bisq.mobile.presentation.ui.components.atoms.icons.BisqLogo
import network.bisq.mobile.presentation.ui.components.foundation.BisqText
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticLayout
import network.bisq.mobile.presentation.ui.theme.*

// TODO: Remove innerPadding once StaticLayout, ScrollLayout are fully done.
@OptIn(ExperimentalResourceApi::class)
@Composable
fun SplashScreen(
    rootNavController: NavController,
    innerPadding: PaddingValues
) {

    var currentProgress by remember { mutableFloatStateOf(0f) }

    val presenter = remember {
        SplashPresenter(rootNavController) { progress ->
            currentProgress = progress
        }
    }

    val strings = LocalStrings.current

    LaunchedEffect(Unit) {
        presenter.startLoading()
    }

    // Render the layout
    BisqStaticLayout {
        BisqLogo()

        Column {
            BisqProgressBar(progress = currentProgress)

            BisqText.baseRegular(
                text = strings.splash_loading_text, // "Connecting to Tor Network...",
                color = BisqTheme.colors.secondaryHover,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                )
        }
    }
}
