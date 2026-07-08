package network.bisq.mobile.data.service.network

/**
 * Tor bootstrap failed before [NetworkServiceFacade] could finish activation.
 *
 * Expected on the user-recovery path (retry / purge Tor data). Must not trigger
 * [network.bisq.mobile.data.service.bootstrap.ApplicationLifecycleService.onUnrecoverableError].
 */
class TorBootstrapNotReadyException(
    message: String = "Tor bootstrap failed before network services could start",
) : Exception(message)
