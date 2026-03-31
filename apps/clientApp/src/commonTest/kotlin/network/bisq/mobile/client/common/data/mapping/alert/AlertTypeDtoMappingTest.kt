package network.bisq.mobile.client.common.data.mapping.alert

import network.bisq.mobile.client.common.data.model.alert.AlertTypeDto
import network.bisq.mobile.domain.model.alert.AlertType
import kotlin.test.Test
import kotlin.test.assertEquals

class AlertTypeDtoMappingTest {
    @Test
    fun `toAlertTypeOrNull maps all supported alert types`() {
        assertEquals(AlertType.INFO, AlertTypeDto.INFO.toAlertTypeOrNull())
        assertEquals(AlertType.WARN, AlertTypeDto.WARN.toAlertTypeOrNull())
        assertEquals(AlertType.EMERGENCY, AlertTypeDto.EMERGENCY.toAlertTypeOrNull())
        assertEquals(AlertType.BAN, AlertTypeDto.BAN.toAlertTypeOrNull())
        assertEquals(AlertType.BANNED_ACCOUNT_DATA, AlertTypeDto.BANNED_ACCOUNT_DATA.toAlertTypeOrNull())
    }
}
