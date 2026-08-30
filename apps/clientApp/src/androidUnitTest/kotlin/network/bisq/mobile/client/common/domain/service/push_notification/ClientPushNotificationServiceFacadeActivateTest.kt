package network.bisq.mobile.client.common.domain.service.push_notification

import android.content.ContentResolver
import android.content.Context
import android.provider.Settings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettings
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepositoryMock
import network.bisq.mobile.client.common.test_utils.ClientKoinIntegrationTestBase
import network.bisq.mobile.data.replicated.common.network.AddressByTransportTypeMapVO
import network.bisq.mobile.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.data.replicated.security.keys.PubKeyVO
import network.bisq.mobile.data.replicated.security.keys.PublicKeyVO
import network.bisq.mobile.data.replicated.security.pow.ProofOfWorkVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.main.ApplicationContextProvider
import network.bisq.mobile.test.mocks.SettingsRepositoryMock
import org.junit.Test
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Integration tests for ClientPushNotificationServiceFacade.activate() method.
 * These tests require Koin to be started because activate() uses serviceScope.launch
 * which depends on CoroutineJobsManager injected via Koin.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ClientPushNotificationServiceFacadeActivateTest : ClientKoinIntegrationTestBase() {
    private val apiGateway: PushNotificationApiGateway = mockk(relaxed = true)
    private val tokenProvider: PushNotificationTokenProvider = mockk(relaxed = true)
    private val userProfileServiceFacade: UserProfileServiceFacade = mockk(relaxed = true)
    private val mockContext = mockk<Context>()
    private val mockContentResolver = mockk<ContentResolver>()

    private lateinit var settingsRepository: SettingsRepositoryMock
    private lateinit var sensitiveSettingsRepository: SensitiveSettingsRepositoryMock
    private lateinit var facade: ClientPushNotificationServiceFacade

    private val testUserProfile =
        UserProfileVO(
            version = 1,
            nickName = "testUser",
            terms = "",
            statement = "",
            avatarVersion = 0,
            networkId =
                NetworkIdVO(
                    addressByTransportTypeMap =
                        AddressByTransportTypeMapVO(
                            emptyMap(),
                        ),
                    pubKey =
                        PubKeyVO(
                            publicKey = PublicKeyVO("testPublicKey"),
                            keyId = "key",
                            hash = "hash",
                            id = "id",
                        ),
                ),
            proofOfWork = ProofOfWorkVO("payload", 1L, "challenge", 2.0, "sol", 100L),
            applicationVersion = "1.0.0",
            nym = "testNym",
            userName = "testUser",
            publishDate = System.currentTimeMillis(),
        )

    override fun additionalModules(): List<Module> = listOf(module { })

    private val savedKeyStoreFactory = network.bisq.mobile.data.crypto.pushNotificationKeyStoreFactory

    override fun onSetup() {
        every { mockContext.applicationContext } returns mockContext
        every { mockContext.contentResolver } returns mockContentResolver
        mockkStatic(Settings.Secure::class)
        every { Settings.Secure.getString(mockContentResolver, Settings.Secure.ANDROID_ID) } returns "test-android-id"
        ApplicationContextProvider.initialize(mockContext)

        // Robolectric can't emulate AndroidKeyStore, which the production key store
        // wraps with. Seed an in-memory fake so getOrCreatePushNotificationKeyBase64() returns
        // a valid key — otherwise validateSymmetricKey aborts registration
        // before the apiGateway.registerDevice mock is exercised.
        network.bisq.mobile.data.crypto.pushNotificationKeyStoreFactory = { InMemoryKeyStoreForTest() }

        settingsRepository = SettingsRepositoryMock()
        sensitiveSettingsRepository = SensitiveSettingsRepositoryMock()
        every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(testUserProfile)

        facade =
            ClientPushNotificationServiceFacade(
                apiGateway = apiGateway,
                settingsRepository = settingsRepository,
                sensitiveSettingsRepository = sensitiveSettingsRepository,
                pushNotificationTokenProvider = tokenProvider,
                userProfileServiceFacade = userProfileServiceFacade,
                backgroundDispatcher = testDispatcher,
            )
    }

    override fun onTearDown() {
        try {
            try {
                unmockkStatic(Settings.Secure::class)
            } finally {
                network.bisq.mobile.data.crypto.pushNotificationKeyStoreFactory = savedKeyStoreFactory
            }
        } finally {
            super.onTearDown()
        }
    }

    private class InMemoryKeyStoreForTest : network.bisq.mobile.data.crypto.PushNotificationKeyStore {
        private var stored: String? = null

        override fun put(base64: String) {
            stored = base64
        }

        override fun get(): String? = stored
    }

    @Test
    fun `activate starts settings collection`() =
        runTest {
            settingsRepository.update { it.copy(pushNotificationsEnabled = false) }
            facade.activate()
            advanceUntilIdle()
            assertFalse(facade.isPushNotificationsEnabled.value)
        }

    @Test
    fun `activate with push enabled but no onboarding does not auto-register`() =
        runTest {
            settingsRepository.update { it.copy(pushNotificationsEnabled = true) }
            sensitiveSettingsRepository.update { SensitiveSettings(bisqApiUrl = "") }
            facade.activate()
            advanceUntilIdle()
            assertTrue(facade.isPushNotificationsEnabled.value)
            assertFalse(facade.isDeviceRegistered.value)
            coVerify(exactly = 0) { apiGateway.registerDevice(any(), any(), any(), any(), any(), any()) }
        }

    @Test
    fun `activate with push enabled and onboarding complete triggers auto-register`() =
        runTest {
            sensitiveSettingsRepository.update { SensitiveSettings(bisqApiUrl = "http://localhost:8080") }
            coEvery { tokenProvider.requestPermission() } returns true
            coEvery { tokenProvider.requestDeviceToken() } returns Result.success("test-device-token")
            coEvery { apiGateway.registerDevice(any(), any(), any(), any(), any(), any()) } returns Result.success(Unit)
            facade.activate()
            advanceUntilIdle()
            settingsRepository.update { it.copy(pushNotificationsEnabled = true) }
            advanceUntilIdle()
            assertTrue(facade.isPushNotificationsEnabled.value)
            coVerify(atLeast = 1) { tokenProvider.requestPermission() }
        }

    @Test
    fun `activate defers auto-registration until the selected user profile arrives`() =
        runTest {
            // Cold start ordering: settings are read locally and are ready immediately, while the
            // profile comes from the trusted node over Tor seconds later. Registering on the
            // settings emission alone aborted with "no user profile selected" and never retried.
            val userProfileFlow = MutableStateFlow<UserProfileVO?>(null)
            every { userProfileServiceFacade.selectedUserProfile } returns userProfileFlow
            sensitiveSettingsRepository.update { SensitiveSettings(bisqApiUrl = "http://localhost:8080") }
            settingsRepository.update { it.copy(pushNotificationsEnabled = true) }
            coEvery { tokenProvider.requestPermission() } returns true
            coEvery { tokenProvider.requestDeviceToken() } returns Result.success("test-device-token")
            coEvery { apiGateway.registerDevice(any(), any(), any(), any(), any(), any()) } returns Result.success(Unit)

            facade.activate()
            advanceUntilIdle()

            assertFalse(facade.isDeviceRegistered.value, "must not register before a profile is selected")
            coVerify(exactly = 0) { apiGateway.registerDevice(any(), any(), any(), any(), any(), any()) }

            userProfileFlow.value = testUserProfile
            advanceUntilIdle()

            assertTrue(facade.isDeviceRegistered.value)
            coVerify(exactly = 1) { apiGateway.registerDevice(any(), any(), any(), any(), any(), any()) }
        }

    @Test
    fun `activate registers once even though a successful registration writes settings again`() =
        runTest {
            // registerTokenWithTrustedNode persists pushNotificationsEnabled=true, which emits
            // through the settings flow again; the device must not be registered twice.
            sensitiveSettingsRepository.update { SensitiveSettings(bisqApiUrl = "http://localhost:8080") }
            coEvery { tokenProvider.requestPermission() } returns true
            coEvery { tokenProvider.requestDeviceToken() } returns Result.success("test-device-token")
            coEvery { apiGateway.registerDevice(any(), any(), any(), any(), any(), any()) } returns Result.success(Unit)

            facade.activate()
            advanceUntilIdle()
            settingsRepository.update { it.copy(pushNotificationsEnabled = true) }
            advanceUntilIdle()

            coVerify(exactly = 1) { apiGateway.registerDevice(any(), any(), any(), any(), any(), any()) }
        }

    @Test
    fun `activate with push enabled and onboarding complete but permission denied`() =
        runTest {
            sensitiveSettingsRepository.update { SensitiveSettings(bisqApiUrl = "http://localhost:8080") }
            coEvery { tokenProvider.requestPermission() } returns false
            facade.activate()
            advanceUntilIdle()
            settingsRepository.update { it.copy(pushNotificationsEnabled = true) }
            advanceUntilIdle()
            assertTrue(facade.isPushNotificationsEnabled.value)
            assertFalse(facade.isDeviceRegistered.value)
        }

    @Test
    fun `onDeviceTokenReceived with push enabled triggers re-registration`() =
        runTest {
            sensitiveSettingsRepository.update { SensitiveSettings(bisqApiUrl = "http://localhost:8080") }
            coEvery { tokenProvider.requestPermission() } returns true
            coEvery { tokenProvider.requestDeviceToken() } returns Result.success("initial-token")
            coEvery { apiGateway.registerDevice(any(), any(), any(), any(), any(), any()) } returns Result.success(Unit)
            facade.activate()
            advanceUntilIdle()
            settingsRepository.update { it.copy(pushNotificationsEnabled = true) }
            advanceUntilIdle()
            facade.onDeviceTokenReceived("new-device-token")
            advanceUntilIdle()
            coVerify(atLeast = 1) { apiGateway.registerDevice(any(), eq("new-device-token"), any(), any(), any(), any()) }
        }

    @Test
    fun `onDeviceTokenReceived with push disabled does not trigger re-registration`() =
        runTest {
            settingsRepository.update { it.copy(pushNotificationsEnabled = false) }
            facade.activate()
            advanceUntilIdle()
            facade.onDeviceTokenReceived("new-device-token")
            advanceUntilIdle()
            coVerify(exactly = 0) { apiGateway.registerDevice(any(), any(), any(), any(), any(), any()) }
        }

    @Test
    fun `onDeviceTokenReceived with re-registration failure updates state`() =
        runTest {
            sensitiveSettingsRepository.update { SensitiveSettings(bisqApiUrl = "http://localhost:8080") }
            coEvery { tokenProvider.requestPermission() } returns true
            coEvery { tokenProvider.requestDeviceToken() } returns Result.success("initial-token")
            coEvery { apiGateway.registerDevice(any(), any(), any(), any(), any(), any()) } returns Result.success(Unit)
            facade.activate()
            advanceUntilIdle()
            settingsRepository.update { it.copy(pushNotificationsEnabled = true) }
            advanceUntilIdle()
            coEvery { apiGateway.registerDevice(any(), eq("new-token"), any(), any(), any(), any()) } returns
                Result.failure(Exception("Network error"))
            facade.onDeviceTokenReceived("new-token")
            advanceUntilIdle()
            assertFalse(facade.isDeviceRegistered.value)
        }

    @Test
    fun `deactivate cancels all coroutines`() =
        runTest {
            facade.activate()
            advanceUntilIdle()
            facade.deactivate()
            advanceUntilIdle()
            // No exception should be thrown - deactivate cleans up all coroutines
        }

    @Test
    fun `when auto-registration returns failure then handles gracefully`() =
        runTest {
            // Given
            sensitiveSettingsRepository.update { SensitiveSettings(bisqApiUrl = "http://localhost:8080") }
            coEvery { tokenProvider.requestPermission() } returns true
            coEvery { tokenProvider.requestDeviceToken() } returns Result.success("test-token")
            coEvery { apiGateway.registerDevice(any(), any(), any(), any(), any(), any()) } returns
                Result.failure(Exception("API error"))

            // When
            facade.activate()
            advanceUntilIdle()
            settingsRepository.update { it.copy(pushNotificationsEnabled = true) }
            advanceUntilIdle()

            // Then
            assertTrue(facade.isPushNotificationsEnabled.value)
            assertFalse(facade.isDeviceRegistered.value)
        }

    @Test
    fun `when auto-registration throws exception then catches and logs error`() =
        runTest {
            // Given
            sensitiveSettingsRepository.update { SensitiveSettings(bisqApiUrl = "http://localhost:8080") }
            coEvery { tokenProvider.requestPermission() } throws RuntimeException("Unexpected error during permission request")

            // When
            facade.activate()
            advanceUntilIdle()
            settingsRepository.update { it.copy(pushNotificationsEnabled = true) }
            advanceUntilIdle()

            // Then - should not crash, exception is caught and logged
            assertTrue(facade.isPushNotificationsEnabled.value)
            assertFalse(facade.isDeviceRegistered.value)
        }
}
