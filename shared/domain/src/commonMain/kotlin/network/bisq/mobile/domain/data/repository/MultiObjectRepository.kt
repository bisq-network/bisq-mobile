package network.bisq.mobile.domain.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.model.BaseModel
import network.bisq.mobile.domain.data.persistance.PersistenceSource
import network.bisq.mobile.domain.utils.Logging

/**
 * Repository implementation for multiple objects. Allows for persistence if the persistence source is provided,
 * otherwise it's memory-only.
 *
 * @param T a domain model that extends BaseModel
 * @param persistenceSource <optional> persistence mechanism to use to save/load data for this repository. Otherwise it's mem-only.
 * @param prototype <optional> an instance of T to use as prototype, can be null if no persistence source will be used
 */
abstract class MultiObjectRepository<out T : BaseModel>(
    private val persistenceSource: PersistenceSource<T>? = null,
    private val prototype: T? = null,
) : Logging {

    private val _dataMap = MutableStateFlow<Map<String, T>>(emptyMap())
    val dataMap: StateFlow<Map<String, T>> get() = _dataMap.asStateFlow()

    private val job = Job()
    private val scope = CoroutineScope(job + IODispatcher)

    /**
     * Creates a new object in the repository.
     * @param data The object to create
     */
    suspend fun create(data: @UnsafeVariance T) {
        if (data.id.isBlank()) {
            throw IllegalArgumentException("Cannot create an object with a blank ID")
        }
        _dataMap.update { it + (data.id to data) }
        persistenceSource?.save(data)
    }

    /**
     * Updates an existing object in the repository.
     * @param data The object to update
     */
    suspend fun update(data: @UnsafeVariance T) {
        if (data.id.isBlank()) {
            throw IllegalArgumentException("Cannot update an object with a blank ID")
        }
        _dataMap.update { it + (data.id to data) }
        persistenceSource?.save(data)
    }

    /**
     * Deletes an object from the repository.
     * @param data The object to delete
     */
    suspend fun delete(data: @UnsafeVariance T) {
        if (data.id.isBlank()) {
            throw IllegalArgumentException("Cannot delete an object with a blank ID")
        }
        _dataMap.update { it - data.id }
        persistenceSource?.delete(data)
    }

    /**
     * Creates multiple objects in the repository in a single operation.
     * @param items The objects to create
     */
    suspend fun createAll(items: List<@UnsafeVariance T>) {
        val itemsWithValidIds = items.filter { it.id.isNotBlank() }
        if (itemsWithValidIds.size != items.size) {
            throw IllegalArgumentException("Cannot create objects with blank IDs")
        }

        val newEntries = itemsWithValidIds.associateBy { it.id }
        _dataMap.update { it + newEntries }
        persistenceSource?.let { source ->
            itemsWithValidIds.forEach { source.save(it) }
        }
    }

    /**
     * Deletes multiple objects from the repository in a single operation.
     * @param items The objects to delete
     */
    suspend fun deleteAll(items: List<@UnsafeVariance T>) {
        val idsToRemove = items.map { it.id }.filter { it.isNotBlank() }
        _dataMap.update { it - idsToRemove.toSet() }
        persistenceSource?.let { source ->
            items.forEach { source.delete(it) }
        }
    }

    /**
     * Fetches all objects from the repository.
     * @return A list of all objects
     */
    suspend fun fetchAll(): List<T> {
        if (_dataMap.value.isEmpty() && persistenceSource != null && prototype != null) {
            val items = persistenceSource.getAll(prototype)
            val newMap = items.associateBy { it.id }
            _dataMap.update { newMap }
        }
        return _dataMap.value.values.toList()
    }

    /**
     * Fetches a specific object by ID.
     * @param id The ID of the object to fetch
     * @return The object with the specified ID, or null if not found
     */
    suspend fun fetchById(id: String): T? {
        if (_dataMap.value.isEmpty() && persistenceSource != null && prototype != null) {
            fetchAll() // Load all data first
        }
        return _dataMap.value[id]
    }

    /**
     * Clears all objects from the repository.
     * Note: This method cancels the internal coroutine scope, making the repository
     * instance unusable after calling this method. Create a new instance if needed.
     */
    suspend fun clear() {
        try {
            persistenceSource?.clear()
            scope.cancel()
        } catch (e: Exception) {
            log.e("Failed to cancel repository coroutine scope", e)
        } finally {
            _dataMap.update { emptyMap() }
        }
    }
}