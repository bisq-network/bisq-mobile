package network.bisq.mobile.domain.di

import network.bisq.mobile.domain.data.model.Greeting
import network.bisq.mobile.domain.data.repository.*
import org.koin.dsl.module

val domainModule = module {
    single<GreetingRepository<Greeting>> { GreetingRepository() }
    single<BisqStatsRepository> { BisqStatsRepository() }
    single<BtcPriceRepository> { BtcPriceRepository() }
    single<SettingsRepository> { SettingsRepository() }
    single<MyTradesRepository> { MyTradesRepository() }
}
