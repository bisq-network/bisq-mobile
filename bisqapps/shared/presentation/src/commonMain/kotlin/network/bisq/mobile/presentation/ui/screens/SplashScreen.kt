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

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import network.bisq.mobile.presentation.ui.navigation.Routes
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.bisq_logo

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
            Image(painterResource(Res.drawable.bisq_logo), "Bisq Logo")
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