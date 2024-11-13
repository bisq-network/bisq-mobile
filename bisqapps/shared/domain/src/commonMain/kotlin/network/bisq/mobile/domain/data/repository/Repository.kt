package network.bisq.mobile.domain.data.repository

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.data.model.BaseModel

/**
 * Behaviour definition for a BisqApps Domain Repository
 * A repository should have a in memory observable cached object and all of the CRUD operations forcing the use of coroutines
 */
interface Repository<out T: BaseModel> {
    val data: StateFlow<T?>

    /**
     * Creates the data in the repository. If data already exists, an exception is thrown.
     * @throws IllegalStateException if the data is already created
     */
    suspend fun create(data: @UnsafeVariance T)

    /**
     * Fetches the data from the repository whatever this is
     */
    suspend fun fetch(): T?

    /**
     * Updates the data of this repository. If existent, it will be replaced
     */
    suspend fun update(data: @UnsafeVariance T)

    /**
     * Deletes the data from the respository
     */
    suspend fun delete(data: @UnsafeVariance T)

    /**
     * Cancel any ongoing operation.
     */
    suspend fun clear()

    /**
     * @return true if data is set, false otherwise
     */
    fun exists() = this.data.value != null
}