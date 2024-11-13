package network.bisq.mobile.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.compose_multiplatform
import kotlinx.coroutines.flow.StateFlow
import org.koin.compose.koinInject

interface AppPresenter {
    // Observables for state
    val isContentVisible: StateFlow<Boolean>
    val greetingText: StateFlow<String>

    // Actions
    fun toggleContentVisibility()
}

/**
 * Main composable view of the application that platforms use to draw.
 */
@Composable
@Preview
fun App() {
    val presenter: AppPresenter = koinInject()
    MaterialTheme {
        // Collecting state from presenter
        val showContent by presenter.isContentVisible.collectAsState()
        val greeting by presenter.greetingText.collectAsState()
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = { presenter.toggleContentVisibility() }) {
                Text("Click me!")
            }
            AnimatedVisibility(showContent) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(Res.drawable.compose_multiplatform), null)
                    Text("Compose: $greeting")
                }
            }
        }
    }
}