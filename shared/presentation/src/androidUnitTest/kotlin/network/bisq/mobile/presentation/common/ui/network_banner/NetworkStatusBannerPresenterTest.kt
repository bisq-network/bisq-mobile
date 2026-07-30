package network.bisq.mobile.presentation.common.ui.network_banner

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.data.service.network.NetworkServiceFacade
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.UiString
import network.bisq.mobile.presentation.common.test_utils.coroutines.PresentationKoinTestBase
import network.bisq.mobile.presentation.main.MainPresenter
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkStatusBannerPresenterTest : PresentationKoinTestBase() {
    private lateinit var mainPresenter: MainPresenter
    private lateinit var networkServiceFacade: NetworkServiceFacade

    override fun onKoinReady() {
        I18nSupport.initialize("en")
        mainPresenter = mockk(relaxed = true)
        networkServiceFacade = mockk(relaxed = true)
    }

    @Test
    fun `inventoryRequestInfo emits UiString keys from allDataReceived only`() =
        runTest {
            val allDataReceived = MutableStateFlow(false)
            every { networkServiceFacade.allDataReceived } returns allDataReceived
            every { networkServiceFacade.numConnections } returns MutableStateFlow(1)
            every { mainPresenter.isMainContentVisible } returns MutableStateFlow(true)

            val presenter = NetworkStatusBannerPresenter(mainPresenter, networkServiceFacade)
            val collected = mutableListOf<UiString>()
            val job = launch { presenter.inventoryRequestInfo.collect { collected.add(it) } }
            advanceUntilIdle()

            assertEquals("mobile.inventoryRequest.requesting", collected.last().key)

            allDataReceived.value = true
            advanceUntilIdle()
            assertEquals("mobile.inventoryRequest.completed", collected.last().key)

            job.cancel()
        }
}
