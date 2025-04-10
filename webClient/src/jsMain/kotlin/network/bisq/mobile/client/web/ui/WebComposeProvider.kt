package network.bisq.mobile.client.web.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import network.bisq.mobile.client.web.lifecycle.LocalViewModelStoreOwner
import network.bisq.mobile.client.web.lifecycle.WebLifecycleOwner
import network.bisq.mobile.client.web.lifecycle.WebViewModelStoreOwner

/**
 * Provides all necessary CompositionLocals for Compose UI to work in a web environment
 */
@Composable
fun WebComposeProvider(content: @Composable () -> Unit) {
    // Create instances
    val lifecycleOwner = WebLifecycleOwner()
    val viewModelStoreOwner = WebViewModelStoreOwner()
    val density = Density(1f)

    // Create a ViewConfiguration
    val viewConfiguration = object : androidx.compose.ui.platform.ViewConfiguration {
        override val longPressTimeoutMillis: Long = 500
        override val doubleTapTimeoutMillis: Long = 300
        override val doubleTapMinTimeMillis: Long = 40
        override val touchSlop: Float = 18f
        override val minimumTouchTargetSize = androidx.compose.ui.unit.DpSize(48.dp, 48.dp)
    }

    CompositionLocalProvider(
        LocalLifecycleOwner provides lifecycleOwner,
        LocalViewModelStoreOwner provides viewModelStoreOwner,
        LocalLayoutDirection provides LayoutDirection.Ltr,
        LocalDensity provides density,
        // For now, we'll use the default font family resolver
        LocalViewConfiguration provides viewConfiguration
    ) {
        content()
    }
}