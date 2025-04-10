package network.bisq.mobile.presentation.ui.lifecycle

import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/**
 * CompositionLocal for ViewModelStoreOwner
 */
val LocalViewModelStoreOwner = compositionLocalOf<ViewModelStoreOwner> { 
    error("No ViewModelStoreOwner provided")
}