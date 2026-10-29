package network.bisq.mobile.client.common.domain.websocket.exception

import network.bisq.mobile.client.common.domain.websocket.subscription.Topic

/** The node did not answer a subscribe in time. Not a CancellationException, so callers treat it as a failure. */
class SubscribeTimeoutException(
    topic: Topic,
    cause: Throwable,
) : RuntimeException("Subscribe for topic $topic timed out", cause)
