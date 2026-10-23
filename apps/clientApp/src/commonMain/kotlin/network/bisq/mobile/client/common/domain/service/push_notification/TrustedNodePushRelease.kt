package network.bisq.mobile.client.common.domain.service.push_notification

import kotlinx.coroutines.flow.SharedFlow

/**
 * Detaches this device from the relayed push delivery of the paired trusted node before that
 * pairing is wiped or replaced. Without it, the node being left keeps pushing trade updates to
 * this device.
 */
interface TrustedNodePushRelease {
    /**
     * Emits when the node being left could not be told to stop and this device cannot stop its
     * pushes by itself, so the user has to ask that node's operator to remove the device.
     */
    val releaseFailures: SharedFlow<Unit>

    /**
     * Must run while the current pairing and its connection are still in place. Deletes the local
     * push keys, revokes the push token, then unregisters from the node if it is connected.
     */
    suspend fun releaseCurrentNode()
}
