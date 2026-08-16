package network.bisq.mobile.presentation.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import network.bisq.mobile.domain.utils.CoroutineExceptionHandlerSetup
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.presentation.common.ui.error.GenericErrorHandler
import network.bisq.mobile.presentation.common.ui.navigation.ExternalUriHandler
import org.koin.android.ext.android.inject

/**
 * Base class for Bisq Android apps Main Activities
 */
abstract class MainActivity :
    ComponentActivity(),
    Logging {
    private val presenter: MainPresenter by inject()
    private val exceptionHandlerSetup: CoroutineExceptionHandlerSetup by inject()

    init {
        GenericErrorHandler.init()
    }

    private fun handleDeepLinkIntent(intent: Intent?) {
        intent?.let {
            if (it.action == Intent.ACTION_VIEW) {
                it.dataString?.let { uriString -> ExternalUriHandler.onNewUri(uriString) }
            }
        }
    }

    private fun sanitizeDeepLinkIntent(intent: Intent): Intent {
        if (intent.action != Intent.ACTION_VIEW || intent.data == null) {
            return intent
        }

        // strip the deeplink off the activity intent after caching it, to stop premature navigation
        return Intent(intent).apply {
            action = null
            data = null
            clipData = null
        }
    }

    override fun onNewIntent(intent: Intent) {
        handleDeepLinkIntent(intent)
        val sanitizedIntent = sanitizeDeepLinkIntent(intent)
        setIntent(sanitizedIntent)
        super.onNewIntent(sanitizedIntent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (MainApplication.wasProcessDead.getAndSet(false)) {
            // this is to enforce proper initialization if process was killed by OS
            super.onCreate(null)
        } else {
            super.onCreate(savedInstanceState)
        }

        // Configure the window before anything can observe or draw into it: subclasses call
        // setContent() right after super.onCreate(), and the presenter may act on the attached view.
        enableEdgeToEdgeCompat()

        // Set up coroutine exception handler after DI is initialized
        GenericErrorHandler.setupCoroutineExceptionHandler(exceptionHandlerSetup)

        handleDeepLinkIntent(intent)
        intent?.let { intent = sanitizeDeepLinkIntent(it) }

        presenter.attachView(this)
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
