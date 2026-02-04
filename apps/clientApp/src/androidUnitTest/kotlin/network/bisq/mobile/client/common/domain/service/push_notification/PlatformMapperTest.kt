package network.bisq.mobile.client.common.domain.service.push_notification

import network.bisq.mobile.domain.PlatformType
import org.junit.Test
import kotlin.test.assertEquals

class PlatformMapperTest {
    @Test
    fun `fromPlatformType maps IOS correctly`() {
        val result = PlatformMapper.fromPlatformType(PlatformType.IOS)
        assertEquals(Platform.IOS, result)
    }

    @Test
    fun `fromPlatformType maps ANDROID correctly`() {
        val result = PlatformMapper.fromPlatformType(PlatformType.ANDROID)
        assertEquals(Platform.ANDROID, result)
    }
}
