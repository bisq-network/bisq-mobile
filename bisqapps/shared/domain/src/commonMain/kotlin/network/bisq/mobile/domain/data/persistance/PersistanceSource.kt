package network.bisq.mobile.domain.data.persistance

import network.bisq.mobile.domain.data.model.BaseModel

interface PersistenceSource<T: BaseModel> {
    suspend fun save(item: T)
    suspend fun saveAll(items: List<T>)
    suspend fun get(): T?
    suspend fun getAll(): List<T>
    suspend fun delete(item: T)
    suspend fun deleteAll()
    suspend fun clear()
}