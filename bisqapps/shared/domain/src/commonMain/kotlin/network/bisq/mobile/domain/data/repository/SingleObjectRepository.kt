package network.bisq.mobile.domain.data.repository

import co.touchlab.kermit.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.data.model.BaseModel
import network.bisq.mobile.domain.data.persistance.PersistenceSource

/**
 * Repository implementation for a single object. Allows for persistance if the persistance source if provided, otherwise is mem-only.
 *
 * TODO: create a map-based multi object repository when needed (might need to leverage some kind of id generation on the base model)
 */
abstract class SingleObjectRepository<out T : BaseModel>(
    private val persistenceSource: PersistenceSource<T>? = null
) : Repository<T> {

    private val logger = Logger.withTag(SingleObjectRepository::class.simpleName ?: "SingleObjectRepository")

    private val _data = MutableStateFlow<T?>(null)
    override val data: StateFlow<T?> = _data

    private val job = Job()
    private val scope = CoroutineScope(job + Dispatchers.IO)

    init {
        // Load from persistence on initialization if available
        persistenceSource?.let {
            scope.launch {
                _data.value = it.get()
            }
        }
    }

    override suspend fun create(data: @UnsafeVariance T) {
        _data.value = data
        persistenceSource?.save(data)
    }

    override suspend fun update(data: @UnsafeVariance T) {
        _data.value = data
        persistenceSource?.save(data)
    }

    override suspend fun delete(data: @UnsafeVariance T) {
        _data.value = null
        persistenceSource?.delete(data)
    }

    override suspend fun fetch(): T? {
        return _data.value ?: persistenceSource?.get().also { _data.value = it }
    }

    override suspend fun clear() {
        try {
            persistenceSource?.clear()
            scope.cancel()
        } catch (e: Exception) {
            logger.e("Failed to cancel repository coroutine scope", e)
        } finally {
            _data.value = null
        }
    }
}