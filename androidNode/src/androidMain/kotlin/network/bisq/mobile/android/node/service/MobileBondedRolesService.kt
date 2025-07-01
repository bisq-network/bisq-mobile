package network.bisq.mobile.android.node.service

import bisq.bonded_roles.BondedRolesService
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService
import bisq.bonded_roles.explorer.ExplorerService
import bisq.bonded_roles.market_price.MarketPriceService
import bisq.bonded_roles.registration.BondedRoleRegistrationService
import bisq.bonded_roles.release.ReleaseNotificationsService
import bisq.bonded_roles.security_manager.alert.AlertService
import bisq.bonded_roles.security_manager.difficulty_adjustment.DifficultyAdjustmentService
import bisq.common.application.Service
import bisq.network.NetworkService
import bisq.persistence.PersistenceService
import java.util.concurrent.CompletableFuture

/**
 * Mobile-specific implementation of BondedRolesService that provides necessary interfaces
 * without processing authorized data, preventing infinite authorization loops.
 */
class MobileBondedRolesService(
    config: BondedRolesService.Config,
    persistenceService: PersistenceService,
    networkService: NetworkService
) : BondedRolesService(config, persistenceService, networkService) {

    // Create minimal implementations that don't process authorized data
    private val authorizedBondedRolesService = MobileAuthorizedBondedRolesService(networkService, config.isIgnoreSecurityManager)
    private val bondedRoleRegistrationService = BondedRoleRegistrationService(networkService, authorizedBondedRolesService)
    private val marketPriceService = MarketPriceService(config.marketPrice, persistenceService, networkService, authorizedBondedRolesService)
    private val explorerService = ExplorerService(ExplorerService.Config.from(config.blockchainExplorer), networkService)
    private val alertService = AlertService(authorizedBondedRolesService)
    private val difficultyAdjustmentService = DifficultyAdjustmentService(authorizedBondedRolesService)
    private val releaseNotificationsService = ReleaseNotificationsService(authorizedBondedRolesService)

    // Getters to match BondedRolesService interface
    override fun getAuthorizedBondedRolesService(): AuthorizedBondedRolesService = authorizedBondedRolesService
    override fun getBondedRoleRegistrationService(): BondedRoleRegistrationService = bondedRoleRegistrationService
    override fun getMarketPriceService(): MarketPriceService = marketPriceService
    override fun getExplorerService(): ExplorerService = explorerService
    override fun getAlertService(): AlertService = alertService
    override fun getDifficultyAdjustmentService(): DifficultyAdjustmentService = difficultyAdjustmentService
    override fun getReleaseNotificationsService(): ReleaseNotificationsService = releaseNotificationsService

    override fun initialize(): CompletableFuture<Boolean> {
        // Initialize only the services that don't process authorized data
        return difficultyAdjustmentService.initialize()
            .thenCompose { bondedRoleRegistrationService.initialize() }
            .thenCompose { marketPriceService.initialize() }
            .thenCompose { explorerService.initialize() }
            .thenCompose { alertService.initialize() }
            .thenCompose { releaseNotificationsService.initialize() }
            // Skip authorizedBondedRolesService.initialize() to prevent authorization processing
            .thenApply { true }
    }

    override fun shutdown(): CompletableFuture<Boolean> {
        return authorizedBondedRolesService.shutdown()
            .thenCompose { difficultyAdjustmentService.shutdown() }
            .thenCompose { alertService.shutdown() }
            .thenCompose { bondedRoleRegistrationService.shutdown() }
            .thenCompose { marketPriceService.shutdown() }
            .thenCompose { explorerService.shutdown() }
            .thenCompose { releaseNotificationsService.shutdown() }
    }
}
