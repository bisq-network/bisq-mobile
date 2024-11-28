package network.bisq.mobile.domain.data.persistance

import network.bisq.mobile.domain.data.model.BaseModel

/**
 * BaseModel related persistance source.
 * Information for kets is derived from the involved runtime base model instance.
 */
interface PersistenceSource<T: BaseModel> {
    suspend fun save(item: T)
    suspend fun saveAll(items: List<T>)

    /**
     * @param prototype: of the object to search for. The prototype class and BaseModel#id is used to find the right obj data.
     */
    suspend fun get(prototype: T): T?
    suspend fun getAll(prototype: T): List<T>
    suspend fun delete(item: T)
    suspend fun deleteAll(prototype: T)
    suspend fun clear()
}