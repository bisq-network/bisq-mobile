package network.bisq.mobile.domain.data.repository

import network.bisq.mobile.domain.data.model.*

// this way of definingsupports both platforms
// add your repositories here and then in your DI module call this classes for instanciation
open class GreetingRepository<T: Greeting>: SingleObjectRepository<T>()
open class BisqStatsRepository: SingleObjectRepository<BisqStats>()
open class BtcPriceRepository: SingleObjectRepository<BtcPrice>()
open class UserProfileRepository: SingleObjectRepository<UserProfile>()
open class SettingsRepository: SingleObjectRepository<Settings>()
