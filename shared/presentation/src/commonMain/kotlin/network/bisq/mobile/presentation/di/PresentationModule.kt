package network.bisq.mobile.presentation.di

import network.bisq.mobile.client.ClientMainPresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.getPlatformCurrentTimeProvider
import network.bisq.mobile.presentation.ui.AppPresenter
import network.bisq.mobile.presentation.ui.components.molecules.ITopBarPresenter
import network.bisq.mobile.presentation.ui.components.molecules.TopBarPresenter
import network.bisq.mobile.presentation.ui.helpers.TimeProvider
import network.bisq.mobile.presentation.ui.uicases.DashboardPresenter
import network.bisq.mobile.presentation.ui.uicases.ITabContainerPresenter
import network.bisq.mobile.presentation.ui.uicases.TabContainerPresenter
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferAmountPresenter
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferDirectionPresenter
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferMarketPresenter
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferPaymentMethodPresenter
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferPresenter
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferPricePresenter
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferReviewPresenter
import network.bisq.mobile.presentation.ui.uicases.guide.TradeGuideOverviewPresenter
import network.bisq.mobile.presentation.ui.uicases.guide.TradeGuideProcessPresenter
import network.bisq.mobile.presentation.ui.uicases.guide.TradeGuideSecurityPresenter
import network.bisq.mobile.presentation.ui.uicases.guide.TradeGuideTradeRulesPresenter
import network.bisq.mobile.presentation.ui.uicases.guide.WalletGuideDownloadPresenter
import network.bisq.mobile.presentation.ui.uicases.guide.WalletGuideIntroPresenter
import network.bisq.mobile.presentation.ui.uicases.guide.WalletGuideNewPresenter
import network.bisq.mobile.presentation.ui.uicases.guide.WalletGuideReceivingPresenter
import network.bisq.mobile.presentation.ui.uicases.offerbook.OfferbookMarketPresenter
import network.bisq.mobile.presentation.ui.uicases.offerbook.OfferbookPresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.OpenTradeListPresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.InterruptedTradePresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.OpenTradePresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeDetailsHeaderPresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.BuyerState1aPresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.BuyerState2aPresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.BuyerState2bPresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.BuyerState3aPresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.BuyerState4Presenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.BuyerStateLightning3bPresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.BuyerStateMainChain3bPresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.SellerState1Presenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.SellerState2aPresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.SellerState2bPresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.SellerState3aPresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.SellerState4Presenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.SellerStateLightning3bPresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.SellerStateMainChain3bPresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.TradeStatesProvider
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.trade_chat.TradeChatPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.IGeneralSettingsPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.IIgnoredUsersPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.IPaymentAccountSettingsPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.IUserProfilePresenter
import network.bisq.mobile.presentation.ui.uicases.settings.IgnoredUsersPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.MiscItemsPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.PaymentAccountsPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.ReputationPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.ResourcesPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.SettingsPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.SupportPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.UserProfilePresenter
import network.bisq.mobile.presentation.ui.uicases.startup.CreateProfilePresenter
import network.bisq.mobile.presentation.ui.uicases.startup.IAgreementPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.SplashPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.TrustedNodeSetupPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.UserAgreementPresenter
import network.bisq.mobile.presentation.ui.uicases.take_offer.TakeOfferAmountPresenter
import network.bisq.mobile.presentation.ui.uicases.take_offer.TakeOfferPaymentMethodPresenter
import network.bisq.mobile.presentation.ui.uicases.take_offer.TakeOfferPresenter
import network.bisq.mobile.presentation.ui.uicases.take_offer.TakeOfferReviewPresenter
import org.koin.dsl.bind
import org.koin.dsl.module

val presentationModule = module {
    single<MainPresenter> {
        ClientMainPresenter(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    } bind AppPresenter::class

    single<TopBarPresenter> { TopBarPresenter(get(), get(), get(), get()) } bind ITopBarPresenter::class

    single<SplashPresenter> {
        SplashPresenter(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }

    factory<UserAgreementPresenter> { UserAgreementPresenter(get(), get()) } bind IAgreementPresenter::class

    single { TabContainerPresenter(get(), get(), get()) } bind ITabContainerPresenter::class

    single<MiscItemsPresenter> { MiscItemsPresenter(get(), get(), get()) }

    single<ReputationPresenter> { ReputationPresenter(get(), get()) }

    single<SupportPresenter> { SupportPresenter(get(), get(), get()) }

    single<ResourcesPresenter> { ResourcesPresenter(get(), get(), get()) }

    single<UserProfilePresenter> {
        UserProfilePresenter(
            get(),
            get(),
            get(),
            get(),
            get()
        )
    } bind IUserProfilePresenter::class

    single<DashboardPresenter> { DashboardPresenter(get(), get(), get(), get(), get(), get(), get(), get()) }

    single {
        CreateProfilePresenter(
            get(),
            get(),
            get()
        )
    }

    single<TrustedNodeSetupPresenter> { TrustedNodeSetupPresenter(get(), get(), get(), get()) }

    factory { SettingsPresenter(get(), get(), get()) } bind IGeneralSettingsPresenter::class

    factory { IgnoredUsersPresenter(get(), get()) } bind IIgnoredUsersPresenter::class

    single { PaymentAccountsPresenter(get(), get()) } bind IPaymentAccountSettingsPresenter::class

    // Offerbook
    single<OfferbookMarketPresenter> { OfferbookMarketPresenter(get(), get(), get(), get()) }
    single<OfferbookPresenter> { OfferbookPresenter(get(), get(), get(), get(), get(), get(), get()) }

    // Take offer
    single { TakeOfferPresenter(get(), get(), get()) }
    factory { TakeOfferAmountPresenter(get(), get(), get()) }
    factory { TakeOfferPaymentMethodPresenter(get(), get()) }
    factory { TakeOfferReviewPresenter(get(), get(), get()) }

    // Create offer
    single { CreateOfferPresenter(get(), get(), get(), get()) }
    factory { CreateOfferDirectionPresenter(get(), get(), get(), get()) }
    factory { CreateOfferMarketPresenter(get(), get(), get(), get()) }
    factory { CreateOfferPricePresenter(get(), get(), get()) }
    factory { CreateOfferAmountPresenter(get(), get(), get(), get(), get()) }
    factory { CreateOfferPaymentMethodPresenter(get(), get()) }
    factory { CreateOfferReviewPresenter(get(), get()) }

    // Trade Seller
    factory { SellerState1Presenter(get(), get(), get()) }
    factory { SellerState2aPresenter(get(), get()) }
    factory { SellerState2bPresenter(get(), get()) }
    single { SellerState3aPresenter(get(), get()) }
    factory { SellerStateMainChain3bPresenter(get(), get(), get()) }
    factory { SellerStateLightning3bPresenter(get(), get()) }
    single { SellerState4Presenter(get(), get(), get()) }

    // Trade Buyer
    single { BuyerState1aPresenter(get(), get()) }
    // BuyerState1bPresenter does not exist as it a static UI
    factory { BuyerState2aPresenter(get(), get()) }
    factory { BuyerState2bPresenter(get(), get()) }
    factory { BuyerState3aPresenter(get(), get()) }
    factory { BuyerStateMainChain3bPresenter(get(), get(), get()) }
    factory { BuyerStateLightning3bPresenter(get(), get()) }
    single { BuyerState4Presenter(get(), get(), get()) }

    // Trade General process
    factory { TradeStatesProvider(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { OpenTradeListPresenter(get(), get(), get(), get()) }
    factory { TradeDetailsHeaderPresenter(get(), get(), get(), get()) }
    factory { InterruptedTradePresenter(get(), get(), get(), get()) }
    factory { TradeFlowPresenter(get(), get(), get()) }
    factory { OpenTradePresenter(get(), get(), get(), get(), get()) }

    factory { TradeChatPresenter(get(), get(), get(), get(), get(), get()) }

    single { TradeGuideOverviewPresenter(get()) } bind TradeGuideOverviewPresenter::class
    single { TradeGuideSecurityPresenter(get()) } bind TradeGuideSecurityPresenter::class
    single { TradeGuideProcessPresenter(get()) } bind TradeGuideProcessPresenter::class
    single { TradeGuideTradeRulesPresenter(get(), get()) } bind TradeGuideTradeRulesPresenter::class
    single { WalletGuideIntroPresenter(get()) } bind WalletGuideIntroPresenter::class
    single { WalletGuideDownloadPresenter(get()) } bind WalletGuideDownloadPresenter::class
    single { WalletGuideNewPresenter(get()) } bind WalletGuideNewPresenter::class
    single { WalletGuideReceivingPresenter(get()) } bind WalletGuideReceivingPresenter::class

    factory<TimeProvider> { getPlatformCurrentTimeProvider() }

}
