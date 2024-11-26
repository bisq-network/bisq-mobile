package network.bisq.mobile.domain.data.persistance

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import network.bisq.mobile.domain.data.model.BaseModel

/**
 * Multi platform key-value storage ("settings")
 */
class KeyValueStorage<T : BaseModel>(
    private val settings: Settings,
    private val keyPrefix: String,
    private val serializer: (t: T) -> String,
    private val deserializer: (String) -> T
) : PersistenceSource<T> {

    override suspend fun save(item: T) {
        settings[keyPrefix + item.id] = serializer(item)
    }

    override suspend fun saveAll(items: List<T>) {
        items.forEach { save(it) }
    }

    override suspend fun get(id: String?): T? {
        // Assume a single item by some predefined ID (or modify logic for specific use cases)
        try {
            val key = settings.keys.firstOrNull { it.startsWith(keyPrefix + (id ?: "")) }
            return key?.let { deserializer(settings.getStringOrNull(it)!!) }
        } catch (e: Exception) {
            throw IllegalArgumentException("No saved object with id $id")
        }
    }

    override suspend fun getAll(): List<T> {
        return settings.keys
            .filter { it.startsWith(keyPrefix) }
            .mapNotNull { settings.getStringOrNull(it)?.let(deserializer) }
    }

    override suspend fun delete(item: T) {
        settings.remove(keyPrefix + item.id)
    }

    override suspend fun deleteAll() {
        settings.keys
            .filter { it.startsWith(keyPrefix) }
            .forEach { settings.remove(it) }
    }

    override suspend fun clear() {
        settings.clear()
    }
}