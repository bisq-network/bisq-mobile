package network.bisq.mobile.presentation.di

import network.bisq.mobile.client.ClientMainPresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.AppPresenter
import network.bisq.mobile.presentation.ui.components.molecules.ITopBarPresenter
import network.bisq.mobile.presentation.ui.components.molecules.TopBarPresenter
import network.bisq.mobile.presentation.ui.uicases.ChatPresenter
import network.bisq.mobile.presentation.ui.uicases.GettingStartedPresenter
import network.bisq.mobile.presentation.ui.uicases.IChatPresenter
import network.bisq.mobile.presentation.ui.uicases.ITabContainerPresenter
import network.bisq.mobile.presentation.ui.uicases.TabContainerPresenter
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferAmountPresenter
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferDirectionPresenter
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferMarketPresenter
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferPaymentMethodPresenter
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferPresenter
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferPricePresenter
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferReviewPresenter
import network.bisq.mobile.presentation.ui.uicases.offerbook.OfferbookMarketPresenter
import network.bisq.mobile.presentation.ui.uicases.offerbook.OfferbookPresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.OpenTradeListPresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.InterruptedTradePresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.OpenTradePresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeDetailsHeaderPresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.*
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
import network.bisq.mobile.presentation.ui.uicases.take_offer.TakeOfferAmountPresenter
import network.bisq.mobile.presentation.ui.uicases.take_offer.TakeOfferPaymentMethodPresenter
import network.bisq.mobile.presentation.ui.uicases.take_offer.TakeOfferPresenter
import network.bisq.mobile.presentation.ui.uicases.take_offer.TakeOfferReviewPresenter
import org.koin.dsl.bind
import org.koin.dsl.module

val presentationModule = module {
    single<MainPresenter> { ClientMainPresenter(get(), get(), get(), get(), get(), get(), get(), get()) } bind AppPresenter::class

    single<TopBarPresenter> { TopBarPresenter(get(), get()) } bind ITopBarPresenter::class

    single<SplashPresenter> {
        SplashPresenter(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }

    single { OnBoardingPresenter(get(), get(), get()) } bind IOnboardingPresenter::class
    single { TabContainerPresenter(get(), get()) } bind ITabContainerPresenter::class

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
            settingsRepository = get(),
            get()
        )
    } bind ITrustedNodeSetupPresenter::class

    single { PaymentAccountPresenter(get(), get()) } bind IPaymentAccountSettingsPresenter::class

    // Offerbook
    single<OfferbookMarketPresenter> { OfferbookMarketPresenter(get(), get()) }
    single<OfferbookPresenter> { OfferbookPresenter(get(), get(), get()) }

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

    // Trade Seller
    single { SellerState1Presenter(get(), get()) }
    single { SellerState2aPresenter(get(), get()) }
    single { SellerState2bPresenter(get(), get()) }
    single { SellerState3aPresenter(get(), get()) }
    single { SellerStateMainChain3bPresenter(get(), get(), get()) }
    single { SellerStateLightning3bPresenter(get(), get()) }
    single { SellerState4Presenter(get(), get()) }

    // Trade Buyer
    single { BuyerState1aPresenter(get(), get()) }
    // BuyerState1bPresenter does not exist as it a static UI
    single { BuyerState2aPresenter(get(), get()) }
    single { BuyerState2bPresenter(get(), get()) }
    single { BuyerState3aPresenter(get(), get()) }
    single { BuyerStateMainChain3bPresenter(get(), get(), get()) }
    single { BuyerStateLightning3bPresenter(get(), get()) }
    single { BuyerState4Presenter(get(), get()) }

    // Trade General process
    single { TradeStatesProvider(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { OpenTradeListPresenter(get(), get(), get()) }
    single { TradeDetailsHeaderPresenter(get(), get()) }
    single { InterruptedTradePresenter(get(), get(), get()) }
    single { TradeFlowPresenter(get(), get(), get()) }
    single { OpenTradePresenter(get(), get(), get()) }

    single { ChatPresenter(get()) } bind IChatPresenter::class
}