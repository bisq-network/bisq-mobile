package network.bisq.mobile.presentation.ui.navigation

import kotlinx.atomicfu.atomic
import kotlin.concurrent.Volatile

// https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-navigation-deep-links.html#handle-received-deep-links

/**
 * common uri handler for deep links
 */
object ExternalUriHandler {
    // Storage for when a URI arrives before the listener is set up
    private var cached = atomic<String?>(null)

    @Volatile
    var listener: ((uri: String) -> Unit)? = null
        set(value) {
            field = value
            if (value != null) {
                // When a listener is set and `cached` is not empty,
                // immediately invoke the listener with the cached URI
                cached.getAndSet(null)?.let { value.invoke(it) }
            }
        }

    // When a new URI arrives, cache it.
    // If the listener is already set, invoke it and clear the cache immediately.
    fun onNewUri(uri: String) {
        cached.value = uri
        listener?.let {
            cached.getAndSet(null)?.let { cachedUri -> it.invoke(cachedUri) }
        }
    }
}
