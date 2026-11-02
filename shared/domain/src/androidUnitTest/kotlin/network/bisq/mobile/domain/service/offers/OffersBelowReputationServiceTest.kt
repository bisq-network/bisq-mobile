package network.bisq.mobile.domain.service.offers

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.replicated.common.currency.MarketVOFactory
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOFactory
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOFactory.fromPrice
import network.bisq.mobile.data.replicated.common.network.AddressByTransportTypeMapVO
import network.bisq.mobile.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.offer.amount.spec.QuoteSideFixedAmountSpecVO
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationDto
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.data.replicated.security.keys.PubKeyVO
import network.bisq.mobile.data.replicated.security.keys.PublicKeyVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.data.service.network.NetworkServiceFacade
import network.bisq.mobile.data.service.offers.AuthorOffersSnapshot
import network.bisq.mobile.data.service.offers.OffersServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.coroutines.DispatcherProvider
import network.bisq.mobile.test.coroutines.StandardTestDispatcherProvider
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The node judges my offers (bisq2#5095) and sends the verdict with each item; this service reads it
 * from the offers cache and keeps what the user dismissed. An offer without a verdict comes from a
 * node before that change.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OffersBelowReputationServiceTest {
    private val me = createMockUserProfile("me")
    private val selectedProfile = MutableStateFlow<UserProfileVO?>(me)
    private val allDataReceived = MutableStateFlow(true)
    private val offersRevision = MutableStateFlow(0L)
    private val snapshots = mutableMapOf<String, AuthorOffersSnapshot>()

    private val offersService =
        mockk<OffersServiceFacade> {
            every { offersRevision } returns this@OffersBelowReputationServiceTest.offersRevision
            coEvery { offersByAuthor(any()) } answers { snapshots[firstArg()] ?: AuthorOffersSnapshot(emptyList(), false) }
            coEvery { deleteOffer(any()) } returns Result.success(true)
        }

    @Test
    fun `my offer the node marks insufficient is flagged on start`() =
        runTest {
            givenMyOffers(sellOffer())

            val service = startService()

            assertEquals(listOf("offer-1"), service.offendingIds())
            assertTrue(service.state.value.isWarningVisible)
        }

    @Test
    fun `an offer the node marks sufficient is not flagged`() =
        runTest {
            givenMyOffers(sellOffer(verdict = true))

            val service = startService()

            assertTrue(service.offendingIds().isEmpty())
            assertFalse(service.state.value.isWarningVisible)
        }

    @Test
    fun `an offer without a verdict is not flagged`() =
        runTest {
            givenMyOffers(sellOffer(verdict = null))

            val service = startService()

            assertTrue(service.offendingIds().isEmpty())
            assertFalse(service.state.value.isWarningVisible)
        }

    @Test
    fun `a flagged offer carries its market and formatted amount`() =
        runTest {
            givenMyOffers(sellOffer())

            val service = startService()

            assertEquals(
                OffendingOffer(offerId = "offer-1", market = MarketVOFactory.USD, formattedAmount = "50 USD"),
                service.state.value.offendingOffers
                    .single(),
            )
        }

    /** The node re-sends the item with the same offer id when the verdict flips. */
    @Test
    fun `a verdict the node changes is applied on the next cache change`() =
        runTest {
            givenMyOffers(sellOffer(verdict = true))
            val service = startService()
            assertTrue(service.offendingIds().isEmpty())

            givenMyOffers(sellOffer(verdict = false))
            offersChanged()
            advanceUntilIdle()

            assertEquals(listOf("offer-1"), service.offendingIds())
            assertTrue(service.state.value.isWarningVisible)
        }

    @Test
    fun `a flag that recovers clears the warning`() =
        runTest {
            givenMyOffers(sellOffer())
            val service = startService()

            givenMyOffers(sellOffer(verdict = true))
            offersChanged()
            advanceUntilIdle()

            assertTrue(service.offendingIds().isEmpty())
            assertFalse(service.state.value.isWarningVisible)
        }

    /** Sold, or deleted on another device: the REMOVED event drops it from the cache. */
    @Test
    fun `an offer that left the cache is dropped from the warning`() =
        runTest {
            givenMyOffers(sellOffer(id = "gone"), sellOffer(id = "kept"))
            val service = startService()

            givenMyOffers(sellOffer(id = "kept"))
            offersChanged()
            advanceUntilIdle()

            assertEquals(listOf("kept"), service.offendingIds())
        }

    @Test
    fun `no check runs before the initial data is received`() =
        runTest {
            allDataReceived.value = false
            givenMyOffers(sellOffer())

            val service = startService()
            assertTrue(service.offendingIds().isEmpty())

            allDataReceived.value = true
            advanceUntilIdle()

            assertEquals(listOf("offer-1"), service.offendingIds())
        }

    @Test
    fun `flagged offers clear and the warning hides while the node resyncs`() =
        runTest {
            givenMyOffers(sellOffer())
            val service = startService()
            assertEquals(listOf("offer-1"), service.offendingIds())

            allDataReceived.value = false
            advanceUntilIdle()

            assertTrue(service.offendingIds().isEmpty())
            assertFalse(service.state.value.isWarningVisible)
        }

    @Test
    fun `no check runs while the node resyncs`() =
        runTest {
            givenMyOffers(sellOffer(verdict = true))
            val service = startService()

            allDataReceived.value = false
            givenMyOffers(sellOffer(verdict = false))
            offersChanged()
            advanceUntilIdle()

            assertTrue(service.offendingIds().isEmpty())
        }

    @Test
    fun `offers are checked again once the node has resynced and dismissed ones stay dismissed`() =
        runTest {
            givenMyOffers(sellOffer())
            val service = startService()
            service.dismiss()
            allDataReceived.value = false
            advanceUntilIdle()

            givenMyOffers(sellOffer(), sellOffer(id = "offer-2"))
            allDataReceived.value = true
            advanceUntilIdle()

            assertEquals(listOf("offer-1", "offer-2"), service.offendingIds())
            assertTrue(service.state.value.isWarningVisible)
            service.dismiss()
            assertFalse(service.state.value.isWarningVisible)
        }

    @Test
    fun `switching profile checks the new profile's offers`() =
        runTest {
            val other = createMockUserProfile("other")
            givenMyOffers(sellOffer(id = "mine"))
            snapshots[other.id] = AuthorOffersSnapshot(listOf(sellOffer(id = "theirs")), false)
            val service = startService()

            selectedProfile.value = other
            advanceUntilIdle()

            assertEquals(listOf("theirs"), service.offendingIds())
        }

    @Test
    fun `a refresh after switching profile does not check the previous profile`() =
        runTest {
            val other = createMockUserProfile("other")
            givenMyOffers(sellOffer(id = "mine"))
            val service = startService()

            selectedProfile.value = other
            advanceUntilIdle()
            service.refresh()
            advanceUntilIdle()

            assertTrue(service.offendingIds().isEmpty())
            assertFalse(service.state.value.isWarningVisible)
            coVerify(exactly = 1) { offersService.offersByAuthor(me.id) }
        }

    @Test
    fun `a check still reading the previous profile's offers does not publish after a switch`() =
        runTest {
            val other = createMockUserProfile("other")
            val read = CompletableDeferred<Unit>()
            // A read that returns although its check was cancelled meanwhile, as one that never suspends does.
            coEvery { offersService.offersByAuthor(me.id) } coAnswers {
                withContext(NonCancellable) { read.await() }
                AuthorOffersSnapshot(listOf(sellOffer(id = "mine")), mayBeIncomplete = false)
            }
            val service = startService()

            selectedProfile.value = other
            advanceUntilIdle()
            read.complete(Unit)
            advanceUntilIdle()

            assertTrue(service.offendingIds().isEmpty())
        }

    @Test
    fun `editing my profile keeps what I dismissed`() =
        runTest {
            givenMyOffers(sellOffer())
            val service = startService()
            service.dismiss()

            selectedProfile.value = me.copy(statement = "edited")
            advanceUntilIdle()
            service.refresh()
            advanceUntilIdle()

            assertEquals(listOf("offer-1"), service.offendingIds())
            assertFalse(service.state.value.isWarningVisible)
        }

    @Test
    fun `dismiss hides the warning but keeps the offers for the badge`() =
        runTest {
            givenMyOffers(sellOffer())
            val service = startService()

            service.dismiss()

            assertFalse(service.state.value.isWarningVisible)
            assertEquals(listOf("offer-1"), service.offendingIds())
        }

    @Test
    fun `after dismiss the same offers do not show the warning again`() =
        runTest {
            givenMyOffers(sellOffer())
            val service = startService()
            service.dismiss()

            service.refresh()
            advanceUntilIdle()

            assertFalse(service.state.value.isWarningVisible)
        }

    @Test
    fun `dismiss is remembered per profile`() =
        runTest {
            val other = createMockUserProfile("other")
            givenMyOffers(sellOffer(id = "mine"))
            snapshots[other.id] = AuthorOffersSnapshot(listOf(sellOffer(id = "theirs")), false)
            val service = startService()
            service.dismiss()
            selectedProfile.value = other
            advanceUntilIdle()
            assertTrue(service.state.value.isWarningVisible)
            service.dismiss()

            selectedProfile.value = me
            advanceUntilIdle()

            assertEquals(listOf("mine"), service.offendingIds())
            assertFalse(service.state.value.isWarningVisible)
        }

    @Test
    fun `after dismiss a newly flagged offer shows the warning again`() =
        runTest {
            givenMyOffers(sellOffer(id = "first"))
            val service = startService()
            service.dismiss()

            givenMyOffers(sellOffer(id = "first"), sellOffer(id = "second"))
            service.refresh()
            advanceUntilIdle()

            assertTrue(service.state.value.isWarningVisible)
            assertEquals(listOf("first", "second"), service.offendingIds())
        }

    @Test
    fun `a dismissed offer that is flagged again after another dismiss stays dismissed`() =
        runTest {
            givenMyOffers(sellOffer(id = "a"), sellOffer(id = "b"))
            val service = startService()
            service.dismiss()
            givenMyOffers(sellOffer(id = "a"), sellOffer(id = "c"))
            service.refresh()
            advanceUntilIdle()
            service.dismiss()

            givenMyOffers(sellOffer(id = "a"), sellOffer(id = "b"), sellOffer(id = "c"))
            service.refresh()
            advanceUntilIdle()

            assertFalse(service.state.value.isWarningVisible)
        }

    @Test
    fun `a refresh re-checks my offers`() =
        runTest {
            val service = startService()
            assertTrue(service.offendingIds().isEmpty())

            givenMyOffers(sellOffer())
            service.refresh()
            advanceUntilIdle()

            assertEquals(listOf("offer-1"), service.offendingIds())
        }

    @Test
    fun `remove deletes each offer once and clears the warning`() =
        runTest {
            givenMyOffers(sellOffer(id = "a"), sellOffer(id = "b"))
            val service = startService()

            val outcome = service.removeOffers()

            assertEquals(OffersBelowReputationService.RemoveOutcome.REMOVED_ALL, outcome)
            coVerify(exactly = 1) { offersService.deleteOffer("a") }
            coVerify(exactly = 1) { offersService.deleteOffer("b") }
            val state = service.state.value
            assertTrue(state.offendingOffers.isEmpty())
            assertFalse(state.isWarningVisible)
            assertFalse(state.isRemoving)
        }

    @Test
    fun `failed removals stay flagged with an error mark`() =
        runTest {
            givenMyOffers(sellOffer(id = "ok"), sellOffer(id = "rejected"), sellOffer(id = "thrown"))
            coEvery { offersService.deleteOffer("rejected") } returns Result.success(false)
            coEvery { offersService.deleteOffer("thrown") } throws IllegalStateException("boom")
            val service = startService()

            val outcome = service.removeOffers()

            assertEquals(OffersBelowReputationService.RemoveOutcome.SOME_FAILED, outcome)
            val state = service.state.value
            assertEquals(listOf("rejected", "thrown"), service.offendingIds())
            assertTrue(state.offendingOffers.all { it.hasRemoveError })
            assertTrue(state.isWarningVisible)
            assertFalse(state.isRemoving)
        }

    @Test
    fun `a failed removal of an offer that is already gone clears it`() =
        runTest {
            givenMyOffers(sellOffer())
            coEvery { offersService.deleteOffer("offer-1") } returns Result.success(false)
            val service = startService()
            givenMyOffers() // removed elsewhere, so the delete has nothing to find

            service.removeOffers()
            advanceUntilIdle()

            assertTrue(service.offendingIds().isEmpty())
            assertFalse(service.state.value.isWarningVisible)
        }

    @Test
    fun `a removal cancelled by a dropped connection fails and the rest are still removed`() =
        runTest {
            givenMyOffers(sellOffer(id = "dropped"), sellOffer(id = "ok"))
            coEvery { offersService.deleteOffer("dropped") } throws CancellationException("request disposed")
            val service = startService()

            val outcome = service.removeOffers()

            assertEquals(OffersBelowReputationService.RemoveOutcome.SOME_FAILED, outcome)
            coVerify(exactly = 1) { offersService.deleteOffer("ok") }
            assertEquals(listOf("dropped"), service.offendingIds())
            assertTrue(
                service.state.value.offendingOffers
                    .single()
                    .hasRemoveError,
            )
        }

    @Test
    fun `a remove error survives a later check`() =
        runTest {
            givenMyOffers(sellOffer())
            coEvery { offersService.deleteOffer("offer-1") } returns Result.success(false)
            val service = startService()
            service.removeOffers()

            service.refresh()
            advanceUntilIdle()

            assertTrue(
                service.state.value.offendingOffers
                    .single()
                    .hasRemoveError,
            )
        }

    @Test
    fun `a cache change during a remove is applied at once`() =
        runTest {
            givenMyOffers(sellOffer())
            val deletion = CompletableDeferred<Result<Boolean>>()
            coEvery { offersService.deleteOffer("offer-1") } coAnswers { deletion.await() }
            val service = startService()
            launch { service.removeOffers() }
            advanceUntilIdle()

            givenMyOffers(sellOffer(verdict = true))
            offersChanged()
            advanceUntilIdle()

            assertTrue(service.offendingIds().isEmpty())
            assertFalse(service.state.value.isWarningVisible)
            deletion.complete(Result.success(false))
            advanceUntilIdle()
            assertTrue(service.offendingIds().isEmpty())
        }

    /** Every delete is a round-trip, on Bisq Connect over Tor: a teardown must not wait for them. */
    @Test
    fun `stop during a remove does not wait for the deletes`() =
        runTest {
            givenMyOffers(sellOffer())
            val deletion = CompletableDeferred<Result<Boolean>>()
            coEvery { offersService.deleteOffer("offer-1") } coAnswers { deletion.await() }
            val service = startService()
            launch { service.removeOffers() }
            advanceUntilIdle()

            val stopped = launch { service.stop() }
            advanceUntilIdle()

            assertTrue(stopped.isCompleted)
            assertTrue(service.offendingIds().isEmpty())
            deletion.complete(Result.success(true))
        }

    @Test
    fun `a removed offer still in the cache does not come back`() =
        runTest {
            givenMyOffers(sellOffer())
            val service = startService()
            service.removeOffers()

            // Bisq Connect drops the offer from its cache only when the REMOVED event arrives.
            service.refresh()
            advanceUntilIdle()

            assertTrue(service.offendingIds().isEmpty())
            assertFalse(service.state.value.isWarningVisible)
        }

    @Test
    fun `a second remove while removing is ignored`() =
        runTest {
            givenMyOffers(sellOffer())
            val deletion = CompletableDeferred<Result<Boolean>>()
            coEvery { offersService.deleteOffer(any()) } coAnswers { deletion.await() }
            val service = startService()

            val first = async { service.removeOffers() }
            advanceUntilIdle()
            val second = service.removeOffers()
            deletion.complete(Result.success(true))

            assertEquals(OffersBelowReputationService.RemoveOutcome.REMOVED_ALL, first.await())
            assertEquals(OffersBelowReputationService.RemoveOutcome.ALREADY_RUNNING, second)
            coVerify(exactly = 1) { offersService.deleteOffer("offer-1") }
        }

    @Test
    fun `an offer deleted elsewhere is dropped for good`() =
        runTest {
            givenMyOffers(sellOffer())
            val service = startService()
            service.dismiss()

            service.markRemoved("offer-1")
            service.refresh()
            advanceUntilIdle()

            assertTrue(service.offendingIds().isEmpty())
        }

    @Test
    fun `deleting a newly flagged offer leaves a dismissed one dismissed`() =
        runTest {
            givenMyOffers(sellOffer(id = "dismissed"))
            val service = startService()
            service.dismiss()
            givenMyOffers(sellOffer(id = "dismissed"), sellOffer(id = "new"))
            service.refresh()
            advanceUntilIdle()
            assertTrue(service.state.value.isWarningVisible)

            service.markRemoved("new")

            assertEquals(listOf("dismissed"), service.offendingIds())
            assertFalse(service.state.value.isWarningVisible)
        }

    @Test
    fun `an offer deleted elsewhere while a check judges it does not come back`() =
        runTest {
            givenMyOffers(sellOffer())
            val service = startService()
            // The deletion lands after the check started reading, as it can from another thread.
            coEvery { offersService.offersByAuthor(me.id) } coAnswers {
                service.markRemoved("offer-1")
                AuthorOffersSnapshot(listOf(sellOffer()), mayBeIncomplete = false)
            }

            service.refresh()
            advanceUntilIdle()

            assertTrue(service.offendingIds().isEmpty())
        }

    @Test
    fun `offers deleted elsewhere at the same time are all dropped`() =
        runTest {
            val ids = (1..CONCURRENT_DELETIONS).map { "offer-$it" }
            givenMyOffers(*ids.map { sellOffer(id = it) }.toTypedArray())
            val service = startService()

            ids.chunked(CONCURRENT_DELETIONS / 4).map { chunk -> thread { chunk.forEach(service::markRemoved) } }.forEach { it.join() }
            service.refresh()
            advanceUntilIdle()

            assertTrue(service.offendingIds().isEmpty())
        }

    /** The node bumps the revision per message of every market: a gossip burst must cost one read. */
    @Test
    fun `cache changes in a row are checked once`() =
        runTest {
            val service = startService()

            offersChanged()
            advanceTimeBy(100)
            offersChanged()
            advanceUntilIdle()

            coVerify(exactly = 2) { offersService.offersByAuthor(me.id) }
        }

    @Test
    fun `starting twice checks once`() =
        runTest {
            givenMyOffers(sellOffer())
            val service = startService()

            service.start()
            advanceUntilIdle()

            coVerify(exactly = 1) { offersService.offersByAuthor(me.id) }
        }

    @Test
    fun `stop clears the flagged offers`() =
        runTest {
            givenMyOffers(sellOffer())
            val service = startService()

            service.stop()

            assertTrue(service.offendingIds().isEmpty())
            assertFalse(service.state.value.isWarningVisible)
        }

    private fun TestScope.startService(): OffersBelowReputationService {
        val service = newService(StandardTestDispatcherProvider(StandardTestDispatcher(testScheduler)))
        service.start()
        advanceUntilIdle()
        return service
    }

    private fun newService(dispatcherProvider: DispatcherProvider): OffersBelowReputationService {
        val userProfileService =
            mockk<UserProfileServiceFacade> {
                every { selectedUserProfile } returns this@OffersBelowReputationServiceTest.selectedProfile
            }
        val networkService =
            mockk<NetworkServiceFacade> {
                every { allDataReceived } returns this@OffersBelowReputationServiceTest.allDataReceived
            }
        return OffersBelowReputationService(
            offersServiceFacade = offersService,
            userProfileServiceFacade = userProfileService,
            networkServiceFacade = networkService,
            dispatcherProvider = dispatcherProvider,
        )
    }

    private fun givenMyOffers(vararg offers: OfferItemPresentationModel) {
        snapshots[me.id] = AuthorOffersSnapshot(offers.toList(), mayBeIncomplete = false)
    }

    /** What the facade does after every OFFERS event, REST prefetch or node-side change. */
    private fun offersChanged() {
        offersRevision.value++
    }

    private fun sellOffer(
        id: String = "offer-1",
        market: MarketVO = MarketVOFactory.USD,
        verdict: Boolean? = false,
    ): OfferItemPresentationModel {
        val offer =
            BisqEasyOfferVO(
                id = id,
                date = 0L,
                makerNetworkId =
                    NetworkIdVO(
                        AddressByTransportTypeMapVO(mapOf()),
                        PubKeyVO(PublicKeyVO("pub"), keyId = "key", hash = "hash", id = me.id),
                    ),
                direction = DirectionEnum.SELL,
                market = market,
                // $50 fixed amount.
                amountSpec = QuoteSideFixedAmountSpecVO(50_0000L),
                priceSpec = FixPriceSpecVO(with(PriceQuoteVOFactory) { fromPrice(USD_PRICE_VALUE, market) }),
                protocolTypes = emptyList(),
                baseSidePaymentMethodSpecs = emptyList(),
                quoteSidePaymentMethodSpecs = emptyList(),
                offerOptions = emptyList(),
                supportedLanguageCodes = emptyList(),
            )
        return OfferItemPresentationModel(
            OfferItemPresentationDto(
                bisqEasyOffer = offer,
                isMyOffer = true,
                userProfile = me,
                formattedDate = "",
                formattedQuoteAmount = "50 USD",
                formattedBaseAmount = "",
                formattedPrice = "",
                formattedPriceSpec = "",
                quoteSidePaymentMethods = emptyList(),
                baseSidePaymentMethods = emptyList(),
                reputationScore = ReputationScoreVO(0, 0.0, 0),
                hasSellerSufficientReputation = verdict,
            ),
        )
    }

    private fun OffersBelowReputationService.offendingIds(): List<String> = state.value.offendingOffers.map { it.offerId }

    private companion object {
        const val USD_PRICE_VALUE = 100_000_00L

        // Enough for unsynchronised writes to the removed ids to collide.
        const val CONCURRENT_DELETIONS = 2_000
    }
}
