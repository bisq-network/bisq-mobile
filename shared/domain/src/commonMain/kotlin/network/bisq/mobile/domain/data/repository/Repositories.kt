package network.bisq.mobile.domain.data.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
open class TradeReadStateRepository(keyValueStorage: KeyValueStorage<TradeReadState>) : MultiObjectRepository<TradeReadState>(keyValueStorage, TradeReadState("prototype", 0)) {

    private val updateMutex = Mutex()

    /**
     * Thread-safe method to get the read count for a specific trade.
     * @param tradeId The ID of the trade
     * @return The current read count for the trade, or 0 if not found
     */
    suspend fun getReadCount(tradeId: String): Int {
        return fetchById(tradeId)?.readCount ?: 0
    }

    /**
     * Thread-safe method to set the read count for a specific trade.
     * @param tradeId The ID of the trade
     * @param count The new read count
     */
    suspend fun setReadCount(tradeId: String, count: Int) {
        if (count < 0) return

        updateMutex.withLock {
            val tradeReadState = TradeReadState(tradeId, count)
            val existing = fetchById(tradeId)

            if (existing != null) {
                update(tradeReadState)
            } else {
                create(tradeReadState)
            }
        }
    }

    /**
     * Thread-safe method to increment the read count for a specific trade.
     * @param tradeId The ID of the trade
     * @return The new read count after incrementing
     */
    suspend fun incrementReadCount(tradeId: String): Int {
        return updateMutex.withLock {
            val currentCount = fetchById(tradeId)?.readCount ?: 0
            val newCount = currentCount + 1
            val tradeReadState = TradeReadState(tradeId, newCount)
            val existing = fetchById(tradeId)

            if (existing != null) {
                update(tradeReadState)
            } else {
                create(tradeReadState)
            }
            newCount
        }
    }

    /**
     * Thread-safe method to update read count only if the new value is greater than current.
     * This prevents race conditions where an older update overwrites a newer one.
     * @param tradeId The ID of the trade
     * @param newCount The new read count
     * @return True if the update was applied, false if the current count was already >= newCount
     */
    suspend fun updateReadCountIfGreater(tradeId: String, newCount: Int): Boolean {
        if (newCount < 0) return false

        return updateMutex.withLock {
            val currentCount = fetchById(tradeId)?.readCount ?: 0
            if (newCount > currentCount) {
                val tradeReadState = TradeReadState(tradeId, newCount)
                val existing = fetchById(tradeId)

                if (existing != null) {
                    update(tradeReadState)
                } else {
                    create(tradeReadState)
                }
                true
            } else {
                false
            }
        }
    }

    /**
     * Thread-safe method to clear the read state for a specific trade.
     * @param tradeId The ID of the trade
     */
    suspend fun clearReadState(tradeId: String) {
        updateMutex.withLock {
            val existing = fetchById(tradeId)
            if (existing != null) {
                delete(existing)
            }
        }
    }
}

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