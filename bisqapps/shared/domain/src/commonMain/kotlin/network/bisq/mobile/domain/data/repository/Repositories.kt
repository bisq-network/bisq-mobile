package network.bisq.mobile.domain.data.repository

import network.bisq.mobile.domain.data.model.*

// this way of definingsupports both platforms
// add your repositories here and then in your DI module call this classes for instanciation
open class GreetingRepository<T: Greeting>: SingleObjectRepository<T>()
open class BisqStatsRepository<T: BisqStats>: SingleObjectRepository<T>()
open class BtcPriceRepository<T: BtcPrice>: SingleObjectRepository<T>()
open class UserProfileRepository<T: UserProfile>: SingleObjectRepository<T>()
open class SettingsRepository<T: Settings>: SingleObjectRepository<T>()
