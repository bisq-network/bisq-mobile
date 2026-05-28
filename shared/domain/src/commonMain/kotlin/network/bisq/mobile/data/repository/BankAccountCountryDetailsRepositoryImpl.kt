package network.bisq.mobile.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.data.model.account.bank_account.BankAccountCountryDetailsCache
import network.bisq.mobile.data.model.account.fiat.BankAccountCountryDetailsDto
import network.bisq.mobile.domain.repository.BankAccountCountryDetailsRepository
import network.bisq.mobile.domain.utils.Logging

class BankAccountCountryDetailsRepositoryImpl(
    private val cacheStore: DataStore<BankAccountCountryDetailsCache>,
) : BankAccountCountryDetailsRepository,
    Logging {
    private val mutex = Mutex()
    private var detailsByCountryCode: Map<String, BankAccountCountryDetailsDto> = emptyMap()
    private var loadedApiVersion: String? = null

    override suspend fun get(
        countryCode: String,
        refresh: suspend () -> List<BankAccountCountryDetailsDto>,
    ): BankAccountCountryDetailsDto {
        val normalizedCountryCode = normalizeCountryCode(countryCode)
        require(normalizedCountryCode.isNotBlank()) { "countryCode cannot be blank" }

        return mutex.withLock {
            ensureLoaded(refresh)
            detailsByCountryCode[normalizedCountryCode]
                ?: error("Bank account country details not found for country code: $normalizedCountryCode")
        }
    }

    private suspend fun ensureLoaded(refresh: suspend () -> List<BankAccountCountryDetailsDto>) {
        if (isMemoryCacheValid()) return

        val cached = readCache()
        if (cached.isValid()) {
            loadedApiVersion = cached.apiVersion
            detailsByCountryCode = cached.detailsByCountryCode.normalizeKeys()
            return
        }

        val refreshed = refresh().associateByNormalizedCountryCode()
        cacheStore.updateData {
            BankAccountCountryDetailsCache(
                apiVersion = BuildConfig.BISQ_API_VERSION,
                detailsByCountryCode = refreshed,
            )
        }
        loadedApiVersion = BuildConfig.BISQ_API_VERSION
        detailsByCountryCode = refreshed
    }

    private suspend fun readCache(): BankAccountCountryDetailsCache =
        try {
            cacheStore.data.first()
        } catch (exception: IOException) {
            log.e("Error reading BankAccountCountryDetailsCache datastore", exception)
            BankAccountCountryDetailsCache()
        }

    private fun isMemoryCacheValid(): Boolean = loadedApiVersion == BuildConfig.BISQ_API_VERSION && detailsByCountryCode.isNotEmpty()

    private fun BankAccountCountryDetailsCache.isValid(): Boolean = apiVersion == BuildConfig.BISQ_API_VERSION && detailsByCountryCode.isNotEmpty()

    private fun List<BankAccountCountryDetailsDto>.associateByNormalizedCountryCode(): Map<String, BankAccountCountryDetailsDto> = associateBy { normalizeCountryCode(it.country.code) }

    private fun Map<String, BankAccountCountryDetailsDto>.normalizeKeys(): Map<String, BankAccountCountryDetailsDto> = entries.associate { (countryCode, details) -> normalizeCountryCode(countryCode) to details }

    private fun normalizeCountryCode(countryCode: String): String = countryCode.trim().uppercase()
}
