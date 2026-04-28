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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepositoryMock
import network.bisq.mobile.data.crypto.PushNotificationKeyStore
import network.bisq.mobile.data.crypto.pushNotificationKeyStoreFactory
import network.bisq.mobile.data.replicated.common.network.AddressByTransportTypeMapVO
import network.bisq.mobile.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.data.replicated.security.keys.PubKeyVO
import network.bisq.mobile.data.replicated.security.keys.PublicKeyVO
import network.bisq.mobile.data.replicated.security.pow.ProofOfWorkVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.main.ApplicationContextProvider
import network.bisq.mobile.test.mocks.SettingsRepositoryMock
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ClientPushNotificationServiceFacadeIntegrationTest {
    private val testDispatcher = StandardTestDispatcher()

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
            proofOfWork = ProofOfWorkVO("payload", 1L, "challenge", 2.0, "sol", 100L),
            avatarVersion = 1,
            networkId =
                NetworkIdVO(
                    addressByTransportTypeMap =
                        AddressByTransportTypeMapVO(
                            mapOf(),
                        ),
                    pubKey =
                        PubKeyVO(
                            publicKey = PublicKeyVO("testPublicKey"),
                            keyId = "key",
                            hash = "hash",
                            id = "id",
                        ),
                ),
            terms = "",
            statement = "",
            applicationVersion = "1.0.0",
            nym = "testNym",
            userName = "testUser",
            publishDate = System.currentTimeMillis(),
        )

    private val savedKeyStoreFactory = pushNotificationKeyStoreFactory

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Setup Android context mock for getDeviceId()
        every { mockContext.applicationContext } returns mockContext
        every { mockContext.contentResolver } returns mockContentResolver
        mockkStatic(Settings.Secure::class)
        every {
            Settings.Secure.getString(mockContentResolver, Settings.Secure.ANDROID_ID)
        } returns "test-android-id-12345"
        ApplicationContextProvider.initialize(mockContext)

        // Robolectric can't run Tink-backed EncryptedSharedPreferences. Seed
        // an in-memory fake so getOrCreatePushNotificationKeyBase64() returns
        // a valid key — otherwise validateSymmetricKey aborts registration
        // before the apiGateway.registerDevice mock is exercised.
        pushNotificationKeyStoreFactory = { InMemoryKeyStoreForTest() }

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

    @After
    fun tearDown() {
        unmockkStatic(Settings.Secure::class)
        Dispatchers.resetMain()
        pushNotificationKeyStoreFactory = savedKeyStoreFactory
    }

    private class InMemoryKeyStoreForTest : PushNotificationKeyStore {
        private var stored: String? = null

        override fun put(base64: String) {
            stored = base64
        }

        override fun get(): String? = stored
    }

    @Test
    fun `initial state has push notifications disabled`() =
        runTest {
            // Given
            // Facade is initialized with default state

            // When / Then
            assertFalse(facade.isPushNotificationsEnabled.value)
            assertFalse(facade.isDeviceRegistered.value)
            assertEquals(null, facade.deviceToken.value)
        }

    @Test
    fun `requestPermission delegates to token provider`() =
        runTest {
            // Given
            coEvery { tokenProvider.requestPermission() } returns true

            // When
            val result = facade.requestPermission()

            // Then
            assertTrue(result)
            coVerify { tokenProvider.requestPermission() }
        }

    @Test
    fun `registerForPushNotifications fails when permission denied`() =
        runTest {
            // Given
            coEvery { tokenProvider.requestPermission() } returns false

            // When
            val result = facade.registerForPushNotifications()

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("Permission denied") == true)
        }

    @Test
    fun `registerForPushNotifications fails when token request fails`() =
        runTest {
            // Given
            coEvery { tokenProvider.requestPermission() } returns true
            coEvery { tokenProvider.requestDeviceToken() } returns
                Result.failure(
                    PushNotificationException("Token request failed"),
                )

            // When
            val result = facade.registerForPushNotifications()

            // Then
            assertTrue(result.isFailure)
        }

    @Test
    fun `registerForPushNotifications fails when token is blank`() =
        runTest {
            // Given
            coEvery { tokenProvider.requestPermission() } returns true
            coEvery { tokenProvider.requestDeviceToken() } returns Result.success("")

            // When
            val result = facade.registerForPushNotifications()

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("null or blank") == true)
        }

    @Test
    fun `registerForPushNotifications fails when no user profile selected`() =
        runTest {
            // Given
            every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(null)
            coEvery { tokenProvider.requestPermission() } returns true
            coEvery { tokenProvider.requestDeviceToken() } returns Result.success("test-token")

            // When
            val result = facade.registerForPushNotifications()

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("No user profile") == true)
        }

    @Test
    fun `onDeviceTokenRegistrationFailed clears device token`() =
        runTest {
            // Given
            val error = RuntimeException("Test error")

            // When
            facade.onDeviceTokenRegistrationFailed(error)

            // Then
            assertEquals(null, facade.deviceToken.value)
        }

    @Test
    fun `registerForPushNotifications succeeds with valid token and profile`() =
        runTest {
            // Given
            coEvery { tokenProvider.requestPermission() } returns true
            coEvery { tokenProvider.requestDeviceToken() } returns Result.success("valid-device-token")
            coEvery { apiGateway.registerDevice(any(), any(), any(), any(), any(), any()) } returns Result.success(Unit)

            // When
            val result = facade.registerForPushNotifications()

            // Then
            assertTrue(result.isSuccess)
            assertTrue(facade.isDeviceRegistered.value)
            assertEquals("valid-device-token", facade.deviceToken.value)
            coVerify { apiGateway.registerDevice(any(), "valid-device-token", any(), any(), any(), any()) }
        }

    @Test
    fun `registerForPushNotifications updates settings on success`() =
        runTest {
            // Given
            coEvery { tokenProvider.requestPermission() } returns true
            coEvery { tokenProvider.requestDeviceToken() } returns Result.success("valid-token")
            coEvery { apiGateway.registerDevice(any(), any(), any(), any(), any(), any()) } returns Result.success(Unit)

            // When
            facade.registerForPushNotifications()

            // Then
            assertTrue(settingsRepository.fetch().pushNotificationsEnabled)
        }

    @Test
    fun `unregisterFromPushNotifications clears registration state`() =
        runTest {
            // Given
            coEvery { apiGateway.unregisterDevice(any()) } returns Result.success(Unit)

            // When
            val result = facade.unregisterFromPushNotifications()

            // Then
            assertTrue(result.isSuccess)
            assertFalse(facade.isDeviceRegistered.value)
            assertFalse(settingsRepository.fetch().pushNotificationsEnabled)
        }

    @Test
    fun `unregisterFromPushNotifications clears state even on API failure`() =
        runTest {
            // Given
            coEvery { apiGateway.unregisterDevice(any()) } returns Result.failure(RuntimeException("API error"))

            // When
            val result = facade.unregisterFromPushNotifications()

            // Then
            assertTrue(result.isFailure)
            assertFalse(facade.isDeviceRegistered.value)
            assertFalse(settingsRepository.fetch().pushNotificationsEnabled)
        }

    @Test
    fun `unregisterFromPushNotifications revokes platform token (Android disables Firebase auto-init)`() =
        runTest {
            // Given
            coEvery { apiGateway.unregisterDevice(any()) } returns Result.success(Unit)
            coEvery { tokenProvider.revokeDeviceToken() } returns Result.success(Unit)

            // When
            facade.unregisterFromPushNotifications()

            // Then — the platform-side cleanup must run so we stop talking to
            // the upstream push provider (Google FCM on Android).
            coVerify(exactly = 1) { tokenProvider.revokeDeviceToken() }
        }

    @Test
    fun `unregisterFromPushNotifications surfaces revokeDeviceToken failure but still clears local state`() =
        runTest {
            // Given — first put the facade into a fully-registered state so
            // the assertions below actually prove that unregister cleared it,
            // not that it was never set. We register, then mock unregister to
            // fail at the platform-token-revoke step.
            coEvery { tokenProvider.requestPermission() } returns true
            coEvery { tokenProvider.requestDeviceToken() } returns Result.success("seeded-token")
            coEvery { apiGateway.registerDevice(any(), any(), any(), any(), any(), any()) } returns Result.success(Unit)
            facade.registerForPushNotifications()
            assertTrue(facade.isDeviceRegistered.value, "precondition: facade must be registered")
            assertEquals("seeded-token", facade.deviceToken.value, "precondition: deviceToken must be set")
            assertTrue(
                settingsRepository.fetch().pushNotificationsEnabled,
                "precondition: pushNotificationsEnabled must be true",
            )

            // Server unregister succeeds, but the platform-side token revoke
            // fails (e.g., FCM unreachable, isAutoInitEnabled couldn't be
            // flipped). The user-facing result must reflect this so the
            // caller can warn the user / retry — silently treating it as
            // success would leave the device receiving pushes despite opt-out.
            coEvery { apiGateway.unregisterDevice(any()) } returns Result.success(Unit)
            coEvery { tokenProvider.revokeDeviceToken() } returns
                Result.failure(RuntimeException("FCM unreachable"))

            // When
            val result = facade.unregisterFromPushNotifications()

            // Then — the aggregated result is a failure, with a clear message
            // distinguishing it from a server-side unregister failure.
            assertTrue(result.isFailure)
            val ex = result.exceptionOrNull()
            assertTrue(ex is PushNotificationException)
            assertTrue(
                ex.message?.contains("local platform token revoke failed") == true,
                "expected aggregated-failure message; got: ${ex.message}",
            )
            // Local state is still cleared so the next opt-in starts fresh.
            assertFalse(facade.isDeviceRegistered.value)
            assertNull(facade.deviceToken.value)
            assertFalse(settingsRepository.fetch().pushNotificationsEnabled)
        }

    @Test
    fun `onDeviceTokenReceived updates token`() =
        runTest {
            // Given
            val newToken = "new-token"
            // Push notifications disabled by default, so no re-registration

            // When
            facade.onDeviceTokenReceived(newToken)

            // Then
            assertEquals(newToken, facade.deviceToken.value)
        }

    @Test
    fun `onDeviceTokenReceived does not re-register when disabled`() =
        runTest {
            // Given
            val newToken = "new-token"
            // Push notifications disabled by default

            // When
            facade.onDeviceTokenReceived(newToken)

            // Then
            assertEquals(newToken, facade.deviceToken.value)
            coVerify(exactly = 0) { apiGateway.registerDevice(any(), any(), any(), any(), any(), any()) }
        }

    @Test
    fun `registerForPushNotifications fails when API returns error`() =
        runTest {
            // Given
            coEvery { tokenProvider.requestPermission() } returns true
            coEvery { tokenProvider.requestDeviceToken() } returns Result.success("valid-token")
            coEvery { apiGateway.registerDevice(any(), any(), any(), any(), any(), any()) } returns
                Result.failure(RuntimeException("Server error"))

            // When
            val result = facade.registerForPushNotifications()

            // Then
            assertTrue(result.isFailure)
            assertFalse(facade.isDeviceRegistered.value)
        }
}
