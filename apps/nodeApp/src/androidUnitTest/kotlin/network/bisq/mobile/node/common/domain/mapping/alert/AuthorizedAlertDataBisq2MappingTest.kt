package network.bisq.mobile.node.common.domain.mapping.alert

import io.mockk.every
import io.mockk.mockk
import network.bisq.mobile.domain.model.alert.AlertType
import network.bisq.mobile.domain.model.alert.AuthorizedAlertData
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import bisq.bonded_roles.security_manager.alert.AlertType as AlertTypeBisq2
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertData as AuthorizedAlertDataBisq2

class AuthorizedAlertDataBisq2MappingTest {
    @Test
    fun `toDomainOrNull trims supported alerts`() {
        val alert =
            mockk<AuthorizedAlertDataBisq2> {
                every { id } returns "warn-1"
                every { alertType } returns AlertTypeBisq2.WARN
                every { headline } returns Optional.of("  Node warning  ")
                every { message } returns Optional.of("  Please update soon.  ")
                every { isHaltTrading } returns false
                every { isRequireVersionForTrading } returns true
                every { minVersion } returns Optional.of(" 2.1.0 ")
                every { date } returns 42L
            }

        val normalized = alert.toDomainOrNull()

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
        val alert =
            mockk<AuthorizedAlertDataBisq2> {
                every { alertType } returns AlertTypeBisq2.BAN
            }

        assertNull(alert.toDomainOrNull())
    }
}
