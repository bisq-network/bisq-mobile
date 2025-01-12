package network.bisq.mobile.domain.data.repository

import network.bisq.mobile.domain.data.model.BisqStats
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.model.User
import network.bisq.mobile.domain.data.persistance.KeyValueStorage

// this way of defining supports both platforms
// add your repositories here and then in your DI module call this classes for instanciation
open class BisqStatsRepository : SingleObjectRepository<BisqStats>()
open class SettingsRepository(keyValueStorage: KeyValueStorage<Settings>) : SingleObjectRepository<Settings>(keyValueStorage, Settings())
open class UserRepository(keyValueStorage: KeyValueStorage<User>) : SingleObjectRepository<User>(keyValueStorage, User())
