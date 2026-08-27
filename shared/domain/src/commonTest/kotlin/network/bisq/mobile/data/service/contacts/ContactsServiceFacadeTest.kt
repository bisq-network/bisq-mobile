package network.bisq.mobile.data.service.contacts

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import network.bisq.mobile.data.replicated.user.contact_list.ContactListEntryVO
import network.bisq.mobile.data.replicated.user.contact_list.ContactReasonEnum
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Pins the base-class helper queries and the interface defaults new backends inherit. */
class ContactsServiceFacadeTest {
    private class MinimalFacade : ContactsServiceFacade() {
        val backing = MutableStateFlow<List<ContactListEntryVO>>(emptyList())
        override val contacts: StateFlow<List<ContactListEntryVO>> = backing.asStateFlow()
        var lastReason: ContactReasonEnum? = null

        override suspend fun addContact(
            userProfileId: String,
            reason: ContactReasonEnum,
        ): Result<Boolean> {
            lastReason = reason
            return Result.success(true)
        }

        override suspend fun removeContact(userProfileId: String): Result<Boolean> = Result.success(true)

        override suspend fun setTag(
            userProfileId: String,
            tag: String,
        ): Result<Unit> = Result.success(Unit)

        override suspend fun setNotes(
            userProfileId: String,
            notes: String,
        ): Result<Unit> = Result.success(Unit)

        override suspend fun setTrustScore(
            userProfileId: String,
            trustScore: Double,
        ): Result<Unit> = Result.success(Unit)
    }

    @Test
    fun `isContact and findContact answer from the flow by profile id`() {
        val facade = MinimalFacade()
        val alice = createMockUserProfile("Alice")
        facade.backing.value =
            listOf(ContactListEntryVO(userProfile = alice, date = 1L, contactReason = ContactReasonEnum.MANUALLY_ADDED, tag = "t"))

        assertTrue(facade.isContact(alice.id))
        assertEquals("t", facade.findContact(alice.id)?.tag)
        assertFalse(facade.isContact("someone-else"))
        assertNull(facade.findContact("someone-else"))
    }

    @Test
    fun `addContact defaults to the MANUALLY_ADDED reason`() {
        val facade = MinimalFacade()
        runBlocking { facade.addContact("some-id") }
        assertEquals(ContactReasonEnum.MANUALLY_ADDED, facade.lastReason)
    }
}
