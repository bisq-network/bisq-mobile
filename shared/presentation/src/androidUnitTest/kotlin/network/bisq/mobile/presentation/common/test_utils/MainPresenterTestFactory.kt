package network.bisq.mobile.presentation.common.test_utils

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.data.model.TradeReadStateMap
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.data.repository.TradeReadStateRepository
import network.bisq.mobile.domain.service.bootstrap.ApplicationLifecycleService
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.common.service.OpenTradesNotificationService
import network.bisq.mobile.presentation.main.MainPresenter

object MainPresenterTestFactory {
    fun create(
        openTradeItems: StateFlow<List<TradeItemPresentationModel>> = MutableStateFlow(emptyList()),
        selectedTrade: StateFlow<TradeItemPresentationModel?> = MutableStateFlow(null),
        ignoredProfileIds: StateFlow<Set<String>> = MutableStateFlow(emptySet()),
        languageCode: StateFlow<String> = MutableStateFlow("en"),
        useAnimations: StateFlow<Boolean> = MutableStateFlow(false),
        openTradesNotificationService: OpenTradesNotificationService = mockk(relaxed = true),
        tradeReadStateRepository: TradeReadStateRepository = DefaultTradeReadStateRepositoryFake(),
        urlLauncher: UrlLauncher = mockk(relaxed = true),
        applicationLifecycleService: ApplicationLifecycleService = TestApplicationLifecycleService(),
    ): MainPresenter {
        val tradesServiceFacade = mockk<TradesServiceFacade>(relaxed = true)
        every { tradesServiceFacade.openTradeItems } returns openTradeItems
        every { tradesServiceFacade.selectedTrade } returns selectedTrade

        val userProfileServiceFacade = mockk<UserProfileServiceFacade>(relaxed = true)
        every { userProfileServiceFacade.ignoredProfileIds } returns ignoredProfileIds

        val settingsServiceFacade = mockk<SettingsServiceFacade>(relaxed = true)
        every { settingsServiceFacade.languageCode } returns languageCode
        every { settingsServiceFacade.useAnimations } returns useAnimations

        return MainPresenter(
            tradesServiceFacade = tradesServiceFacade,
            userProfileServiceFacade = userProfileServiceFacade,
            openTradesNotificationService = openTradesNotificationService,
            settingsService = settingsServiceFacade,
            tradeReadStateRepository = tradeReadStateRepository,
            urlLauncher = urlLauncher,
            applicationLifecycleService = applicationLifecycleService,
        )
    }

    // Keep the default fake small and stable.
    private class DefaultTradeReadStateRepositoryFake : TradeReadStateRepository {
        override val data: Flow<TradeReadStateMap> = flowOf(TradeReadStateMap())

        override suspend fun setCount(
            tradeId: String,
            count: Int,
        ) {
        }

        override suspend fun clearId(tradeId: String) {
        }
    }
}
