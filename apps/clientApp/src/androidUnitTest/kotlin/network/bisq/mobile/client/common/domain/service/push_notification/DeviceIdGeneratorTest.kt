package network.bisq.mobile.client.common.domain.service.push_notification

import android.content.ContentResolver
import android.content.Context
import android.provider.Settings
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import network.bisq.mobile.presentation.main.ApplicationContextProvider
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Android-specific tests for getDeviceId().
 * These tests verify the deterministic, hardware-based device ID generation.
 */
class DeviceIdGeneratorTest {
    private val mockContext = mockk<Context>()
    private val mockContentResolver = mockk<ContentResolver>()
    private val testAndroidId = "abc123def456789"

    @Before
    fun setup() {
        every { mockContext.applicationContext } returns mockContext
        every { mockContext.contentResolver } returns mockContentResolver

        mockkStatic(Settings.Secure::class)
        every {
            Settings.Secure.getString(mockContentResolver, Settings.Secure.ANDROID_ID)
        } returns testAndroidId

        ApplicationContextProvider.initialize(mockContext)
    }

    @After
    fun tearDown() {
        unmockkStatic(Settings.Secure::class)
        ApplicationContextProvider.reset()
    }

    @Test
    fun `when getDeviceId called multiple times then returns same value`() {
        // Given - device context already set up in @Before

        // When
        val deviceId1 = getDeviceId()
        val deviceId2 = getDeviceId()

        // Then
        assertEquals(deviceId1, deviceId2, "Same device should always produce same deviceId")
    }

    @Test
    fun `when getDeviceId called then returns non-empty string`() {
        // Given - device context already set up in @Before

        // When
        val deviceId = getDeviceId()

        // Then
        assertTrue(deviceId.isNotBlank(), "DeviceId should not be blank")
    }

    @Test
    fun `when getDeviceId called then returns the Android ID`() {
        // Given - device context already set up in @Before

        // When
        val deviceId = getDeviceId()

        // Then
        assertEquals(testAndroidId, deviceId, "DeviceId should be the Android ID")
    }

    @Test
    fun `when getDeviceId called then returns reasonable length string`() {
        // Given - device context already set up in @Before

        // When
        val deviceId = getDeviceId()

        // Then
        // Android ID is typically 16 hex characters (64 bits)
        assertTrue(deviceId.length >= 8, "DeviceId should be at least 8 characters")
    }
}
