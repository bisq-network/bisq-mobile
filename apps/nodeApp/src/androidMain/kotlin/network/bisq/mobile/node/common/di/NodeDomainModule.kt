package network.bisq.mobile.node.common.di

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import network.bisq.mobile.domain.AndroidUrlLauncher
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.service.AppForegroundController
import network.bisq.mobile.domain.service.ForegroundDetector
import network.bisq.mobile.domain.service.accounts.AccountsServiceFacade
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.bootstrap.ApplicationLifecycleService
import network.bisq.mobile.domain.service.chat.trade.TradeChatMessagesServiceFacade
import network.bisq.mobile.domain.service.common.LanguageServiceFacade
import network.bisq.mobile.domain.service.explorer.ExplorerServiceFacade
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.mediation.MediationServiceFacade
import network.bisq.mobile.domain.service.message_delivery.MessageDeliveryServiceFacade
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.domain.service.network.KmpTorService
import network.bisq.mobile.domain.service.network.NetworkServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.reputation.ReputationServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.AndroidDeviceInfoProvider
import network.bisq.mobile.domain.utils.DeviceInfoProvider
import network.bisq.mobile.domain.utils.VersionProvider
import network.bisq.mobile.node.BuildConfig
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService
import network.bisq.mobile.node.common.domain.service.NodeApplicationLifecycleService
import network.bisq.mobile.node.common.domain.service.accounts.NodeAccountsServiceFacade
import network.bisq.mobile.node.common.domain.service.bootstrap.NodeApplicationBootstrapFacade
import network.bisq.mobile.node.common.domain.service.cat_hash.AndroidNodeCatHashService
import network.bisq.mobile.node.common.domain.service.chat.trade.NodeTradeChatMessagesServiceFacade
import network.bisq.mobile.node.common.domain.service.common.NodeLanguageServiceFacade
import network.bisq.mobile.node.common.domain.service.explorer.NodeExplorerServiceFacade
import network.bisq.mobile.node.common.domain.service.market_price.NodeMarketPriceServiceFacade
import network.bisq.mobile.node.common.domain.service.mediation.NodeMediationServiceFacade
import network.bisq.mobile.node.common.domain.service.message_delivery.NodeMessageDeliveryServiceFacade
import network.bisq.mobile.node.common.domain.service.network.NodeConnectivityService
import network.bisq.mobile.node.common.domain.service.network.NodeNetworkServiceFacade
import network.bisq.mobile.node.common.domain.service.offers.NodeOffersServiceFacade
import network.bisq.mobile.node.common.domain.service.reputation.NodeReputationServiceFacade
import network.bisq.mobile.node.common.domain.service.settings.NodeSettingsServiceFacade
import network.bisq.mobile.node.common.domain.service.trades.NodeTradesServiceFacade
import network.bisq.mobile.node.common.domain.service.user_profile.NodeUserProfileServiceFacade
import network.bisq.mobile.node.common.domain.utils.AndroidMemoryReportService
import network.bisq.mobile.node.common.domain.utils.NodeVersionProvider
import network.bisq.mobile.node.main.NodeMainActivity
import network.bisq.mobile.node.settings.backup.domain.NodeBackupServiceFacade
import network.bisq.mobile.presentation.common.notification.ForegroundServiceController
import network.bisq.mobile.presentation.common.notification.ForegroundServiceControllerImpl
import network.bisq.mobile.presentation.common.notification.NotificationController
import network.bisq.mobile.presentation.common.notification.NotificationControllerImpl
import network.bisq.mobile.presentation.common.service.OpenTradesNotificationService
import okio.Path.Companion.toOkioPath
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

val androidNodeDomainModule = module {
    // System services for memory reporting
    single<ActivityManager> { androidContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager }
    single { ActivityManager.MemoryInfo() }
    single { Debug.MemoryInfo() }
    single { Runtime.getRuntime() }

    single<AndroidMemoryReportService> {
        val isDebug = BuildConfig.DEBUG
        AndroidMemoryReportService(get(), get(), get(), get(), isDebug)
    }

    single<AndroidNodeCatHashService> {
        val context = androidContext()
        AndroidNodeCatHashService(context, context.filesDir.toPath())
    }

    single<AndroidApplicationService> {
        AndroidApplicationService(get(), androidContext(), androidContext().filesDir.toPath())
    }

    single {
        val provider = AndroidApplicationService.Provider()
        provider.applicationService = get<AndroidApplicationService>()
        provider
    }

    single<MessageDeliveryServiceFacade> { NodeMessageDeliveryServiceFacade(get()) }

    single { NodeNetworkServiceFacade(get(), get()) } bind NetworkServiceFacade::class

    single<KmpTorService> {
        val applicationService = get<AndroidApplicationService>()
        KmpTorService(applicationService.config.baseDir.toOkioPath(true))
    }

    single { NodeApplicationBootstrapFacade(get(), get()) } bind ApplicationBootstrapFacade::class

    single<MarketPriceServiceFacade> { NodeMarketPriceServiceFacade(get(), get()) }

    single<UserProfileServiceFacade> { NodeUserProfileServiceFacade(get()) }

    single<OffersServiceFacade> { NodeOffersServiceFacade(get(), get(), get()) }

    single<ExplorerServiceFacade> { NodeExplorerServiceFacade(get()) }

    single<TradesServiceFacade> { NodeTradesServiceFacade(get()) }

    single<TradeChatMessagesServiceFacade> {
        NodeTradeChatMessagesServiceFacade(
            get(),
            get(),
            get()
        )
    }

    single<MediationServiceFacade> { NodeMediationServiceFacade(get()) }

    single<SettingsServiceFacade> { NodeSettingsServiceFacade(get()) }

    single<AccountsServiceFacade> { NodeAccountsServiceFacade(get()) }

    single<LanguageServiceFacade> { NodeLanguageServiceFacade() }

    single<NodeBackupServiceFacade> { NodeBackupServiceFacade(get(), get()) }

    single<ReputationServiceFacade> { NodeReputationServiceFacade(get()) }

    single { NodeConnectivityService(get()) } bind ConnectivityService::class

    single<UrlLauncher> { AndroidUrlLauncher(androidContext()) }

    single<NodeApplicationLifecycleService> {
        NodeApplicationLifecycleService(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    } bind ApplicationLifecycleService::class

    single<DeviceInfoProvider> { AndroidDeviceInfoProvider(androidContext()) }

    single<VersionProvider> { NodeVersionProvider() }

    single { AppForegroundController(androidContext()) } bind ForegroundDetector::class

    single {
        NotificationControllerImpl(
            get(),
            NodeMainActivity::class.java
        )
    } bind NotificationController::class

    single { ForegroundServiceControllerImpl(get()) } bind ForegroundServiceController::class

    single {
        OpenTradesNotificationService(get(), get(), get(), get(), get())
    }

}