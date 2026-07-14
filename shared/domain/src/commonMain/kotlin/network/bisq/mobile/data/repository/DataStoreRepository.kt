package network.bisq.mobile.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import network.bisq.mobile.domain.utils.Logging

/**
 * Base class for DataStore-backed repositories.
 *
 * Provides the [data] flow that falls back to [createDefault] when the datastore
 * fails to read with an [IOException], plus [set] for one-line field updates and
 * a default [clear] implementation.
 */
abstract class DataStoreRepository<T>(
    protected val store: DataStore<T>,
) : Logging {
    protected abstract fun createDefault(): T

    val data: Flow<T>
        get() =
            store.data.catch { exception ->
                if (exception is IOException) {
                    log.e("Error reading datastore", exception)
                    emit(createDefault())
                } else {
                    throw exception
                }
            }

    protected suspend fun set(transform: suspend (T) -> T) {
        store.updateData(transform)
    }

    open suspend fun clear() {
        store.updateData { createDefault() }
    }
}
