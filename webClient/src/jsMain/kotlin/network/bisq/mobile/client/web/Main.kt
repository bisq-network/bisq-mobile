package network.bisq.mobile.client.web

import androidx.compose.runtime.Composable
import kotlinx.browser.document
import network.bisq.mobile.client.di.clientModule
import network.bisq.mobile.client.web.di.webClientModule
import network.bisq.mobile.domain.di.domainModule
import network.bisq.mobile.presentation.ui.App
import org.jetbrains.compose.web.renderComposable
import org.koin.core.context.startKoin

fun main() {
    setupKoin()
    
    renderComposable(rootElementId = "root") {
        BisqWebApp()
    }
}

@Composable
fun BisqWebApp() {
    App()
}

private fun setupKoin() {
    startKoin {
        modules(
            domainModule,
            clientModule,
            webClientModule
        )
    }
}