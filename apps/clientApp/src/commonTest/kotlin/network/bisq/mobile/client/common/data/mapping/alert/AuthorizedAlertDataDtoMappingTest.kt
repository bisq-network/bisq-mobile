package network.bisq.mobile.client.common.data.mapping.alert

import network.bisq.mobile.client.common.data.model.alert.AlertTypeDto
import network.bisq.mobile.client.common.data.model.alert.AuthorizedAlertDataDto
import network.bisq.mobile.domain.model.alert.AlertType
import network.bisq.mobile.domain.model.alert.AuthorizedAlertData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthorizedAlertDataDtoMappingTest {
    @Test
    fun `toDomainOrNull trims supported alerts`() {
        val dto =
            AuthorizedAlertDataDto(
                id = "warn-1",
                alertType = AlertTypeDto.WARN,
                headline = "  Node warning  ",
                message = "  Please update soon.  ",
                haltTrading = false,
                requireVersionForTrading = true,
                minVersion = " 2.1.0 ",
                date = 42L,
            )

        val normalized = dto.toDomainOrNull()

        assertEquals(
            AuthorizedAlertData(
                id = "warn-1",
                type = AlertType.WARN,
                headline = "Node warning",
                message = "Please update soon.",
                haltTrading = false,
                requireVersionForTrading = true,
                minVersion = "2.1.0",
                date = 42L,
            ),
            normalized,
        )
    }

    @Test
    fun `toDomainOrNull drops unsupported alert types`() {
        val dto =
            AuthorizedAlertDataDto(
                id = "ban-1",
                alertType = AlertTypeDto.BAN,
                headline = "Banned",
                message = "Unsupported for mobile banner",
                date = 99L,
            )

        assertNull(dto.toDomainOrNull())
    }
}
