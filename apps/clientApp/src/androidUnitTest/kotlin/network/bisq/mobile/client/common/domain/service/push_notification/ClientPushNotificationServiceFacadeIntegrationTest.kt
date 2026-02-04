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
import network.bisq.mobile.domain.data.replicated.common.network.AddressByTransportTypeMapVO
import network.bisq.mobile.domain.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.domain.data.replicated.security.keys.PubKeyVO
import network.bisq.mobile.domain.data.replicated.security.keys.PublicKeyVO
import network.bisq.mobile.domain.data.replicated.security.pow.ProofOfWorkVO
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.repository.SettingsRepositoryMock
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.main.ApplicationContextProvider
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
                    addressByTransportTypeMap = AddressByTransportTypeMapVO(mapOf()),
                    pubKey = PubKeyVO(publicKey = PublicKeyVO("testPublicKey"), keyId = "key", hash = "hash", id = "id"),
                ),
            terms = "",
            statement = "",
            applicationVersion = "1.0.0",
            nym = "testNym",
            userName = "testUser",
            publishDate = System.currentTimeMillis(),
        )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Setup Android context mock for getDeviceId()
        every { mockContext.applicationContext } returns mockContext
        every { mockContext.contentResolver } returns mockContentResolver
        mockkStatic(Settings.Secure::class)
        every {
            Settings.Secure.getString(mockContentResolver, Settings.Secure.ANDROID_ID)
        } returns "test-android-id-12345"
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

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Settings.Secure::class)
    }

    @Test
    fun `initial state has push notifications disabled`() =
        runTest {
            assertFalse(facade.isPushNotificationsEnabled.value)
            assertFalse(facade.isDeviceRegistered.value)
            assertEquals(null, facade.deviceToken.value)
        }

    @Test
    fun `requestPermission delegates to token provider`() =
        runTest {
            coEvery { tokenProvider.requestPermission() } returns true
            val result = facade.requestPermission()
            assertTrue(result)
            coVerify { tokenProvider.requestPermission() }
        }

    @Test
    fun `registerForPushNotifications fails when permission denied`() =
        runTest {
            coEvery { tokenProvider.requestPermission() } returns false

            val result = facade.registerForPushNotifications()

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("Permission denied") == true)
        }

    @Test
    fun `registerForPushNotifications fails when token request fails`() =
        runTest {
            coEvery { tokenProvider.requestPermission() } returns true
            coEvery { tokenProvider.requestDeviceToken() } returns
                Result.failure(
                    PushNotificationException("Token request failed"),
                )

            val result = facade.registerForPushNotifications()

            assertTrue(result.isFailure)
        }

    @Test
    fun `registerForPushNotifications fails when token is blank`() =
        runTest {
            coEvery { tokenProvider.requestPermission() } returns true
            coEvery { tokenProvider.requestDeviceToken() } returns Result.success("")

            val result = facade.registerForPushNotifications()

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("null or blank") == true)
        }

    @Test
    fun `registerForPushNotifications fails when no user profile selected`() =
        runTest {
            every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(null)
            coEvery { tokenProvider.requestPermission() } returns true
            coEvery { tokenProvider.requestDeviceToken() } returns Result.success("test-token")

            val result = facade.registerForPushNotifications()

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("No user profile") == true)
        }

    @Test
    fun `onDeviceTokenRegistrationFailed clears device token`() =
        runTest {
            facade.onDeviceTokenRegistrationFailed(RuntimeException("Test error"))
            assertEquals(null, facade.deviceToken.value)
        }

    @Test
    fun `registerForPushNotifications succeeds with valid token and profile`() =
        runTest {
            coEvery { tokenProvider.requestPermission() } returns true
            coEvery { tokenProvider.requestDeviceToken() } returns Result.success("valid-device-token")
            coEvery { apiGateway.registerDevice(any(), any(), any(), any(), any()) } returns Result.success(Unit)

            val result = facade.registerForPushNotifications()

            assertTrue(result.isSuccess)
            assertTrue(facade.isDeviceRegistered.value)
            assertEquals("valid-device-token", facade.deviceToken.value)
            coVerify { apiGateway.registerDevice(any(), "valid-device-token", any(), any(), any()) }
        }

    @Test
    fun `registerForPushNotifications updates settings on success`() =
        runTest {
            coEvery { tokenProvider.requestPermission() } returns true
            coEvery { tokenProvider.requestDeviceToken() } returns Result.success("valid-token")
            coEvery { apiGateway.registerDevice(any(), any(), any(), any(), any()) } returns Result.success(Unit)

            facade.registerForPushNotifications()

            assertTrue(settingsRepository.fetch().pushNotificationsEnabled)
        }

    @Test
    fun `unregisterFromPushNotifications clears registration state`() =
        runTest {
            coEvery { apiGateway.unregisterDevice(any()) } returns Result.success(Unit)

            val result = facade.unregisterFromPushNotifications()

            assertTrue(result.isSuccess)
            assertFalse(facade.isDeviceRegistered.value)
            assertFalse(settingsRepository.fetch().pushNotificationsEnabled)
        }

    @Test
    fun `unregisterFromPushNotifications clears state even on API failure`() =
        runTest {
            coEvery { apiGateway.unregisterDevice(any()) } returns Result.failure(RuntimeException("API error"))

            val result = facade.unregisterFromPushNotifications()

            assertTrue(result.isFailure)
            assertFalse(facade.isDeviceRegistered.value)
            assertFalse(settingsRepository.fetch().pushNotificationsEnabled)
        }

    @Test
    fun `onDeviceTokenReceived updates token`() =
        runTest {
            // Push notifications disabled by default, so no re-registration
            facade.onDeviceTokenReceived("new-token")

            assertEquals("new-token", facade.deviceToken.value)
        }

    @Test
    fun `onDeviceTokenReceived does not re-register when disabled`() =
        runTest {
            // Push notifications disabled by default
            facade.onDeviceTokenReceived("new-token")

            assertEquals("new-token", facade.deviceToken.value)
            coVerify(exactly = 0) { apiGateway.registerDevice(any(), any(), any(), any(), any()) }
        }

    @Test
    fun `registerForPushNotifications fails when API returns error`() =
        runTest {
            coEvery { tokenProvider.requestPermission() } returns true
            coEvery { tokenProvider.requestDeviceToken() } returns Result.success("valid-token")
            coEvery { apiGateway.registerDevice(any(), any(), any(), any(), any()) } returns
                Result.failure(RuntimeException("Server error"))

            val result = facade.registerForPushNotifications()

            assertTrue(result.isFailure)
            assertFalse(facade.isDeviceRegistered.value)
        }
}
