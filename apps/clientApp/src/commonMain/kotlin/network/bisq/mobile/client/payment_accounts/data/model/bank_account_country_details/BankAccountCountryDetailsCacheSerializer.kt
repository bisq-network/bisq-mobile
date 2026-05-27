package network.bisq.mobile.client.payment_accounts.data.model.bank_account_country_details

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.okio.OkioSerializer
import kotlinx.serialization.SerializationException
import network.bisq.mobile.data.datastore.dataStoreJson
import okio.BufferedSink
import okio.BufferedSource

object BankAccountCountryDetailsCacheSerializer : OkioSerializer<BankAccountCountryDetailsCache> {
    override val defaultValue: BankAccountCountryDetailsCache
        get() = BankAccountCountryDetailsCache()

    override suspend fun readFrom(source: BufferedSource): BankAccountCountryDetailsCache {
        if (source.exhausted()) return defaultValue
        return try {
            dataStoreJson.decodeFromString(
                BankAccountCountryDetailsCache.serializer(),
                source.readUtf8(),
            )
        } catch (e: SerializationException) {
            throw CorruptionException("Cannot deserialize BankAccountCountryDetailsCache", e)
        } catch (e: IllegalArgumentException) {
            throw CorruptionException("Cannot read BankAccountCountryDetailsCache", e)
        }
    }

    override suspend fun writeTo(
        t: BankAccountCountryDetailsCache,
        sink: BufferedSink,
    ) {
        val payload = dataStoreJson.encodeToString(BankAccountCountryDetailsCache.serializer(), t)
        sink.writeUtf8(payload)
    }
}
