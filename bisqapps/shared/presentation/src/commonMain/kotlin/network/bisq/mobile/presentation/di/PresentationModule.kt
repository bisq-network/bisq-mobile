package network.bisq.mobile.presentation.di

import androidx.navigation.NavController
import androidx.navigation.NavHostController
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.AppPresenter
import network.bisq.mobile.presentation.ui.uicases.GettingStartedPresenter
import network.bisq.mobile.presentation.ui.uicases.IGettingStarted
import network.bisq.mobile.presentation.ui.uicases.offers.CurrencyListPresenter
import network.bisq.mobile.presentation.ui.uicases.offers.ICurrencyList
import network.bisq.mobile.presentation.ui.uicases.offers.IOffersList
import network.bisq.mobile.presentation.ui.uicases.offers.OffersListPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.CreateProfilePresenter
import network.bisq.mobile.presentation.ui.uicases.startup.IOnboardingPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.ITrustedNodeSetupPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.OnBoardingPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.SplashPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.TrustedNodeSetupPresenter
import network.bisq.mobile.presentation.ui.uicases.trades.IMyTrades
import network.bisq.mobile.presentation.ui.uicases.trades.MyTradesPresenter
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val presentationModule = module {
    single(named("RootNavController")) { getKoin().getProperty<NavHostController>("RootNavController") }
    single(named("TabNavController")) { getKoin().getProperty<NavHostController>("TabNavController") }

    single<MainPresenter> { MainPresenter(get()) } bind AppPresenter::class

    single {
        SplashPresenter(
            get(),
            get()
        )
    }

    single {
        OnBoardingPresenter(
            get()
        )
    } bind IOnboardingPresenter::class

    single<GettingStartedPresenter> {
        GettingStartedPresenter(
            get(),
            priceRepository = get(),
            bisqStatsRepository = get()
        )
    } bind IGettingStarted::class

    single {
        CreateProfilePresenter(
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

    single { CurrencyListPresenter(get(), get()) } bind ICurrencyList::class

    single { OffersListPresenter(get()) } bind IOffersList::class

    single {
        (navController: NavController, tabController: NavController) -> MyTradesPresenter(
            get(),
            tabController = tabController,
            myTradesRepository = get()
        )
    } bind IMyTrades::class
}