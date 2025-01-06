package network.bisq.mobile.presentation.di

import network.bisq.mobile.client.ClientMainPresenter
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.AppPresenter
import network.bisq.mobile.presentation.ui.components.molecules.ITopBarPresenter
import network.bisq.mobile.presentation.ui.components.molecules.TopBarPresenter
import network.bisq.mobile.presentation.ui.uicases.GettingStartedPresenter
import network.bisq.mobile.presentation.ui.uicases.offer.MarketListPresenter
import network.bisq.mobile.presentation.ui.uicases.offer.OffersListPresenter
import network.bisq.mobile.presentation.ui.uicases.offer.create_offer.CreateOfferAmountPresenter
import network.bisq.mobile.presentation.ui.uicases.offer.create_offer.CreateOfferDirectionPresenter
import network.bisq.mobile.presentation.ui.uicases.offer.create_offer.CreateOfferMarketPresenter
import network.bisq.mobile.presentation.ui.uicases.offer.create_offer.CreateOfferPaymentMethodPresenter
import network.bisq.mobile.presentation.ui.uicases.offer.create_offer.CreateOfferPresenter
import network.bisq.mobile.presentation.ui.uicases.offer.create_offer.CreateOfferPricePresenter
import network.bisq.mobile.presentation.ui.uicases.offer.create_offer.CreateOfferReviewPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.IPaymentAccountSettingsPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.ISettingsPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.IUserProfileSettingsPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.PaymentAccountPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.SettingsPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.UserProfileSettingsPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.CreateProfilePresenter
import network.bisq.mobile.presentation.ui.uicases.startup.IOnboardingPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.ITrustedNodeSetupPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.OnBoardingPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.SplashPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.TrustedNodeSetupPresenter
import network.bisq.mobile.presentation.ui.uicases.trade.take_offer.TakeOfferAmountPresenter
import network.bisq.mobile.presentation.ui.uicases.trade.take_offer.TakeOfferPaymentMethodPresenter
import network.bisq.mobile.presentation.ui.uicases.trade.take_offer.TakeOfferPresenter
import network.bisq.mobile.presentation.ui.uicases.trade.take_offer.TakeOfferReviewPresenter
import network.bisq.mobile.presentation.ui.uicases.trades.IMyTrades
import network.bisq.mobile.presentation.ui.uicases.trades.ITradeFlowPresenter
import network.bisq.mobile.presentation.ui.uicases.trades.MyTradesPresenter
import network.bisq.mobile.presentation.ui.uicases.trades.TradeFlowPresenter
import org.koin.dsl.bind
import org.koin.dsl.module

val presentationModule = module {
    single<MainPresenter> { ClientMainPresenter(get(), get(), get(), get(), get(), get()) } bind AppPresenter::class

    single<TopBarPresenter> { TopBarPresenter(get(), get()) } bind ITopBarPresenter::class

    single<SplashPresenter> {
        SplashPresenter(
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }

//    single { TabContainerPresenter(get()) } bind ITabContainerPresenter::class

    single { OnBoardingPresenter(get(), get(), get()) } bind IOnboardingPresenter::class

    single<SettingsPresenter> { SettingsPresenter(get(), get()) } bind ISettingsPresenter::class

    single<UserProfileSettingsPresenter> { UserProfileSettingsPresenter(get(), get(), get()) } bind IUserProfileSettingsPresenter::class

    single<GettingStartedPresenter> { GettingStartedPresenter(get(), get(), get(), get()) }

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

    single<OffersListPresenter> { OffersListPresenter(get(), get(), get()) }

    single {
        MyTradesPresenter(
            get(),
            myTradesRepository = get()
        )
    } bind IMyTrades::class

    single { TradeFlowPresenter(get(), get()) } bind ITradeFlowPresenter::class

    single { PaymentAccountPresenter(get(), get()) } bind IPaymentAccountSettingsPresenter::class

    // Take offer
    single { TakeOfferPresenter(get(), get(), get()) }
    single { TakeOfferAmountPresenter(get(), get()) }
    single { TakeOfferPaymentMethodPresenter(get(), get()) }
    single { TakeOfferReviewPresenter(get(), get(), get()) }

    // Create offer
    single { CreateOfferPresenter(get(), get(), get()) }
    single { CreateOfferDirectionPresenter(get(), get()) }
    single { CreateOfferMarketPresenter(get(), get(), get()) }
    single { CreateOfferPricePresenter(get(), get()) }
    single { CreateOfferAmountPresenter(get(), get(), get()) }
    single { CreateOfferPaymentMethodPresenter(get(), get()) }
    single { CreateOfferReviewPresenter(get(), get()) }
}