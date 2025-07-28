package network.bisq.mobile.domain.data.repository

import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.model.TradeReadState
import network.bisq.mobile.domain.data.model.User
import network.bisq.mobile.domain.data.persistance.KeyValueStorage

// this way of defining supports both platforms
// add your repositories here and then in your DI module call this classes for instanciation
open class SettingsRepository(keyValueStorage: KeyValueStorage<Settings>) : SingleObjectRepository<Settings>(keyValueStorage, Settings())
open class TradeReadStateRepository(keyValueStorage: KeyValueStorage<TradeReadState>) : SingleObjectRepository<TradeReadState>(keyValueStorage, TradeReadState())

open class UserRepository(keyValueStorage: KeyValueStorage<User>) : SingleObjectRepository<User>(keyValueStorage, User()) {
    suspend fun updateLastActivity(): User? {
        return withContext(IODispatcher) {
            val user = fetch()
            if (user != null) {
                update(user.apply {
                    lastActivity = Clock.System.now().toEpochMilliseconds()
                })
            }
            user
        }
    }
}