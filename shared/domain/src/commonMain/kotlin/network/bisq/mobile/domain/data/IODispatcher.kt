package network.bisq.mobile.domain.data

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Provides the best suited dispatcher for IO operations.
 *
 * For Android Dispatchers.IO dispatcher is used for offloading blocking I/O tasks to a shared pool of threads.
 * For iOS there is no dedicated IO dispatch, thus Dispatchers.Default is used.
 *
 * Typical use cases:
 * - Reading/writing files
 * - Network requests using blocking clients
 * - Accessing databases
 * - Interacting with legacy APIs that block the thread
 */
expect val IODispatcher: CoroutineDispatcher