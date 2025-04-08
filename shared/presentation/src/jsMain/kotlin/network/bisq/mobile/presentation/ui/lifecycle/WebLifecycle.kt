package network.bisq.mobile.client.web.lifecycle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

// Simple web implementation of LifecycleOwner
class WebLifecycleOwner : LifecycleOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)

    init {
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
}

// Default instance
val defaultLifecycleOwner = WebLifecycleOwner()

// Simple web implementation of ViewModelStoreOwner
class WebViewModelStoreOwner : ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()
}

// Default instance
val defaultViewModelStoreOwner = WebViewModelStoreOwner()

// CompositionLocal for ViewModelStoreOwner
val LocalViewModelStoreOwner = staticCompositionLocalOf<ViewModelStoreOwner> {
    defaultViewModelStoreOwner
}

/**
 * Provides web-specific lifecycle components and other necessary CompositionLocals
 */
@Composable
fun WebLifecycleProvider(content: @Composable () -> Unit) {
    // Create a default density
    val density = Density(1f)

    CompositionLocalProvider(
        LocalLifecycleOwner provides defaultLifecycleOwner,
        LocalViewModelStoreOwner provides defaultViewModelStoreOwner,
        LocalLayoutDirection provides LayoutDirection.Ltr,
        LocalDensity provides density
    ) {
        content()
    }
}