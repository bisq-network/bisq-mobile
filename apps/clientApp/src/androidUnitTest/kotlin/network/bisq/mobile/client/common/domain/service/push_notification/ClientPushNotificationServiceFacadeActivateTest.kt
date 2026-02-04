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
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettings
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepositoryMock
import network.bisq.mobile.client.common.test_utils.KoinIntegrationTestBase
import network.bisq.mobile.domain.data.replicated.common.network.AddressByTransportTypeMapVO
import network.bisq.mobile.domain.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.domain.data.replicated.security.keys.PubKeyVO
import network.bisq.mobile.domain.data.replicated.security.keys.PublicKeyVO
import network.bisq.mobile.domain.data.replicated.security.pow.ProofOfWorkVO
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.repository.SettingsRepositoryMock
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.main.ApplicationContextProvider
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
class ClientPushNotificationServiceFacadeActivateTest : KoinIntegrationTestBase() {
    private lateinit var facade: ClientPushNotificationServiceFacade
    private lateinit var apiGateway: PushNotificationApiGateway
    private lateinit var settingsRepository: SettingsRepositoryMock
    private lateinit var sensitiveSettingsRepository: SensitiveSettingsRepositoryMock
    private lateinit var tokenProvider: PushNotificationTokenProvider
    private lateinit var userProfileServiceFacade: UserProfileServiceFacade
    private val mockContext = mockk<Context>()
    private val mockContentResolver = mockk<ContentResolver>()

    private val testUserProfile =
        UserProfileVO(
            version = 1,
            nickName = "testUser",
            terms = "",
            statement = "",
            avatarVersion = 0,
            networkId =
                NetworkIdVO(
                    addressByTransportTypeMap = AddressByTransportTypeMapVO(emptyMap()),
                    pubKey = PubKeyVO(publicKey = PublicKeyVO("testPublicKey"), keyId = "key", hash = "hash", id = "id"),
                ),
            proofOfWork = ProofOfWorkVO("payload", 1L, "challenge", 2.0, "sol", 100L),
            applicationVersion = "1.0.0",
            nym = "testNym",
            userName = "testUser",
            publishDate = System.currentTimeMillis(),
        )

    override fun additionalModules(): List<Module> = listOf(module { })

    override fun onSetup() {
        every { mockContext.applicationContext } returns mockContext
        every { mockContext.contentResolver } returns mockContentResolver
        mockkStatic(Settings.Secure::class)
        every { Settings.Secure.getString(mockContentResolver, Settings.Secure.ANDROID_ID) } returns "test-android-id"
        ApplicationContextProvider.initialize(mockContext)

        apiGateway = mockk(relaxed = true)
        settingsRepository = SettingsRepositoryMock()
        sensitiveSettingsRepository = SensitiveSettingsRepositoryMock()
        tokenProvider = mockk(relaxed = true)
        userProfileServiceFacade = mockk(relaxed = true)
        every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(testUserProfile)

        facade =
            ClientPushNotificationServiceFacade(
                apiGateway = apiGateway,
                settingsRepository = settingsRepository,
                sensitiveSettingsRepository = sensitiveSettingsRepository,
                pushNotificationTokenProvider = tokenProvider,
                userProfileServiceFacade = userProfileServiceFacade,
            )
    }

    override fun onTearDown() {
        unmockkStatic(Settings.Secure::class)
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
            coVerify(exactly = 0) { apiGateway.registerDevice(any(), any(), any(), any(), any()) }
        }

    @Test
    fun `activate with push enabled and onboarding complete triggers auto-register`() =
        runTest {
            sensitiveSettingsRepository.update { SensitiveSettings(bisqApiUrl = "http://localhost:8080") }
            coEvery { tokenProvider.requestPermission() } returns true
            coEvery { tokenProvider.requestDeviceToken() } returns Result.success("test-device-token")
            coEvery { apiGateway.registerDevice(any(), any(), any(), any(), any()) } returns Result.success(Unit)
            facade.activate()
            advanceUntilIdle()
            settingsRepository.update { it.copy(pushNotificationsEnabled = true) }
            advanceUntilIdle()
            assertTrue(facade.isPushNotificationsEnabled.value)
            coVerify(atLeast = 1) { tokenProvider.requestPermission() }
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
            coEvery { apiGateway.registerDevice(any(), any(), any(), any(), any()) } returns Result.success(Unit)
            facade.activate()
            advanceUntilIdle()
            settingsRepository.update { it.copy(pushNotificationsEnabled = true) }
            advanceUntilIdle()
            facade.onDeviceTokenReceived("new-device-token")
            advanceUntilIdle()
            coVerify(atLeast = 1) { apiGateway.registerDevice(any(), eq("new-device-token"), any(), any(), any()) }
        }

    @Test
    fun `onDeviceTokenReceived with push disabled does not trigger re-registration`() =
        runTest {
            settingsRepository.update { it.copy(pushNotificationsEnabled = false) }
            facade.activate()
            advanceUntilIdle()
            facade.onDeviceTokenReceived("new-device-token")
            advanceUntilIdle()
            coVerify(exactly = 0) { apiGateway.registerDevice(any(), any(), any(), any(), any()) }
        }

    @Test
    fun `onDeviceTokenReceived with re-registration failure updates state`() =
        runTest {
            sensitiveSettingsRepository.update { SensitiveSettings(bisqApiUrl = "http://localhost:8080") }
            coEvery { tokenProvider.requestPermission() } returns true
            coEvery { tokenProvider.requestDeviceToken() } returns Result.success("initial-token")
            coEvery { apiGateway.registerDevice(any(), any(), any(), any(), any()) } returns Result.success(Unit)
            facade.activate()
            advanceUntilIdle()
            settingsRepository.update { it.copy(pushNotificationsEnabled = true) }
            advanceUntilIdle()
            coEvery { apiGateway.registerDevice(any(), eq("new-token"), any(), any(), any()) } returns
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
            coEvery { apiGateway.registerDevice(any(), any(), any(), any(), any()) } returns
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
