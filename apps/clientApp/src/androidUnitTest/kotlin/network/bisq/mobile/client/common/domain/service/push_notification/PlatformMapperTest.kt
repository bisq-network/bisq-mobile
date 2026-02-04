package network.bisq.mobile.client.common.domain.service.push_notification

import network.bisq.mobile.domain.PlatformType
import org.junit.Test
import kotlin.test.assertEquals

class PlatformMapperTest {
    @Test
    fun `fromPlatformType maps IOS correctly`() {
        // Given
        val platformType = PlatformType.IOS

        // When
        val result = PlatformMapper.fromPlatformType(platformType)

        // Then
        assertEquals(Platform.IOS, result)
    }

    @Test
    fun `fromPlatformType maps ANDROID correctly`() {
        // Given
        val platformType = PlatformType.ANDROID

        // When
        val result = PlatformMapper.fromPlatformType(platformType)

        // Then
        assertEquals(Platform.ANDROID, result)
    }
}
