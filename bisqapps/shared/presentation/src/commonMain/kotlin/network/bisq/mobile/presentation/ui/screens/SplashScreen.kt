package network.bisq.mobile.presentation.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.ProgressIndicatorDefaults.drawStopIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import network.bisq.mobile.presentation.ui.theme.backgroundColor
import network.bisq.mobile.presentation.ui.theme.grey2
import network.bisq.mobile.presentation.ui.theme.primaryStandard
import network.bisq.mobile.presentation.ui.theme.secondaryHover
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.ExperimentalResourceApi

import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import bisqapps.shared.presentation.generated.resources.Res
import network.bisq.mobile.presentation.ui.navigation.Routes

//import bisqapps.shared.presentation.generated.resources.logo_with_slogan
//import io.kamel.image.KamelImage
//import io.kamel.image.asyncPainterResource

@OptIn(ExperimentalResourceApi::class)
@Composable
fun SplashScreen(rootNavController: NavController,
                 innerPadding: PaddingValues) {
    Scaffold(
        containerColor = backgroundColor,
        ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .background(color = backgroundColor)
                .padding(top = 48.dp, bottom = 30.dp)
        ) {
            //        3. Having issues rendering in both iOS and Android using Kamel (TODO: Yet to understand how ktor integration works with the lib, to properly try)
            //        KamelImage(
            //            { asyncPainterResource("https://image.nostr.build/resp/1080p/1f323cd199ad021209f74166e2de3693548918f8842eb2f29eb2fb9c04c3bdd8.jpg") }, contentDescription = "Hi"
            //        )
            //        KamelImage(
            //            { asyncPainterResource("drawable/logo_with_slogan.svg") }, contentDescription = null
            //        )

            //        2. Image not loading in iOS, though some Image placeholder is placed.
            //        Though it's said that with Coil3.0.0, iOS is supported
                    AsyncImage(
                        model = Res.getUri("drawable/logo_with_slogan.svg"),
                        contentDescription = "Bisq logo with slogan",
                        modifier = Modifier.height(92.dp).width(300.dp),
                    )
            //        TODO: Have to try this sample setup using 'viewModel'
            //        https://github.com/coil-kt/coil/blob/main/samples/compose/src/commonMain/kotlin/sample/compose/App.kt
            //        AsyncImage(
            //            model = ImageRequest.Builder(LocalPlatformContext.current)
            //                .data(screen.image.uri)
            //                .placeholderMemoryCacheKey(screen.placeholder)
            //                .extras(screen.image.extras)
            //                .build(),
            //            contentDescription = null,
            //            modifier = Modifier.fillMaxSize(),
            //        )
            //        AsyncImage(
            //            model = Res.getUri("https://image.nostr.build/resp/1080p/1f323cd199ad021209f74166e2de3693548918f8842eb2f29eb2fb9c04c3bdd8.jpg"),
            //            contentDescription = "Bisq logo with slogan",
            //            modifier = Modifier.height(92.dp).width(300.dp),
            //        )
            //
            //        1. Loading images from resources, doesn't work with SVG.
            //        Solution is to convert the SVG to VectorDrawable.
            //        But doing that makes it work only in Android, not with iOS
            //        Image(painterResource(Res.drawable.logo_with_slogan), null)
            LoadingProgress(rootNavController)
        }
    }
}


@Composable
fun LoadingProgress(navController: NavController) {
    var currentProgress by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()

    Column {
        LaunchedEffect(true) {
            scope.launch {
                loadProgress { progress ->
                    currentProgress = progress
                }
                navController.navigate(Routes.Onboarding.name) {
                    popUpTo(Routes.Splash.name) { inclusive = true }
                }
            }
        }

        LinearProgressIndicator(
            trackColor = grey2,
            color = primaryStandard,
            progress = { currentProgress },
            gapSize = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 100.dp)
                .padding(bottom = 20.dp)
                .height(2.dp),
            drawStopIndicator = {
                drawStopIndicator(
                    drawScope = this,
                    stopSize = 0.dp,
                    color = grey2,
                    strokeCap = ProgressIndicatorDefaults.LinearStrokeCap
                )
            }
        )
        Text(
            text = "Connecting to Tor Network...",
            color = secondaryHover,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

    }
}

suspend fun loadProgress(updateProgress: (Float) -> Unit) {
    for (i in 1..100) {
        updateProgress(i.toFloat() / 100)
        delay(100)
    }
}