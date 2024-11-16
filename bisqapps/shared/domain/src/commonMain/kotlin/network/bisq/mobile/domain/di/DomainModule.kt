package network.bisq.mobile.domain.di

import network.bisq.mobile.domain.data.model.BtcPrice
import network.bisq.mobile.domain.data.model.BisqStats
import network.bisq.mobile.domain.data.model.Greeting
import network.bisq.mobile.domain.data.model.NetworkModel
import network.bisq.mobile.domain.data.repository.BisqStatsRepository
import network.bisq.mobile.domain.data.repository.BtcPriceRepository
import network.bisq.mobile.domain.data.repository.GreetingRepository
import network.bisq.mobile.domain.data.repository.NetworkRepository
import org.koin.dsl.module

val domainModule = module {
    single<GreetingRepository<Greeting>> { GreetingRepository() }
    single<BisqStatsRepository<BisqStats>> { BisqStatsRepository() }
    single<BtcPriceRepository<BtcPrice>> { BtcPriceRepository() }
    single<NetworkRepository<NetworkModel>> { NetworkRepository() }
}
