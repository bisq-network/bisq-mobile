package network.bisq.mobile.presentation.main

import android.os.Build
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Lays the window out edge-to-edge without androidx.activity's `enableEdgeToEdge`, which still
 * calls `Window.setStatusBarColor`, `Window.setNavigationBarColor` and requests
 * `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` - all deprecated in Android 15 and reported by the
 * Play Console.
 *
 * The system bars stay transparent; the app paints its own background behind them (see
 * `SafeInsetsContainer`) and pads content by the system bar and display cutout insets.
 */
fun ComponentActivity.enableEdgeToEdgeCompat() {
    WindowCompat.setDecorFitsSystemWindows(window, false)

    // Render into the cutout area on all orientations. LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS exists
    // from API 30 on; older devices keep the platform default (letterboxed cutout in landscape).
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        window.attributes =
            window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
    }

    // The app is dark-only, so the system bar icons must always be light.
    WindowInsetsControllerCompat(window, window.decorView).apply {
        isAppearanceLightStatusBars = false
        isAppearanceLightNavigationBars = false
    }
}
