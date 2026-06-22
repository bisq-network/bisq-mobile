package network.bisq.mobile.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bisq.mobile.data.model.offerbook.OfferbookFilterConfig
import network.bisq.mobile.data.model.offerbook.OfferbookFilterConfigs
import network.bisq.mobile.domain.repository.OfferbookFilterConfigRepository
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.Logging

class OfferbookFilterConfigRepositoryImpl(
    private val offerbookFilterConfigsStore: DataStore<OfferbookFilterConfigs>,
    private val settingsRepository: SettingsRepository,
    jobsManager: CoroutineJobsManager,
) : OfferbookFilterConfigRepository,
    Logging {
    private val initialized = CompletableDeferred<Unit>()
    private val _data = MutableStateFlow(OfferbookFilterConfigs())
    private var rememberOfferbookFilterPreferences: Boolean = true

    override val data: Flow<OfferbookFilterConfigs> =
        flow {
            initialized.await()
            emitAll(_data)
        }

    init {
        jobsManager.getScope().launch {
            initializeSessionConfig()
            observeRememberFilterPreferencesChanges()
        }
    }

    override suspend fun getConfig(marketKey: String): OfferbookFilterConfig {
        initialized.await()
        return _data.value.configsByMarket[marketKey] ?: OfferbookFilterConfig()
    }

    override suspend fun setConfig(
        marketKey: String,
        config: OfferbookFilterConfig,
    ) {
        require(marketKey.isNotBlank()) { "marketKey cannot be blank" }
        initialized.await()
        _data.update {
            it.copy(configsByMarket = it.configsByMarket + (marketKey to config))
        }
        persistCurrentSessionConfigIfEnabled()
    }

    private suspend fun initializeSessionConfig() {
        val enabled =
            runCatching { settingsRepository.fetch().rememberOfferbookFilterPreferences }
                .onFailure { log.w(it) { "Failed to read offerbook filter persistence setting; using in-memory defaults" } }
                .getOrDefault(rememberOfferbookFilterPreferences)
        rememberOfferbookFilterPreferences = enabled
        if (enabled) {
            _data.value = readPersistedConfigs()
        } else {
            _data.value = OfferbookFilterConfigs()
            clearPersisted()
        }
        initialized.complete(Unit)
    }

    private suspend fun observeRememberFilterPreferencesChanges() {
        settingsRepository.data
            .map { it.rememberOfferbookFilterPreferences }
            .distinctUntilChanged()
            .catch { exception ->
                log.w(exception) { "Failed to observe offerbook filter persistence setting; keeping last known setting" }
            }.collect { enabled ->
                initialized.await()
                if (enabled == rememberOfferbookFilterPreferences) return@collect
                rememberOfferbookFilterPreferences = enabled
                if (enabled) {
                    persistCurrentSessionConfig()
                } else {
                    clearPersisted()
                }
            }
    }

    private suspend fun persistCurrentSessionConfigIfEnabled() {
        if (rememberOfferbookFilterPreferences) {
            persistCurrentSessionConfig()
        }
    }

    private suspend fun persistCurrentSessionConfig() {
        val current = _data.value
        runCatching { offerbookFilterConfigsStore.updateData { current } }
            .onFailure { log.w(it) { "Failed to persist offerbook filter configs; keeping in-memory state" } }
    }

    private suspend fun clearPersisted() {
        runCatching { offerbookFilterConfigsStore.updateData { OfferbookFilterConfigs() } }
            .onFailure { log.w(it) { "Failed to clear persisted offerbook filter configs; keeping in-memory state" } }
    }

    private suspend fun readPersistedConfigs(): OfferbookFilterConfigs =
        runCatching {
            offerbookFilterConfigsStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        log.e("Error reading OfferbookFilterConfigs datastore", exception)
                        emit(OfferbookFilterConfigs())
                    } else {
                        throw exception
                    }
                }.first()
        }.onFailure { log.w(it) { "Failed to read persisted offerbook filter configs; using empty in-memory state" } }
            .getOrDefault(OfferbookFilterConfigs())
}
