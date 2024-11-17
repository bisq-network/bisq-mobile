package network.bisq.mobile.domain.di

import network.bisq.mobile.domain.data.model.*
import network.bisq.mobile.domain.data.repository.*
import org.koin.dsl.module

val domainModule = module {
    single<GreetingRepository<Greeting>> { GreetingRepository() }
    single<BisqStatsRepository> { BisqStatsRepository() }
    single<BtcPriceRepository> { BtcPriceRepository() }
    single<UserProfileRepository> { UserProfileRepository() }
    single<SettingsRepository> { SettingsRepository() }
}
