package network.bisq.mobile.domain.data

import kotlinx.coroutines.CoroutineDispatcher

/**
 * To use for I/O access and any expensive operation, selects the right dispatcher platform-based
 */
expect val BackgroundDispatcher: CoroutineDispatcher