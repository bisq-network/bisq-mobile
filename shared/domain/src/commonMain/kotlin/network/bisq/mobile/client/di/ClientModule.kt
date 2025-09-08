package network.bisq.mobile.client.di

import network.bisq.mobile.client.service.accounts.AccountsApiGateway
import network.bisq.mobile.client.service.accounts.ClientAccountsServiceFacade
import network.bisq.mobile.client.service.bootstrap.ClientApplicationBootstrapFacade
import network.bisq.mobile.client.service.chat.trade.ClientTradeChatMessagesServiceFacade
import network.bisq.mobile.client.service.chat.trade.TradeChatMessagesApiGateway
import network.bisq.mobile.client.service.common.ClientLanguageServiceFacade
import network.bisq.mobile.client.service.explorer.ClientExplorerServiceFacade
import network.bisq.mobile.client.service.explorer.ExplorerApiGateway
import network.bisq.mobile.client.service.market.ClientMarketPriceServiceFacade
import network.bisq.mobile.client.service.market.MarketPriceApiGateway
import network.bisq.mobile.client.service.mediation.ClientMediationServiceFacade
import network.bisq.mobile.client.service.mediation.MediationApiGateway
import network.bisq.mobile.client.service.offers.ClientOffersServiceFacade
import network.bisq.mobile.client.service.offers.OfferbookApiGateway
import network.bisq.mobile.client.service.reputation.ClientReputationServiceFacade
import network.bisq.mobile.client.service.reputation.ReputationApiGateway
import network.bisq.mobile.client.service.settings.ClientSettingsServiceFacade
import network.bisq.mobile.client.service.settings.SettingsApiGateway
import network.bisq.mobile.client.service.trades.ClientTradesServiceFacade
import network.bisq.mobile.client.service.trades.TradesApiGateway
import network.bisq.mobile.client.service.user_profile.ClientUserProfileServiceFacade
import network.bisq.mobile.client.service.user_profile.UserProfileApiGateway
import network.bisq.mobile.domain.service.accounts.AccountsServiceFacade
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.chat.trade.TradeChatMessagesServiceFacade
import network.bisq.mobile.domain.service.common.LanguageServiceFacade
import network.bisq.mobile.domain.service.explorer.ExplorerServiceFacade
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.mediation.MediationServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.reputation.ReputationServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import org.koin.dsl.module

// services dependencies
val clientModule = module {

    single<ApplicationBootstrapFacade> { ClientApplicationBootstrapFacade(get(), get(), get()) }

    single { MarketPriceApiGateway(get(), get()) }
    single<MarketPriceServiceFacade> { ClientMarketPriceServiceFacade(get(), get(), get()) }

    single { UserProfileApiGateway(get(), get()) }
    single<UserProfileServiceFacade> { ClientUserProfileServiceFacade(get(), get(), get()) }

    single { OfferbookApiGateway(get(), get()) }
    single<OffersServiceFacade> { ClientOffersServiceFacade(get(), get(), get()) }

    single { TradesApiGateway(get()) }
    single<TradesServiceFacade> { ClientTradesServiceFacade(get(), get(), get()) }

    single { TradeChatMessagesApiGateway(get(), get()) }
    single<TradeChatMessagesServiceFacade> { ClientTradeChatMessagesServiceFacade(get(), get(), get(), get()) }

    single { ExplorerApiGateway(get()) }
    single<ExplorerServiceFacade> { ClientExplorerServiceFacade(get()) }

    single { MediationApiGateway(get()) }
    single<MediationServiceFacade> { ClientMediationServiceFacade(get()) }

    single { SettingsApiGateway(get()) }
    single<SettingsServiceFacade> { ClientSettingsServiceFacade(get()) }

    single { AccountsApiGateway(get()) }
    single<AccountsServiceFacade> { ClientAccountsServiceFacade(get()) }

    single<LanguageServiceFacade> { ClientLanguageServiceFacade() }

    single { ReputationApiGateway(get(), get()) }
    single<ReputationServiceFacade> { ClientReputationServiceFacade(get(), get()) }
}
