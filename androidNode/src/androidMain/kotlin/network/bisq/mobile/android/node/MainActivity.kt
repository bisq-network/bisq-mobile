package network.bisq.mobile.android.node

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.App
import network.bisq.mobile.presentation.ui.error.GenericErrorHandler
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.adjustGamma
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val presenter : MainPresenter by inject()

    // TODO probably better to handle from presenter once the user reach home
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    init {
        GenericErrorHandler.init()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        intent?.getStringExtra("destination")?.let { destination ->
            Routes.fromString(destination)?.let { presenter.navigateToTab(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cleanupKoin()

        presenter.attachView(this)
        val bgColor = Color(0xFF1C1C1C).adjustGamma().toArgb()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(bgColor),
            navigationBarStyle = SystemBarStyle.dark(bgColor),
        )

        setContent {
            App()
        }

        handleDynamicPermissions()
    }

    private fun cleanupKoin() {
        // this is needed here to ensure cleanups in "zombie state"
        MainApplication.setupKoinDI(applicationContext)
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

    private fun handleDynamicPermissions() {
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission granted, proceed with posting notifications
            } else {
                // Permission denied, show a message to the user
                Toast.makeText(this, "Permission denied. Notifications won't be sent.", Toast.LENGTH_SHORT).show()
            }
        }

        // Call the method to check and request permission in APIs where its mandatory
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkAndRequestNotificationPermission()
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkAndRequestNotificationPermission() {
        // Check if the permission is granted
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted, proceed with posting notifications
        } else {
            // Request permission if not granted
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}