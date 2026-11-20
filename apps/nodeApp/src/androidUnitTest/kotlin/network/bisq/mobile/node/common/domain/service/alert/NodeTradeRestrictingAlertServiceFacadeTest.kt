package network.bisq.mobile.node.common.domain.service.alert

import androidx.core.util.Supplier
import bisq.bonded_roles.release.AppType
import bisq.bonded_roles.security_manager.alert.AlertService
import bisq.common.observable.collection.ObservableSet
import io.mockk.every
import io.mockk.mockk
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService
import network.bisq.mobile.node.common.test_utils.NodeKoinIntegrationTestBase
import org.junit.Test
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNull
import bisq.bonded_roles.security_manager.alert.AlertType as AlertTypeBisq2
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertData as AuthorizedAlertDataBisq2

/**
 * The node's trade services enforce the alerts of the mobile node app type, so the trade-restricting
 * dialog must show exactly those: an alert for another app type would block the UI for a trade the
 * core allows, or hide the reason the core refuses one.
 */
class NodeTradeRestrictingAlertServiceFacadeTest : NodeKoinIntegrationTestBase() {
    private val alerts = ObservableSet<AuthorizedAlertDataBisq2>()
    private lateinit var facade: NodeTradeRestrictingAlertServiceFacade

    override fun onSetup() {
        val alertService = mockk<AlertService> { every { authorizedAlertDataSet } returns alerts }
        val provider = AndroidApplicationService.Provider().apply { this.alertService = Supplier { alertService } }
        facade = NodeTradeRestrictingAlertServiceFacade(provider)
    }

    private fun haltAlert(
        id: String,
        appType: AppType,
    ) = mockk<AuthorizedAlertDataBisq2> {
        every { this@mockk.id } returns id
        every { this@mockk.appType } returns appType
        every { alertType } returns AlertTypeBisq2.EMERGENCY
        every { isTradeRestrictingAlert } returns true
        every { isHaltTrading } returns true
        every { isRequireVersionForTrading } returns false
        every { headline } returns Optional.of("Trading halted")
        every { message } returns Optional.of("Please wait for the fix.")
        every { minVersion } returns Optional.empty()
        every { date } returns 1L
    }

    @Test
    fun `shows a halt published for the mobile node`() =
        runTest {
            alerts.add(haltAlert("node-halt", AppType.MOBILE_NODE))

            facade.activate()

            assertEquals("node-halt", facade.alert.value?.id)
        }

    @Test
    fun `ignores a halt published only for the mobile client`() =
        runTest {
            alerts.add(haltAlert("client-halt", AppType.MOBILE_CLIENT))

            facade.activate()

            assertNull(facade.alert.value)
        }
}
