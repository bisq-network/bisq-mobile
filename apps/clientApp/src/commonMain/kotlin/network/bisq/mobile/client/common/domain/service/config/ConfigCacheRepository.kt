package network.bisq.mobile.client.common.domain.service.config

/**
 * Disk persistence for the static config snapshot ([ConfigCacheEntry]). Survives restarts so the
 * client can render from the last successful fetch instantly and only re-fetch when the paired node's
 * API version changes.
 */
interface ConfigCacheRepository {
    suspend fun get(): ConfigCacheEntry?

    suspend fun set(entry: ConfigCacheEntry)

    suspend fun clear()
}
