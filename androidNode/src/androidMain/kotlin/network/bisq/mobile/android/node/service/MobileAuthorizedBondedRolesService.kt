package network.bisq.mobile.android.node.service

import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService
import bisq.network.NetworkService
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData
import java.util.concurrent.CompletableFuture

/**
 * Mobile-specific implementation of AuthorizedBondedRolesService that provides
 * necessary interfaces without processing authorized data from the network.
 * This prevents infinite authorization loops in the mobile app.
 */
class MobileAuthorizedBondedRolesService(
    networkService: NetworkService,
    ignoreSecurityManager: Boolean
) : AuthorizedBondedRolesService(networkService, ignoreSecurityManager) {

    // Mobile app inherits all the necessary methods from AuthorizedBondedRolesService
    // but overrides initialization to prevent authorization processing

    override fun initialize(): CompletableFuture<Boolean> {
        // Mobile app skips the problematic authorization processing that causes infinite loops
        // We don't call super.initialize() to avoid processing authorized data from the network
        return CompletableFuture.completedFuture(true)
    }

    override fun shutdown(): CompletableFuture<Boolean> {
        // Call parent shutdown but don't process any data
        return super.shutdown()
    }

    // Override the problematic methods to prevent authorization processing
    override fun onAuthorizedDataAdded(authorizedData: AuthorizedData) {
        // Mobile app ignores authorized data to prevent authorization loops
        // Don't call super.onAuthorizedDataAdded() to avoid processing
    }

    override fun onAuthorizedDataRemoved(authorizedData: AuthorizedData) {
        // Mobile app ignores authorized data to prevent authorization loops
        // Don't call super.onAuthorizedDataRemoved() to avoid processing
    }
}
