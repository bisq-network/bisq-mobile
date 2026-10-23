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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettings
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepositoryMock
import network.bisq.mobile.client.common.test_utils.ClientKoinIntegrationTestBase
import network.bisq.mobile.data.crypto.PushNotificationKeyStore
import network.bisq.mobile.data.crypto.pushNotificationKeyStoreFactory
import network.bisq.mobile.data.crypto.readPushNotificationKeyCandidatesBase64
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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Switching trusted node must stop the node being left from pushing to this device, and the next
 * pairing must register the device again.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ClientPushNotificationServiceFacadeNodeChangeTest : ClientKoinIntegrationTestBase() {
    private val apiGateway: PushNotificationApiGateway = mockk(relaxed = true)
    private val tokenProvider: PushNotificationTokenProvider = mockk(relaxed = true)
    private val userProfileServiceFacade: UserProfileServiceFacade = mockk(relaxed = true)
    private val mockContext = mockk<Context>()
    private val mockContentResolver = mockk<ContentResolver>()
    private val keyStore = InMemoryKeyStore()

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
                    addressByTransportTypeMap = AddressByTransportTypeMapVO(emptyMap()),
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

    private val savedKeyStoreFactory = pushNotificationKeyStoreFactory

    override fun onSetup() {
        every { tokenProvider.isSupported } returns true
        every { tokenProvider.revokeStopsDelivery } returns true
        coEvery { tokenProvider.requestPermission() } returns true
        coEvery { tokenProvider.requestDeviceToken() } returns Result.success("device-token")
        coEvery { tokenProvider.revokeDeviceToken() } returns Result.success(Unit)
        coEvery { apiGateway.registerDevice(any(), any(), any(), any(), any(), any()) } returns Result.success(Unit)
        coEvery { apiGateway.unregisterDevice(any()) } returns Result.success(Unit)
        every { apiGateway.isNodeConnected() } returns true

        every { mockContext.applicationContext } returns mockContext
        every { mockContext.contentResolver } returns mockContentResolver
        mockkStatic(Settings.Secure::class)
        every { Settings.Secure.getString(mockContentResolver, Settings.Secure.ANDROID_ID) } returns "test-android-id"
        ApplicationContextProvider.initialize(mockContext)
        pushNotificationKeyStoreFactory = { keyStore }

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
                pushNotificationKeyStoreFactory = savedKeyStoreFactory
            }
        } finally {
            super.onTearDown()
        }
    }

    private suspend fun activateRegisteredWithOldNode() {
        sensitiveSettingsRepository.update { SensitiveSettings(bisqApiUrl = "http://old-node.onion:8090", clientId = "old-client") }
        settingsRepository.update { it.copy(pushNotificationsEnabled = true) }
        facade.activate()
    }

    // advanceUntilIdle() skips backgroundScope work, so assertions on this list follow a runCurrent().
    private fun TestScope.collectFailures(): List<Unit> {
        val failures = mutableListOf<Unit>()
        backgroundScope.launch { facade.releaseFailures.collect { failures.add(it) } }
        return failures
    }

    @Test
    fun `release revokes the token and deletes the keys but keeps the opt-in`() =
        runTest {
            activateRegisteredWithOldNode()
            advanceUntilIdle()
            assertTrue(facade.isDeviceRegistered.value)

            facade.releaseCurrentNode()

            coVerify(exactly = 1) { tokenProvider.revokeDeviceToken() }
            assertEquals(emptyList(), readPushNotificationKeyCandidatesBase64())
            assertFalse(facade.isDeviceRegistered.value)
            assertTrue(settingsRepository.fetch().pushNotificationsEnabled, "switching node is not an opt-out")
        }

    @Test
    fun `release unregisters the device from the connected node`() =
        runTest {
            activateRegisteredWithOldNode()
            advanceUntilIdle()

            facade.releaseCurrentNode()

            coVerify(exactly = 1) { apiGateway.unregisterDevice(any()) }
        }

    @Test
    fun `release does not wait for a node that is not connected and reports it`() =
        runTest {
            every { tokenProvider.revokeStopsDelivery } returns false
            every { apiGateway.isNodeConnected() } returns false
            activateRegisteredWithOldNode()
            advanceUntilIdle()
            val failures = collectFailures()
            advanceUntilIdle()

            facade.releaseCurrentNode()
            runCurrent()

            coVerify(exactly = 0) { apiGateway.unregisterDevice(any()) }
            assertEquals(1, failures.size)
        }

    @Test
    fun `release reports a failure when the node refuses and revoking cannot stop delivery`() =
        runTest {
            // APNs usually hands back the same token, so on iOS only the node can stop its pushes.
            every { tokenProvider.revokeStopsDelivery } returns false
            coEvery { apiGateway.unregisterDevice(any()) } returns Result.failure(Exception("refused"))
            activateRegisteredWithOldNode()
            advanceUntilIdle()
            val failures = collectFailures()
            advanceUntilIdle()

            facade.releaseCurrentNode()
            runCurrent()

            assertEquals(1, failures.size)
        }

    @Test
    fun `release stays silent when the node refuses but revoking the token stops delivery`() =
        runTest {
            // FCM rejects the revoked token, and the node drops a registration the relay reports as gone.
            coEvery { apiGateway.unregisterDevice(any()) } returns Result.failure(Exception("refused"))
            activateRegisteredWithOldNode()
            advanceUntilIdle()
            val failures = collectFailures()
            advanceUntilIdle()

            facade.releaseCurrentNode()
            runCurrent()

            assertEquals(0, failures.size)
        }

    @Test
    fun `release reports a failure when both the node and the token revoke fail`() =
        runTest {
            coEvery { apiGateway.unregisterDevice(any()) } returns Result.failure(Exception("refused"))
            coEvery { tokenProvider.revokeDeviceToken() } returns Result.failure(Exception("FCM unavailable"))
            activateRegisteredWithOldNode()
            advanceUntilIdle()
            val failures = collectFailures()
            advanceUntilIdle()

            facade.releaseCurrentNode()
            runCurrent()

            assertEquals(1, failures.size)
        }

    @Test
    fun `release gives up on a connected node that never answers and reports it`() =
        runTest {
            every { tokenProvider.revokeStopsDelivery } returns false
            coEvery { apiGateway.unregisterDevice(any()) } coAnswers { awaitCancellation() }
            activateRegisteredWithOldNode()
            advanceUntilIdle()
            val failures = collectFailures()
            advanceUntilIdle()

            val release = launch { facade.releaseCurrentNode() }
            advanceTimeBy(ClientPushNotificationServiceFacade.RELEASE_UNREGISTER_TIMEOUT_MS + 1)
            runCurrent()

            assertTrue(release.isCompleted)
            assertEquals(1, failures.size)
        }

    @Test
    fun `release does nothing when push notifications are off`() =
        runTest {
            sensitiveSettingsRepository.update { SensitiveSettings(bisqApiUrl = "http://old-node.onion:8090", clientId = "old-client") }
            facade.activate()
            advanceUntilIdle()
            val failures = collectFailures()

            facade.releaseCurrentNode()
            runCurrent()

            coVerify(exactly = 0) { tokenProvider.revokeDeviceToken() }
            coVerify(exactly = 0) { apiGateway.unregisterDevice(any()) }
            assertEquals(0, failures.size)
        }

    @Test
    fun `pairing the next node registers the device with it again`() =
        runTest {
            activateRegisteredWithOldNode()
            advanceUntilIdle()
            coVerify(exactly = 1) { apiGateway.registerDevice(any(), any(), any(), any(), any(), any()) }

            facade.releaseCurrentNode()
            sensitiveSettingsRepository.clear()
            advanceUntilIdle()
            sensitiveSettingsRepository.update { SensitiveSettings(bisqApiUrl = "http://new-node.onion:8090", clientId = "new-client") }
            advanceUntilIdle()

            coVerify(exactly = 2) { apiGateway.registerDevice(any(), any(), any(), any(), any(), any()) }
            assertTrue(facade.isDeviceRegistered.value)
        }

    @Test
    fun `a registration requested during the release fetches its token after the revoke`() =
        runTest {
            // Fetching the token before the revoke completes would register the next node with a token
            // the revoke is about to delete.
            activateRegisteredWithOldNode()
            advanceUntilIdle()
            val revokeCanFinish = CompletableDeferred<Unit>()
            var revokeFinished = false
            val revokeFinishedAtTokenFetch = mutableListOf<Boolean>()
            coEvery { tokenProvider.revokeDeviceToken() } coAnswers {
                revokeCanFinish.await()
                revokeFinished = true
                Result.success(Unit)
            }
            coEvery { tokenProvider.requestDeviceToken() } coAnswers {
                revokeFinishedAtTokenFetch.add(revokeFinished)
                Result.success("new-device-token")
            }

            val release = launch { facade.releaseCurrentNode() }
            advanceUntilIdle()
            val registration = launch { facade.registerForPushNotifications() }
            advanceUntilIdle()
            revokeCanFinish.complete(Unit)
            release.join()
            registration.join()

            assertEquals(listOf(true), revokeFinishedAtTokenFetch)
        }

    @Test
    fun `a token request that never answers fails and does not block the release`() =
        runTest {
            activateRegisteredWithOldNode()
            advanceUntilIdle()
            coEvery { tokenProvider.requestDeviceToken() } coAnswers { awaitCancellation() }

            val registration = async { facade.registerForPushNotifications() }
            runCurrent()
            val release = launch { facade.releaseCurrentNode() }
            advanceTimeBy(ClientPushNotificationServiceFacade.TOKEN_REQUEST_TIMEOUT_MS + 1)
            runCurrent()

            assertTrue(registration.await().isFailure)
            assertTrue(release.isCompleted)
            coVerify(exactly = 1) { tokenProvider.revokeDeviceToken() }
        }

    private class InMemoryKeyStore : PushNotificationKeyStore {
        private var stored: String? = null
        private var previous: String? = null

        override fun put(base64: String) {
            previous = stored
            stored = base64
        }

        override fun get(): String? = stored

        override fun getPrevious(): String? = previous

        override fun clear() {
            stored = null
            previous = null
        }
    }
}
