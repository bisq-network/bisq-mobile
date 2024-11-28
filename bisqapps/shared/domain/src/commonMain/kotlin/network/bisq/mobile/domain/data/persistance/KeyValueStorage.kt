package network.bisq.mobile.domain.data.persistance

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import network.bisq.mobile.domain.data.model.BaseModel

/**
 * Multi platform key-value storage ("settings") linked to the usage of our BaseModels.
 * The key for the key-value is exctracted from the model based on its final runtime class and an id if available
 * Hence why some methods will require a prototype of the model being used.
 * This allows us to reuse a single instance of this storage in different repositories without having data collision
 * If you are going to persist only one obj of T, you can just leave the id with default BaseModel#UNDEFINED_ID value.
 */
class KeyValueStorage<T : BaseModel>(
    private val settings: Settings,
    private val serializer: (t: T) -> String,
    private val deserializer: (String) -> T
) : PersistenceSource<T> {

    override suspend fun save(item: T) {
        settings[generateKey(item)] = serializer(item)
    }

    override suspend fun saveAll(items: List<T>) {
        items.forEach { save(it) }
    }

    /**
     * @param prototype the prototype of the object you want to retrieve, which the id you are looking
     * for or BaseModel.UNDEFINED_ID to get the generic settings associated with the class key
     * @throws IllegalArgumentException if no object found with that key
     */
    override suspend fun get(prototype: T): T? {
        // Assume a single item by some predefined ID (or modify logic for specific use cases)
        val searchKey = generateKey(prototype)
        try {
            val key = settings.keys.firstOrNull { it == searchKey }
            return key?.let { deserializer(settings.getStringOrNull(it)!!) }
        } catch (e: Exception) {
            throw IllegalArgumentException("No saved object with id $searchKey")
        }
    }

    override suspend fun getAll(prototype: T): List<T> {
        return settings.keys
            .filter { it.startsWith(generatePrefix(prototype)) }
            .mapNotNull { settings.getStringOrNull(it)?.let(deserializer) }
    }

    override suspend fun delete(item: T) {
        settings.remove(generateKey(item))
    }

    override suspend fun deleteAll(prototype: T) {
        settings.keys
            .filter { it.startsWith(generatePrefix(prototype)) }
            .forEach { settings.remove(it) }
    }

    override suspend fun clear() {
        settings.clear()
    }

    private fun generatePrefix(t: T) = t::class.simpleName ?: "BaseModel"
    private fun generateKey(t: T) = "${generatePrefix(t)}_${t.id}"
}