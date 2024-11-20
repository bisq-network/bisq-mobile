package network.bisq.mobile.presentation.di

import androidx.navigation.NavController
import androidx.navigation.NavHostController
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.AppPresenter
import network.bisq.mobile.presentation.ui.uicases.GettingStartedPresenter
import network.bisq.mobile.presentation.ui.uicases.IGettingStarted
import network.bisq.mobile.presentation.ui.uicases.startup.CreateProfilePresenter
import network.bisq.mobile.presentation.ui.uicases.startup.IOnboardingPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.ITrustedNodeSetupPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.OnBoardingPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.SplashPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.TrustedNodeSetupPresenter
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val presentationModule = module {

    single(named("RootNavController")) { getKoin().getProperty<NavHostController>("RootNavController") }
    single(named("TabNavController")) { getKoin().getProperty<NavHostController>("TabNavController") }

    single<MainPresenter> { MainPresenter(get()) } bind AppPresenter::class

    // TODO: Since NavController will be required for almost all Presenters for basic navigation
    // Added this as top constructor level param. Is this okay?
    single { (navController: NavController) ->
        SplashPresenter(
            get(),
            navController = navController,
            get()
        )
    }

    single { (navController: NavController) ->
        OnBoardingPresenter(
            get(),
            navController
        )
    } bind IOnboardingPresenter::class

    single<GettingStartedPresenter> {
        GettingStartedPresenter(
            get(),
            priceRepository = get(),
            bisqStatsRepository = get()
        )
    } bind IGettingStarted::class

    single { (navController: NavController) ->
        CreateProfilePresenter(
            get(),
            navController = navController,
            get()
        )
    }

    single { (navController: NavController) ->
        TrustedNodeSetupPresenter(
            get(),
            navController = navController,
            settingsRepository = get()
        )
    } bind ITrustedNodeSetupPresenter::class
}