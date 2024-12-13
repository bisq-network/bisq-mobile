package network.bisq.mobile.presentation.di

import androidx.navigation.NavController
import androidx.navigation.NavHostController
import network.bisq.mobile.client.ClientMainPresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.AppPresenter
import network.bisq.mobile.presentation.ui.components.molecules.ITopBarPresenter
import network.bisq.mobile.presentation.ui.components.molecules.TopBarPresenter
import network.bisq.mobile.presentation.ui.uicases.GettingStartedPresenter
import network.bisq.mobile.presentation.ui.uicases.offers.IOffersListPresenter
import network.bisq.mobile.presentation.ui.uicases.offers.MarketListPresenter
import network.bisq.mobile.presentation.ui.uicases.offers.OffersListPresenter
import network.bisq.mobile.presentation.ui.uicases.offers.takeOffer.ITakeOfferPaymentMethodPresenter
import network.bisq.mobile.presentation.ui.uicases.offers.takeOffer.ITakeOfferReviewTradePresenter
import network.bisq.mobile.presentation.ui.uicases.offers.takeOffer.ITakeOfferTradeAmountPresenter
import network.bisq.mobile.presentation.ui.uicases.offers.takeOffer.PaymentMethodPresenter
import network.bisq.mobile.presentation.ui.uicases.offers.takeOffer.ReviewTradePresenter
import network.bisq.mobile.presentation.ui.uicases.offers.takeOffer.TradeAmountPresenter
import network.bisq.mobile.presentation.ui.uicases.offers.createOffer.CreateOfferPresenter
import network.bisq.mobile.presentation.ui.uicases.offers.createOffer.ICreateOfferPresenter
import network.bisq.mobile.presentation.ui.uicases.offers.takeOffer.*
import network.bisq.mobile.presentation.ui.uicases.settings.ISettingsPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.SettingsPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.CreateProfilePresenter
import network.bisq.mobile.presentation.ui.uicases.startup.IOnboardingPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.ITrustedNodeSetupPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.OnBoardingPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.SplashPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.TrustedNodeSetupPresenter
import network.bisq.mobile.presentation.ui.uicases.trades.IMyTrades
import network.bisq.mobile.presentation.ui.uicases.trades.ITradeFlowPresenter
import network.bisq.mobile.presentation.ui.uicases.trades.MyTradesPresenter
import network.bisq.mobile.presentation.ui.uicases.trades.TradeFlowPresenter
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val presentationModule = module {
    single(named("RootNavController")) { getKoin().getProperty<NavHostController>("RootNavController") }
    single(named("TabNavController")) { getKoin().getProperty<NavHostController>("TabNavController") }

    single<MainPresenter> { ClientMainPresenter(get(), get(), get(), get(), get()) } bind AppPresenter::class

    single<TopBarPresenter> { TopBarPresenter(get(), get()) } bind ITopBarPresenter::class

    single<SplashPresenter> {
        SplashPresenter(
            get(),
            get(),
            get(),
            get(),
        )
    }

    single { OnBoardingPresenter(get(), get()) } bind IOnboardingPresenter::class

    single<SettingsPresenter> { SettingsPresenter(get(), get()) } bind ISettingsPresenter::class

    single<GettingStartedPresenter> {
        GettingStartedPresenter(
            get(),
            bisqStatsRepository = get(),
            get()
        )
    }

    single {
        CreateProfilePresenter(
            get(),
            get(),
            get()
        )
    }

    single {
        TrustedNodeSetupPresenter(
            get(),
            settingsRepository = get()
        )
    } bind ITrustedNodeSetupPresenter::class

    single<MarketListPresenter> { MarketListPresenter(get(), get()) }

    single<OffersListPresenter> { OffersListPresenter(get(), get()) } bind IOffersListPresenter::class

    single<TradeAmountPresenter> { TradeAmountPresenter(get(), get()) } bind ITakeOfferTradeAmountPresenter::class

    single { PaymentMethodPresenter(get(), get()) } bind ITakeOfferPaymentMethodPresenter::class

    single{ ReviewTradePresenter(get(), get()) } bind ITakeOfferReviewTradePresenter::class

    single { (navController: NavController, tabController: NavController) ->
        MyTradesPresenter(
            get(),
            tabController = tabController,
            myTradesRepository = get()
        )
    } bind IMyTrades::class

    single{ TradeFlowPresenter(get(), get()) } bind ITradeFlowPresenter::class

    single{ CreateOfferPresenter(get(), get()) } bind ICreateOfferPresenter::class
}