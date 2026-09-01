package network.bisq.mobile.data.service.accounts

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.domain.model.account.create.fiat.CreateUserDefinedFiatAccount
import network.bisq.mobile.domain.model.account.fiat.UserDefinedFiatAccount
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Guards the facade-layer contract that `runCatching` used to break: a backend call cancelled with
 * its caller must not come back as an ordinary `Result.failure` the presenter reports as an error.
 */
class UserDefinedAccountsServiceFacadeCancellationTest {
    private class TestFacade(
        private val onGetAccounts: suspend () -> Result<List<UserDefinedFiatAccount>>,
    ) : UserDefinedAccountsServiceFacade() {
        override suspend fun executeGetAccounts(): Result<List<UserDefinedFiatAccount>> = onGetAccounts()

        override suspend fun executeGetSelectedAccount(): Result<UserDefinedFiatAccount?> = Result.success(null)

        override suspend fun executeAddAccount(account: CreateUserDefinedFiatAccount): Result<UserDefinedFiatAccount> = Result.failure(NotImplementedError())

        override suspend fun executeSaveAccount(
            accountName: String,
            account: CreateUserDefinedFiatAccount,
        ): Result<UserDefinedFiatAccount> = Result.failure(NotImplementedError())

        override suspend fun executeDeleteAccount(accountName: String): Result<Unit> = Result.success(Unit)

        override suspend fun executeSetSelectedAccount(accountName: String): Result<Unit> = Result.success(Unit)
    }

    @Test
    fun `caller cancellation propagates instead of becoming a failed Result`() =
        runTest {
            val started = CompletableDeferred<Unit>()
            val gate = CompletableDeferred<Unit>()
            val facade =
                TestFacade {
                    started.complete(Unit)
                    gate.await()
                    Result.success(emptyList())
                }

            var outcome: Result<List<UserDefinedFiatAccount>>? = null
            var propagated: CancellationException? = null
            val caller =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    try {
                        outcome = facade.getAccounts()
                    } catch (e: CancellationException) {
                        propagated = e
                        throw e
                    }
                }

            started.await()
            caller.cancel()
            caller.join()

            assertNotNull(propagated)
            assertNull(outcome)
        }

    @Test
    fun `backend failure becomes a failed Result`() =
        runTest {
            val facade = TestFacade { Result.failure(IllegalStateException("boom")) }

            val result = facade.getAccounts()

            assertTrue(result.isFailure)
            assertEquals("boom", result.exceptionOrNull()?.message)
        }
}
