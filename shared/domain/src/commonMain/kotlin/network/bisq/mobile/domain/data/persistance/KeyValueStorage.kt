package network.bisq.mobile.domain.data.persistance

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import network.bisq.mobile.domain.data.model.BaseModel
import network.bisq.mobile.domain.utils.getLogger

/**
 * Multi platform key-value storage ("settings") linked to the usage of our BaseModels.
 * The key for the key-value is extracted from the model based on its final runtime class and an id if available
 * Hence why some methods will require a prototype of the model being used.
 * This allows us to reuse a single instance of this storage in different repositories without having data collision
 * If you are going to persist only one obj of T, you can just leave the id with default BaseModel#UNDEFINED_ID value.
 *
 * Thread-safe: All operations are synchronized using a mutex to prevent race conditions.
 */
class KeyValueStorage<T : BaseModel>(
    private val settings: Settings,
    private val serializer: (t: T) -> String,
    private val deserializer: (String) -> T
) : PersistenceSource<T> {

    private val mutex = Mutex()

    override suspend fun save(item: T) {
        mutex.withLock {
            settings[generateKey(item)] = serializer(item)
        }
    }

    override suspend fun saveAll(items: List<T>) {
        mutex.withLock {
            items.forEach { item ->
                settings[generateKey(item)] = serializer(item)
            }
        }
    }

    /**
     * @param prototype the prototype of the object you want to retrieve, which the id you are looking
     * for or BaseModel.UNDEFINED_ID to get the generic settings associated with the class key
     * @throws IllegalArgumentException if no object found with that key
     */
    override suspend fun get(prototype: T): T? {
        return mutex.withLock {
            val searchKey = generateKey(prototype)
            try {
                val key = settings.keys.firstOrNull { it == searchKey }
                key?.let { deserializer(settings.getStringOrNull(it)!!) }
            } catch (e: Exception) {
                getLogger("KeyValueStorage").e { "No saved object with id $searchKey" }
                null
            }
        }
    }

    override suspend fun getAll(prototype: T): List<T> {
        return mutex.withLock {
            // Create a snapshot of keys to avoid concurrent modification during iteration
            val keysSnapshot = settings.keys.toList()
            keysSnapshot
                .filter { it.startsWith(generatePrefix(prototype)) }
                .mapNotNull { key ->
                    try {
                        settings.getStringOrNull(key)?.let(deserializer)
                    } catch (e: Exception) {
                        getLogger("KeyValueStorage").e { "Failed to deserialize item with key $key" }
                        null
                    }
                }
        }
    }

    override suspend fun delete(item: T) {
        mutex.withLock {
            settings.remove(generateKey(item))
        }
    }

    override suspend fun deleteAll(prototype: T) {
        mutex.withLock {
            // Create a snapshot of keys to avoid concurrent modification during iteration
            val keysToRemove = settings.keys.filter { it.startsWith(generatePrefix(prototype)) }.toList()
            keysToRemove.forEach { key ->
                settings.remove(key)
            }
        }
    }

    override suspend fun clear() {
        mutex.withLock {
            settings.clear()
        }
    }

    private fun generatePrefix(t: T) = t::class.simpleName ?: "BaseModel"
    private fun generateKey(t: T) = "${generatePrefix(t)}_${t.id}"
}