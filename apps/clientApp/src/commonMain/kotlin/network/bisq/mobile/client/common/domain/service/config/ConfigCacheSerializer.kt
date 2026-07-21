package network.bisq.mobile.client.common.domain.service.config

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.okio.OkioSerializer
import kotlinx.serialization.SerializationException
import network.bisq.mobile.data.datastore.dataStoreJson
import okio.BufferedSink
import okio.BufferedSource

/**
 * Plaintext serializer for [ConfigCache]. The cached data is version-global static config plus a
 * hash of the node host (no raw onion, no secrets), so it does not need encryption.
 */
object ConfigCacheSerializer : OkioSerializer<ConfigCache> {
    override val defaultValue: ConfigCache
        get() = ConfigCache()

    override suspend fun readFrom(source: BufferedSource): ConfigCache {
        if (source.exhausted()) return defaultValue
        return try {
            dataStoreJson.decodeFromString(ConfigCache.serializer(), source.readUtf8())
        } catch (e: SerializationException) {
            throw CorruptionException("Cannot deserialize ConfigCache", e)
        } catch (e: IllegalArgumentException) {
            throw CorruptionException("Cannot read ConfigCache", e)
        }
    }

    override suspend fun writeTo(
        t: ConfigCache,
        sink: BufferedSink,
    ) {
        sink.writeUtf8(dataStoreJson.encodeToString(ConfigCache.serializer(), t))
    }
}
