package network.bisq.mobile.domain.data.repository

import kotlinx.coroutines.runBlocking
import network.bisq.mobile.domain.data.model.*
import network.bisq.mobile.domain.data.model.BisqStats
import network.bisq.mobile.domain.data.model.BtcPrice
import network.bisq.mobile.domain.data.model.Greeting
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.persistance.KeyValueStorage

// this way of defining supports both platforms
// add your repositories here and then in your DI module call this classes for instanciation
open class GreetingRepository<T: Greeting>: SingleObjectRepository<T>()
open class BisqStatsRepository: SingleObjectRepository<BisqStats>()
open class BtcPriceRepository: SingleObjectRepository<BtcPrice>()

open class MyTradesRepository : SingleObjectRepository<MyTrades>() {
    init {
        runBlocking {
            val myTrades = MyTrades(
                trades = listOf(
                    BisqOffer(id = "offer1", isBuy = true, price = 95000, currency = "USD"),
                    BisqOffer(id = "offer2", isBuy = false, price = 96000, currency = "USD"),
                    BisqOffer(id = "offer3", isBuy = true, price = 97000, currency = "USD"),
                    BisqOffer(id = "offer4", isBuy = false, price = 98000, currency = "USD"),
                    BisqOffer(id = "offer5", isBuy = true, price = 99000, currency = "USD"),
                    BisqOffer(id = "offer1", isBuy = true, price = 95000, currency = "USD"),
                    BisqOffer(id = "offer2", isBuy = false, price = 96000, currency = "USD"),
                    BisqOffer(id = "offer3", isBuy = true, price = 97000, currency = "USD"),
                    BisqOffer(id = "offer4", isBuy = false, price = 98000, currency = "USD"),
                    BisqOffer(id = "offer5", isBuy = true, price = 99000, currency = "USD")
                )
            )

            create(myTrades)

            println("MyTradeRepo :: Created")
        }
    }
}
open class SettingsRepository(keyValueStorage: KeyValueStorage<Settings>): SingleObjectRepository<Settings>(keyValueStorage, Settings())
