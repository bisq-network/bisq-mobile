package network.bisq.mobile.client.common.domain.service.contacts

import network.bisq.mobile.client.common.test_utils.ClientKoinIntegrationTestBase
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The client contacts facade is a DORMANT STUB until the trusted-node API ships (#1238 PR 3):
 * nothing user-reachable calls it (the segment is gated off), so these tests pin the defensive
 * backstop — empty flow, every mutation a failure, helper queries answering from the empty flow.
 */
class ClientContactsServiceFacadeTest : ClientKoinIntegrationTestBase() {
    private val facade = ClientContactsServiceFacade()

    @Test
    fun `exposes an empty contacts flow`() {
        assertTrue(facade.contacts.value.isEmpty())
        assertFalse(facade.isContact("any-id"))
        assertEquals(null, facade.findContact("any-id"))
    }

    @Test
    fun `every mutation fails until the trusted-node API ships`() =
        runTest {
            assertTrue(facade.addContact("id").isFailure)
            assertTrue(facade.removeContact("id").isFailure)
            assertTrue(facade.setTag("id", "tag").isFailure)
            assertTrue(facade.setNotes("id", "notes").isFailure)
            assertTrue(facade.setTrustScore("id", 0.5).isFailure)
        }
}
