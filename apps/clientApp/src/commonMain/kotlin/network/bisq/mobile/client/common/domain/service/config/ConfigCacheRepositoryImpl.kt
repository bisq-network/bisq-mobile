package network.bisq.mobile.client.common.domain.service.config

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import network.bisq.mobile.data.repository.DataStoreRepository

/**
 * DataStore container for the config cache. Wraps a nullable [ConfigCacheEntry] so the store has a
 * usable default (empty) before the first successful fetch.
 */
@Serializable
data class ConfigCache(
    val entry: ConfigCacheEntry? = null,
)

class ConfigCacheRepositoryImpl(
    store: DataStore<ConfigCache>,
) : DataStoreRepository<ConfigCache>(store),
    ConfigCacheRepository {
    override fun createDefault() = ConfigCache()

    override suspend fun get(): ConfigCacheEntry? = data.first().entry

    override suspend fun set(entry: ConfigCacheEntry) = set { it.copy(entry = entry) }

    override suspend fun clear() = set { ConfigCache() }
}
