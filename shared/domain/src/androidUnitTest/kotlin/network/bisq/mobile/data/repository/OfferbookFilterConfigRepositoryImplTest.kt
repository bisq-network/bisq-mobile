package network.bisq.mobile.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.data.model.BatteryOptimizationState
import network.bisq.mobile.data.model.PermissionState
import network.bisq.mobile.data.model.Settings
import network.bisq.mobile.data.model.market.MarketFilter
import network.bisq.mobile.data.model.market.MarketSortBy
import network.bisq.mobile.data.model.offerbook.OfferbookFilterConfig
import network.bisq.mobile.data.model.offerbook.OfferbookFilterConfigs
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class OfferbookFilterConfigRepositoryImplTest {
    private class InMemoryDataStore<T>(
        initialValue: T,
    ) : DataStore<T> {
        private var persistedValue = initialValue
        private val state = MutableStateFlow(initialValue)
        override val data: Flow<T> = state

        override suspend fun updateData(transform: suspend (t: T) -> T): T {
            val updated = transform(persistedValue)
            persistedValue = updated
            state.value = updated
            return updated
        }

        fun fetch(): T = persistedValue
    }

    private class ThrowingDataStore<T>(
        private val exception: Throwable,
    ) : DataStore<T> {
        override val data: Flow<T> = flow { throw exception }

        override suspend fun updateData(transform: suspend (t: T) -> T): T = throw exception
    }

    private class RecoveringDataStore<T>(
        initialValue: T,
        private var remainingWriteFailures: Int = 0,
    ) : DataStore<T> {
        private var persistedValue = initialValue
        private val state = MutableStateFlow(initialValue)
        override val data: Flow<T> = state

        override suspend fun updateData(transform: suspend (t: T) -> T): T {
            if (remainingWriteFailures > 0) {
                remainingWriteFailures--
                throw IOException("write failed")
            }
            val updated = transform(persistedValue)
            persistedValue = updated
            state.value = updated
            return updated
        }

        fun fetch(): T = persistedValue
    }

    private class FakeSettingsRepository(
        initialSettings: Settings = Settings(),
        private val fetchException: Throwable? = null,
    ) : SettingsRepository {
        private val state = MutableStateFlow(initialSettings)
        override val data: StateFlow<Settings> = state.asStateFlow()

        override suspend fun fetch(): Settings = fetchException?.let { throw it } ?: state.value

        override suspend fun setFirstLaunch(value: Boolean) {
            update { it.copy(firstLaunch = value) }
        }

        override suspend fun setShowChatRulesWarnBox(value: Boolean) {
            update { it.copy(showChatRulesWarnBox = value) }
        }

        override suspend fun setSelectedMarketCode(value: String) {
            update { it.copy(selectedMarketCode = value) }
        }

        override suspend fun setNotificationPermissionState(value: PermissionState) {
            update { it.copy(notificationPermissionState = value) }
        }

        override suspend fun setBatteryOptimizationPermissionState(value: BatteryOptimizationState) {
            update { it.copy(batteryOptimizationState = value) }
        }

        override suspend fun update(transform: suspend (t: Settings) -> Settings) {
            state.value = transform(state.value)
        }

        override suspend fun clear() {
            state.value = Settings()
        }

        override suspend fun setMarketSortBy(value: MarketSortBy) {
            update { it.copy(marketSortBy = value) }
        }

        override suspend fun setMarketFilter(value: MarketFilter) {
            update { it.copy(marketFilter = value) }
        }

        override suspend fun setDontShowAgainHyperlinksOpenInBrowser(value: Boolean) {
            update { it.copy(dontShowAgainHyperlinksOpenInBrowser = value) }
        }

        override suspend fun setPermitOpeningBrowser(value: Boolean) {
            update { it.copy(cookiePermitOpeningBrowser = value) }
        }

        override suspend fun setAnalyticsEnabled(value: Boolean) {
            update { it.copy(analyticsEnabled = value) }
        }

        override suspend fun setAnalyticsPromptSeen(value: Boolean) {
            update { it.copy(analyticsPromptSeen = value) }
        }

        override suspend fun setAnalyticsBaselineSent(value: Boolean) {
            update { it.copy(analyticsBaselineSent = value) }
        }

        override suspend fun setRememberOfferbookFilterPreferences(value: Boolean) {
            update { it.copy(rememberOfferbookFilterPreferences = value) }
        }
    }

    private class TestCoroutineJobsManager(
        private val scope: CoroutineScope,
    ) : CoroutineJobsManager {
        override var coroutineExceptionHandler: ((Throwable) -> Unit)? = null

        override suspend fun dispose() {
            scope.cancel()
        }

        override fun getScope(): CoroutineScope = scope
    }

    private data class Fixture(
        val repository: OfferbookFilterConfigRepositoryImpl,
        val store: InMemoryDataStore<OfferbookFilterConfigs>,
        val settingsRepository: FakeSettingsRepository,
        val jobsManager: TestCoroutineJobsManager,
    )

    private fun fixture(
        scope: CoroutineScope,
        persistedConfigs: OfferbookFilterConfigs = OfferbookFilterConfigs(),
        rememberFilters: Boolean = true,
    ): Fixture {
        val store = InMemoryDataStore(persistedConfigs)
        val settingsRepository =
            FakeSettingsRepository(Settings(rememberOfferbookFilterPreferences = rememberFilters))
        val jobsManager = TestCoroutineJobsManager(scope)
        val repository =
            OfferbookFilterConfigRepositoryImpl(
                offerbookFilterConfigsStore = store,
                settingsRepository = settingsRepository,
                jobsManager = jobsManager,
            )
        return Fixture(repository, store, settingsRepository, jobsManager)
    }

    private fun config(
        paymentMethods: Set<String> = setOf("SEPA"),
        settlementMethods: Set<String> = setOf("MAIN_CHAIN"),
        onlyMyOffers: Boolean = false,
    ) = OfferbookFilterConfig(
        selectedPaymentMethodIds = paymentMethods,
        selectedSettlementMethodIds = settlementMethods,
        onlyMyOffers = onlyMyOffers,
        hasManualPaymentFilter = true,
        hasManualSettlementFilter = true,
    )

    @Test
    fun `initializes session state from persisted configs when remembering filters is enabled`() =
        runTest {
            val persistedConfig = config(paymentMethods = setOf("WISE"))
            val fixture =
                fixture(
                    scope = CoroutineScope(StandardTestDispatcher(testScheduler) + SupervisorJob()),
                    persistedConfigs = OfferbookFilterConfigs(mapOf("BTC/EUR" to persistedConfig)),
                    rememberFilters = true,
                )

            advanceUntilIdle()

            assertEquals(persistedConfig, fixture.repository.getConfig("BTC/EUR"))
            assertEquals(OfferbookFilterConfig(), fixture.repository.getConfig("BTC/USD"))
            assertEquals(OfferbookFilterConfigs(mapOf("BTC/EUR" to persistedConfig)), fixture.store.fetch())
        }

    @Test
    fun `data emits initialized session configs`() =
        runTest {
            val persistedConfig = config(paymentMethods = setOf("WISE"))
            val persistedConfigs = OfferbookFilterConfigs(mapOf("BTC/EUR" to persistedConfig))
            val fixture =
                fixture(
                    scope = CoroutineScope(StandardTestDispatcher(testScheduler) + SupervisorJob()),
                    persistedConfigs = persistedConfigs,
                    rememberFilters = true,
                )
            advanceUntilIdle()

            assertEquals(persistedConfigs, fixture.repository.data.first())
        }

    @Test
    fun `initializes with empty configs when persisted DataStore read throws IOException`() =
        runTest {
            val store = ThrowingDataStore<OfferbookFilterConfigs>(IOException("read failed"))
            val settingsRepository = FakeSettingsRepository(Settings(rememberOfferbookFilterPreferences = true))
            val repository =
                OfferbookFilterConfigRepositoryImpl(
                    offerbookFilterConfigsStore = store,
                    settingsRepository = settingsRepository,
                    jobsManager = TestCoroutineJobsManager(CoroutineScope(StandardTestDispatcher(testScheduler) + SupervisorJob())),
                )

            advanceUntilIdle()

            assertEquals(OfferbookFilterConfigs(), repository.data.first())
        }

    @Test
    fun `settings fetch failure falls back to in-memory configs and allows future writes`() =
        runTest {
            val store = InMemoryDataStore(OfferbookFilterConfigs())
            val repository =
                OfferbookFilterConfigRepositoryImpl(
                    offerbookFilterConfigsStore = store,
                    settingsRepository = FakeSettingsRepository(fetchException = IllegalStateException("settings unavailable")),
                    jobsManager = TestCoroutineJobsManager(CoroutineScope(StandardTestDispatcher(testScheduler) + SupervisorJob())),
                )
            advanceUntilIdle()
            val usdConfig = config(paymentMethods = setOf("ZELLE"), onlyMyOffers = true)

            assertEquals(OfferbookFilterConfigs(), repository.data.first())
            repository.setConfig("BTC/USD", usdConfig)
            advanceUntilIdle()

            assertEquals(usdConfig, repository.getConfig("BTC/USD"))
            assertEquals(OfferbookFilterConfigs(mapOf("BTC/USD" to usdConfig)), store.fetch())
        }

    @Test
    fun `setConfig persists session config when remembering filters is enabled`() =
        runTest {
            val fixture = fixture(scope = CoroutineScope(StandardTestDispatcher(testScheduler) + SupervisorJob()), rememberFilters = true)
            advanceUntilIdle()
            val usdConfig = config(paymentMethods = setOf("ZELLE"), onlyMyOffers = true)

            fixture.repository.setConfig("BTC/USD", usdConfig)
            advanceUntilIdle()

            assertEquals(usdConfig, fixture.repository.getConfig("BTC/USD"))
            assertEquals(OfferbookFilterConfigs(mapOf("BTC/USD" to usdConfig)), fixture.store.fetch())
        }

    @Test
    fun `setConfig keeps in-memory config when persistence write fails`() =
        runTest {
            val store = ThrowingDataStore<OfferbookFilterConfigs>(IOException("write failed"))
            val repository =
                OfferbookFilterConfigRepositoryImpl(
                    offerbookFilterConfigsStore = store,
                    settingsRepository = FakeSettingsRepository(Settings(rememberOfferbookFilterPreferences = true)),
                    jobsManager = TestCoroutineJobsManager(CoroutineScope(StandardTestDispatcher(testScheduler) + SupervisorJob())),
                )
            advanceUntilIdle()
            val usdConfig = config(paymentMethods = setOf("ZELLE"), onlyMyOffers = true)

            repository.setConfig("BTC/USD", usdConfig)
            advanceUntilIdle()

            assertEquals(usdConfig, repository.getConfig("BTC/USD"))
            assertEquals(OfferbookFilterConfigs(mapOf("BTC/USD" to usdConfig)), repository.data.first())
        }

    @Test
    fun `setConfig retries persistence on later writes after an earlier write failure`() =
        runTest {
            val store = RecoveringDataStore(OfferbookFilterConfigs(), remainingWriteFailures = 1)
            val repository =
                OfferbookFilterConfigRepositoryImpl(
                    offerbookFilterConfigsStore = store,
                    settingsRepository = FakeSettingsRepository(Settings(rememberOfferbookFilterPreferences = true)),
                    jobsManager = TestCoroutineJobsManager(CoroutineScope(StandardTestDispatcher(testScheduler) + SupervisorJob())),
                )
            advanceUntilIdle()
            val firstConfig = config(paymentMethods = setOf("ZELLE"), onlyMyOffers = true)
            val secondConfig = config(paymentMethods = setOf("ACH"), onlyMyOffers = false)

            repository.setConfig("BTC/USD", firstConfig)
            advanceUntilIdle()
            assertEquals(firstConfig, repository.getConfig("BTC/USD"))
            assertEquals(OfferbookFilterConfigs(), store.fetch())

            repository.setConfig("BTC/USD", secondConfig)
            advanceUntilIdle()

            assertEquals(secondConfig, repository.getConfig("BTC/USD"))
            assertEquals(OfferbookFilterConfigs(mapOf("BTC/USD" to secondConfig)), store.fetch())
        }

    @Test
    fun `disabling remembering filters clears persisted configs but keeps session configs`() =
        runTest {
            val persistedConfig = config(paymentMethods = setOf("SEPA", "REVOLUT"))
            val fixture =
                fixture(
                    scope = CoroutineScope(StandardTestDispatcher(testScheduler) + SupervisorJob()),
                    persistedConfigs = OfferbookFilterConfigs(mapOf("BTC/EUR" to persistedConfig)),
                    rememberFilters = true,
                )
            advanceUntilIdle()
            assertEquals(persistedConfig, fixture.repository.getConfig("BTC/EUR"))
            advanceUntilIdle()

            fixture.settingsRepository.setRememberOfferbookFilterPreferences(false)
            advanceUntilIdle()

            assertEquals(persistedConfig, fixture.repository.getConfig("BTC/EUR"))
            assertEquals(OfferbookFilterConfigs(), fixture.store.fetch())
        }

    @Test
    fun `setConfig updates session only when remembering filters is disabled`() =
        runTest {
            val persistedConfig = config(paymentMethods = setOf("SEPA"))
            val sessionOnlyConfig = config(paymentMethods = setOf("CASH_APP"))
            val fixture =
                fixture(
                    scope = CoroutineScope(StandardTestDispatcher(testScheduler) + SupervisorJob()),
                    persistedConfigs = OfferbookFilterConfigs(mapOf("BTC/USD" to persistedConfig)),
                    rememberFilters = true,
                )
            advanceUntilIdle()
            assertEquals(persistedConfig, fixture.repository.getConfig("BTC/USD"))
            advanceUntilIdle()
            fixture.settingsRepository.setRememberOfferbookFilterPreferences(false)
            advanceUntilIdle()

            fixture.repository.setConfig("BTC/USD", sessionOnlyConfig)
            advanceUntilIdle()

            assertEquals(sessionOnlyConfig, fixture.repository.getConfig("BTC/USD"))
            assertEquals(OfferbookFilterConfigs(), fixture.store.fetch())
        }

    @Test
    fun `re-enabling remembering filters persists current session configs`() =
        runTest {
            val persistedConfig = config(paymentMethods = setOf("SEPA"))
            val sessionOnlyConfig = config(paymentMethods = setOf("ACH"), onlyMyOffers = true)
            val fixture =
                fixture(
                    scope = CoroutineScope(StandardTestDispatcher(testScheduler) + SupervisorJob()),
                    persistedConfigs = OfferbookFilterConfigs(mapOf("BTC/USD" to persistedConfig)),
                    rememberFilters = true,
                )
            advanceUntilIdle()
            assertEquals(persistedConfig, fixture.repository.getConfig("BTC/USD"))
            advanceUntilIdle()
            fixture.settingsRepository.setRememberOfferbookFilterPreferences(false)
            advanceUntilIdle()
            fixture.repository.setConfig("BTC/USD", sessionOnlyConfig)
            advanceUntilIdle()

            fixture.settingsRepository.setRememberOfferbookFilterPreferences(true)
            advanceUntilIdle()

            assertEquals(sessionOnlyConfig, fixture.repository.getConfig("BTC/USD"))
            assertEquals(OfferbookFilterConfigs(mapOf("BTC/USD" to sessionOnlyConfig)), fixture.store.fetch())
        }

    @Test
    fun `initializes with empty session and clears persisted configs when remembering filters is disabled at startup`() =
        runTest {
            val persistedConfig = config(paymentMethods = setOf("SEPA"))
            val fixture =
                fixture(
                    scope = CoroutineScope(StandardTestDispatcher(testScheduler) + SupervisorJob()),
                    persistedConfigs = OfferbookFilterConfigs(mapOf("BTC/USD" to persistedConfig)),
                    rememberFilters = false,
                )

            advanceUntilIdle()

            assertEquals(OfferbookFilterConfig(), fixture.repository.getConfig("BTC/USD"))
            assertEquals(OfferbookFilterConfigs(), fixture.store.fetch())
        }

    @Test
    fun `clear failure during disabled startup still initializes with empty in-memory configs`() =
        runTest {
            val store = ThrowingDataStore<OfferbookFilterConfigs>(IOException("clear failed"))
            val repository =
                OfferbookFilterConfigRepositoryImpl(
                    offerbookFilterConfigsStore = store,
                    settingsRepository = FakeSettingsRepository(Settings(rememberOfferbookFilterPreferences = false)),
                    jobsManager = TestCoroutineJobsManager(CoroutineScope(StandardTestDispatcher(testScheduler) + SupervisorJob())),
                )
            advanceUntilIdle()

            assertEquals(OfferbookFilterConfig(), repository.getConfig("BTC/USD"))
            assertEquals(OfferbookFilterConfigs(), repository.data.first())
        }
}
