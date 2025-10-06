package network.bisq.mobile.android.node.di

import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.NodeApplicationLifecycleService
import network.bisq.mobile.android.node.presentation.NodeDashboardPresenter
import network.bisq.mobile.android.node.presentation.NodeMainPresenter
import network.bisq.mobile.android.node.presentation.NodeMiscItemsPresenter
import network.bisq.mobile.android.node.presentation.NodeOnboardingPresenter
import network.bisq.mobile.android.node.presentation.NodeSettingsPresenter
import network.bisq.mobile.android.node.presentation.NodeSplashPresenter
import network.bisq.mobile.android.node.service.AndroidMemoryReportService
import network.bisq.mobile.android.node.service.AndroidNodeCatHashService
import network.bisq.mobile.android.node.service.accounts.NodeAccountsServiceFacade
import network.bisq.mobile.android.node.service.bootstrap.NodeApplicationBootstrapFacade
import network.bisq.mobile.android.node.service.chat.trade.NodeTradeChatMessagesServiceFacade
import network.bisq.mobile.android.node.service.common.NodeLanguageServiceFacade
import network.bisq.mobile.android.node.service.explorer.NodeExplorerServiceFacade
import network.bisq.mobile.android.node.service.market_price.NodeMarketPriceServiceFacade
import network.bisq.mobile.android.node.service.mediation.NodeMediationServiceFacade
import network.bisq.mobile.android.node.service.message_delivery.NodeMessageDeliveryServiceFacade
import network.bisq.mobile.android.node.service.network.NodeConnectivityService
import network.bisq.mobile.android.node.service.network.NodeNetworkServiceFacade
import network.bisq.mobile.android.node.service.offers.NodeOffersServiceFacade
import network.bisq.mobile.android.node.service.reputation.NodeReputationServiceFacade
import network.bisq.mobile.android.node.service.settings.NodeSettingsServiceFacade
import network.bisq.mobile.android.node.service.trades.NodeTradesServiceFacade
import network.bisq.mobile.android.node.service.user_profile.NodeUserProfileServiceFacade
import network.bisq.mobile.android.node.utils.NodeVersionProvider
import network.bisq.mobile.domain.AndroidUrlLauncher
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.service.accounts.AccountsServiceFacade
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
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
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.AppPresenter
import network.bisq.mobile.presentation.ui.uicases.DashboardPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.IGeneralSettingsPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.MiscItemsPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.SettingsPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.IOnboardingPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.SplashPresenter
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

val androidNodeModule = module {
    single<AndroidMemoryReportService> {
        AndroidMemoryReportService(androidContext())
    }

    single<AndroidNodeCatHashService> {
        val context = androidContext()
        AndroidNodeCatHashService(context, context.filesDir.toPath())
    }

    single { AndroidApplicationService.Provider() }

    single<MessageDeliveryServiceFacade> { NodeMessageDeliveryServiceFacade(get()) }

    single { NodeNetworkServiceFacade(get()) } bind NetworkServiceFacade::class

    single<KmpTorService> { KmpTorService() }

    single { NodeApplicationBootstrapFacade(get(), get()) } bind ApplicationBootstrapFacade::class

    single<MarketPriceServiceFacade> { NodeMarketPriceServiceFacade(get(), get()) }

    single<UserProfileServiceFacade> { NodeUserProfileServiceFacade(get()) }

    single<OffersServiceFacade> { NodeOffersServiceFacade(get(), get(), get()) }

    single<ExplorerServiceFacade> { NodeExplorerServiceFacade(get()) }

    single<TradesServiceFacade> { NodeTradesServiceFacade(get()) }

    single<TradeChatMessagesServiceFacade> { NodeTradeChatMessagesServiceFacade(get(), get(), get()) }

    single<MediationServiceFacade> { NodeMediationServiceFacade(get()) }

    single<SettingsServiceFacade> { NodeSettingsServiceFacade(get()) }

    single<AccountsServiceFacade> { NodeAccountsServiceFacade(get()) }

    single<LanguageServiceFacade> { NodeLanguageServiceFacade() }

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
            get()
        )
    }

    single<MainPresenter> {
        NodeMainPresenter(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    } bind AppPresenter::class

    single<SplashPresenter> {
        NodeSplashPresenter(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }

    single<DashboardPresenter> {
        NodeDashboardPresenter(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }

    single<MiscItemsPresenter> { NodeMiscItemsPresenter(get(), get()) }

    single<DeviceInfoProvider> { AndroidDeviceInfoProvider(androidContext()) }

    single<VersionProvider> { NodeVersionProvider() }

    factory<SettingsPresenter> { NodeSettingsPresenter(get(), get(), get()) } bind IGeneralSettingsPresenter::class

    single<IOnboardingPresenter> {
        NodeOnboardingPresenter(
            get(),
            get(),
            get()
        )
    } bind IOnboardingPresenter::class
}