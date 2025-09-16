/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */
package network.bisq.mobile.android.node

import android.content.Context
import androidx.core.util.Supplier
import bisq.account.AccountService
import bisq.application.ApplicationService
import bisq.application.State
import bisq.bisq_easy.BisqEasyService
import bisq.bonded_roles.BondedRolesService
import bisq.bonded_roles.security_manager.alert.AlertNotificationsService
import bisq.chat.ChatService
import bisq.common.locale.LanguageRepository
import bisq.common.observable.Observable
import bisq.common.util.ExceptionUtil
import bisq.contract.ContractService
import bisq.identity.IdentityService
import bisq.network.NetworkService
import bisq.network.NetworkServiceConfig
import bisq.offer.OfferService
import bisq.presentation.notifications.SystemNotificationService
import bisq.security.SecurityService
import bisq.settings.DontShowAgainService
import bisq.settings.FavouriteMarketsService
import bisq.settings.SettingsService
import bisq.support.SupportService
import bisq.trade.TradeService
import bisq.user.UserService
import bisq.user.reputation.ReputationService
import lombok.Getter
import lombok.Setter
import lombok.extern.slf4j.Slf4j
import network.bisq.mobile.android.node.service.AndroidMemoryReportService
import network.bisq.mobile.android.node.service.AndroidNodeCatHashService
import network.bisq.mobile.domain.utils.Logging
import java.nio.file.Path
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * Creates domain specific options from program arguments and application options.
 * Creates domain instance with options and optional dependency to other domain objects.
 * Initializes the domain instances according to the requirements of their dependencies either in sequence
 * or in parallel.
 */
@Slf4j
@Getter
class AndroidApplicationService(
    androidMemoryReportService: AndroidMemoryReportService,
    context: Context,
    userDataDir: Path?
) :
    ApplicationService("android", arrayOf<String>(), userDataDir), Logging {

    @Getter
    class Provider {
        @Setter
        lateinit var applicationService: AndroidApplicationService
        var state: Supplier<Observable<State>> =
            Supplier { applicationService.state }
        var androidCatHashService: Supplier<AndroidNodeCatHashService> =
            Supplier { applicationService.androidCatHashService }
        var securityService: Supplier<SecurityService> =
            Supplier { applicationService.securityService }
        var networkService: Supplier<NetworkService> =
            Supplier { applicationService.networkService }
        var identityService: Supplier<IdentityService> =
            Supplier { applicationService.identityService }
        var bondedRolesService: Supplier<BondedRolesService> =
            Supplier { applicationService.bondedRolesService }
        var accountService: Supplier<AccountService> =
            Supplier { applicationService.accountService }
        var offerService: Supplier<OfferService> =
            Supplier { applicationService.offerService }
        var contractService: Supplier<ContractService> =
            Supplier { applicationService.contractService }
        var userService: Supplier<UserService> =
            Supplier { applicationService.userService }
        var chatService: Supplier<ChatService> =
            Supplier { applicationService.chatService }
        var settingsService: Supplier<SettingsService> =
            Supplier { applicationService.settingsService }
        var bisqEasyService: Supplier<BisqEasyService> =
            Supplier { applicationService.bisqEasyService }
        var supportService: Supplier<SupportService> =
            Supplier { applicationService.supportService }
        var systemNotificationService: Supplier<SystemNotificationService> =
            Supplier { applicationService.systemNotificationService }
        var tradeService: Supplier<TradeService> =
            Supplier { applicationService.tradeService }
        var alertNotificationsService: Supplier<AlertNotificationsService> =
            Supplier { applicationService.alertNotificationsService }
        var favouriteMarketsService: Supplier<FavouriteMarketsService> =
            Supplier { applicationService.favouriteMarketsService }
        var dontShowAgainService: Supplier<DontShowAgainService> =
            Supplier { applicationService.dontShowAgainService }

        var languageRepository: Supplier<LanguageRepository> =
            Supplier { applicationService.languageRepository }
        var reputationService: Supplier<ReputationService> =
            Supplier { applicationService.userService.reputationService }
    }

    companion object {
        const val STARTUP_TIMEOUT_SEC: Long = 120
        const val SHUTDOWN_TIMEOUT_SEC: Long = 10
    }

    val shutDownErrorMessage = Observable<String>()
    val startupErrorMessage = Observable<String>()

    val androidCatHashService = AndroidNodeCatHashService(context, config.baseDir)

    val securityService =
        SecurityService(persistenceService, SecurityService.Config.from(getConfig("security")))

    val networkServiceConfig = NetworkServiceConfig.from(
        config.baseDir,
        getConfig("network")
    )

    val networkService = NetworkService(
        networkServiceConfig,
        persistenceService,
        securityService.keyBundleService,
        securityService.hashCashProofOfWorkService,
        securityService.equihashProofOfWorkService,
        androidMemoryReportService
    )
    val identityService = IdentityService(
        persistenceService,
        securityService.keyBundleService,
        networkService
    )
    val bondedRolesService = BondedRolesService(
        BondedRolesService.Config.from(getConfig("bondedRoles")),
        getPersistenceService(),
        networkService
    )
    val accountService = AccountService(persistenceService)
    val offerService = OfferService(networkService, identityService, persistenceService)
    val contractService = ContractService(securityService)
    val userService = UserService(
        persistenceService,
        securityService,
        identityService,
        networkService,
        bondedRolesService
    )
    val chatService: ChatService
    val settingsService = SettingsService(persistenceService)
    val supportService: SupportService
    val systemNotificationService = SystemNotificationService(Optional.empty())
    val tradeService: TradeService
    val bisqEasyService: BisqEasyService
    val alertNotificationsService: AlertNotificationsService
    val favouriteMarketsService: FavouriteMarketsService
    val dontShowAgainService: DontShowAgainService
    val languageRepository: LanguageRepository

    init {
        chatService = ChatService(
            persistenceService,
            networkService,
            userService,
            settingsService,
            systemNotificationService
        )

        supportService = SupportService(
            SupportService.Config.from(getConfig("support")),
            persistenceService,
            networkService,
            chatService,
            userService,
            bondedRolesService
        )


        tradeService = TradeService(
//            TODO: this is part of Bisq 2.1.8
//            TradeService.Config.from(getConfig("trade")),
            null,
            networkService,
            identityService,
            persistenceService,
            offerService,
            contractService,
            supportService,
            chatService,
            bondedRolesService,
            userService,
            settingsService
        )

        bisqEasyService = BisqEasyService(
            persistenceService,
            securityService,
            networkService,
            identityService,
            bondedRolesService,
            accountService,
            offerService,
            contractService,
            userService,
            chatService,
            settingsService,
            supportService,
            systemNotificationService,
            tradeService
        )

        alertNotificationsService =
            AlertNotificationsService(settingsService, bondedRolesService.alertService)

        favouriteMarketsService = FavouriteMarketsService(settingsService)

        dontShowAgainService = DontShowAgainService(settingsService)

        languageRepository = LanguageRepository()

    }

    override fun initialize(): CompletableFuture<Boolean> {
        var ts = System.currentTimeMillis()
        pruneAllBackups().join()
        log.i("pruneAllBackups took ${(System.currentTimeMillis() - ts)} ms")

        ts = System.currentTimeMillis()
        readAllPersisted().join()
        log.i("readAllPersisted took ${(System.currentTimeMillis() - ts)} ms")

        log.i("Starting securityService.initialize()")
        return securityService.initialize()
            .thenCompose { result: Boolean? ->
                log.i("securityService.initialize() completed with result: $result")
                setState(State.INITIALIZE_NETWORK)
                log.i("Starting networkService.initialize()")
                networkService.initialize()
            }
            .whenComplete { r: Boolean?, throwable: Throwable? ->
                if (throwable == null) {
                    log.i("networkService.initialize() completed with result: $r")
                    setState(State.INITIALIZE_SERVICES)
                } else {
                    log.e("networkService.initialize() failed", throwable)
                }
            }
            .thenCompose { result: Boolean? ->
                log.i("Starting identityService.initialize()")
                identityService.initialize()
            }
            .thenCompose { result: Boolean? ->
                log.i("identityService completed, starting bondedRolesService.initialize()")
                bondedRolesService.initialize()
            }
            .thenCompose { result: Boolean? ->
                log.i("bondedRolesService completed, starting accountService.initialize()")
                accountService.initialize()
            }
            .thenCompose { result: Boolean? ->
                log.i("accountService completed, starting contractService.initialize()")
                contractService.initialize()
            }
            .thenCompose { result: Boolean? ->
                log.i("contractService completed, starting userService.initialize()")
                userService.initialize()
            }
            .thenCompose { result: Boolean? -> settingsService.initialize() }
            .thenCompose { result: Boolean? -> offerService.initialize() }
            .thenCompose { result: Boolean? -> chatService.initialize() }
            .thenCompose { result: Boolean? -> systemNotificationService.initialize() }
            .thenCompose { result: Boolean? -> supportService.initialize() }
            .thenCompose { result: Boolean? -> tradeService.initialize() }
            .thenCompose { result: Boolean? -> alertNotificationsService.initialize() }
            .thenCompose { result: Boolean? -> favouriteMarketsService.initialize() }
            .thenCompose { result: Boolean? -> dontShowAgainService.initialize() }
            .orTimeout(STARTUP_TIMEOUT_SEC, TimeUnit.SECONDS)
            .handle { result: Boolean?, throwable: Throwable? ->
                if (throwable == null) {
                    if (result != null && result) {
                        setState(State.APP_INITIALIZED)
                        log.i("ApplicationService initialized")
                        return@handle true
                    } else {
                        startupErrorMessage.set("Initializing applicationService failed with result=false")
                        log.e(startupErrorMessage.get())
                    }
                } else {
                    log.e("Initializing applicationService failed", throwable)
                    startupErrorMessage.set(ExceptionUtil.getRootCauseMessage(throwable))
                }
                setState(State.FAILED)
                false
            }
    }

    override fun shutdown(): CompletableFuture<Boolean> {
        log.i("shutdown")
        // We shut down services in opposite order as they are initialized
        // In case a shutdown method completes exceptionally we log the error and map the result to `false` to not
        // interrupt the shutdown sequence.
        return CompletableFuture.supplyAsync<Boolean> {
            dontShowAgainService.shutdown()
                .exceptionally { throwable: Throwable -> this.logError(throwable) }
                .thenCompose { result: Boolean? ->
                    favouriteMarketsService.shutdown()
                        .exceptionally { throwable: Throwable -> this.logError(throwable) }
                }
                .thenCompose { result: Boolean? ->
                    alertNotificationsService.shutdown()
                        .exceptionally { throwable: Throwable -> this.logError(throwable) }
                }
                .thenCompose { result: Boolean? ->
                    tradeService.shutdown()
                        .exceptionally { throwable: Throwable -> this.logError(throwable) }
                }
                .thenCompose { result: Boolean? ->
                    supportService.shutdown()
                        .exceptionally { throwable: Throwable -> this.logError(throwable) }
                }
                .thenCompose { result: Boolean? ->
                    systemNotificationService.shutdown()
                        .exceptionally { throwable: Throwable -> this.logError(throwable) }
                }
                .thenCompose { result: Boolean? ->
                    chatService.shutdown()
                        .exceptionally { throwable: Throwable -> this.logError(throwable) }
                }
                .thenCompose { result: Boolean? ->
                    offerService.shutdown()
                        .exceptionally { throwable: Throwable -> this.logError(throwable) }
                }
                .thenCompose { result: Boolean? ->
                    settingsService.shutdown()
                        .exceptionally { throwable: Throwable -> this.logError(throwable) }
                }
                .thenCompose { result: Boolean? ->
                    userService.shutdown()
                        .exceptionally { throwable: Throwable -> this.logError(throwable) }
                }
                .thenCompose { result: Boolean? ->
                    contractService.shutdown()
                        .exceptionally { throwable: Throwable -> this.logError(throwable) }
                }
                .thenCompose { result: Boolean? ->
                    accountService.shutdown()
                        .exceptionally { throwable: Throwable -> this.logError(throwable) }
                }
                .thenCompose { result: Boolean? ->
                    bondedRolesService.shutdown()
                        .exceptionally { throwable: Throwable -> this.logError(throwable) }
                }
                .thenCompose { result: Boolean? ->
                    identityService.shutdown()
                        .exceptionally { throwable: Throwable -> this.logError(throwable) }
                }
                .thenCompose { result: Boolean? ->
                    networkService.shutdown()
                        .exceptionally { throwable: Throwable -> this.logError(throwable) }
                }
                .thenCompose { result: Boolean? ->
                    securityService.shutdown()
                        .exceptionally { throwable: Throwable -> this.logError(throwable) }
                }
                .orTimeout(SHUTDOWN_TIMEOUT_SEC, TimeUnit.SECONDS)
                .handle { result: Boolean?, throwable: Throwable? ->
                    if (throwable == null) {
                        if (result != null && result) {
                            log.i("ApplicationService shutdown completed")
                            return@handle true
                        } else {
                            startupErrorMessage.set("Shutdown applicationService failed with result=false")
                            log.e(shutDownErrorMessage.get())
                        }
                    } else {
                        log.e("Shutdown applicationService failed", throwable)
                        shutDownErrorMessage.set(ExceptionUtil.getRootCauseMessage(throwable))
                    }
                    false
                }
                .join()
        }
    }

    private fun logError(throwable: Throwable): Boolean {
        log.e("Exception at shutdown", throwable)
        return false
    }

    override fun checkInstanceLock() {
        // On mobile we dont need instance check
    }
}
