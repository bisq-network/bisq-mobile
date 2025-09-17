package network.bisq.mobile.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import network.bisq.mobile.domain.service.notifications.controller.NotificationServiceController
import network.bisq.mobile.domain.utils.CoroutineExceptionHandlerSetup
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.presentation.ui.App
import network.bisq.mobile.presentation.ui.error.GenericErrorHandler
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.theme.adjustGamma
import org.koin.android.ext.android.inject

/**
 * Base class for Bisq Android apps Main Activities
 */
abstract class MainActivity : ComponentActivity(), Logging {
    companion object {
        const val BACKGROUND_COLOR_CODE = 0xFF1C1C1C
    }

    private val presenter: MainPresenter by inject()
    private val exceptionHandlerSetup: CoroutineExceptionHandlerSetup by inject()

    // TODO probably better to handle from presenter once the user reach home?
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    init {
        GenericErrorHandler.init()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(NotificationServiceController.EXTRA_DESTINATION)?.let { destination ->
            Routes.fromString(destination)?.let { presenter.navigateToTab(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up coroutine exception handler after DI is initialized
        GenericErrorHandler.setupCoroutineExceptionHandler(exceptionHandlerSetup)

        presenter.attachView(this)

        val bgColor = Color(BACKGROUND_COLOR_CODE).adjustGamma().toArgb()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(bgColor),
            navigationBarStyle = SystemBarStyle.dark(bgColor),
        )
        setContent {
            App()
        }
    }

    override fun onStart() {
        super.onStart()
        presenter.onStart()
    }

    override fun onResume() {
        super.onResume()
        presenter.onResume()
    }

    override fun onPause() {
        presenter.onPause()
        super.onPause()
    }

    override fun onStop() {
        presenter.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        presenter.detachView()
        presenter.onDestroy()
        super.onDestroy()
    }
} 
