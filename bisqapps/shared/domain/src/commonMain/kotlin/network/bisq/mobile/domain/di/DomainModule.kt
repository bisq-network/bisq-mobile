package network.bisq.mobile.domain.di

import network.bisq.mobile.domain.data.model.*
import network.bisq.mobile.domain.data.repository.*
import org.koin.dsl.module

val domainModule = module {
    single<GreetingRepository<Greeting>> { GreetingRepository() }
    single<BisqStatsRepository<BisqStats>> { BisqStatsRepository() }
    single<BtcPriceRepository<BtcPrice>> { BtcPriceRepository() }
    single<NetworkRepository<NetworkModel>> { NetworkRepository() }
    single<UserProfileRepository<UserProfile>> { UserProfileRepository() }
    single<SettingsRepository<Settings>> { SettingsRepository() }
}
