package network.bisq.mobile.presentation.common.di

import network.bisq.mobile.presentation.tabs.dashboard.DashboardPresenter
import network.bisq.mobile.presentation.common.ui.platform.getPlatformCurrentTimeProvider
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.utils.TimeProvider
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManagerImpl
import network.bisq.mobile.presentation.tabs.tab.ITabContainerPresenter
import network.bisq.mobile.presentation.tabs.tab.TabContainerPresenter
import network.bisq.mobile.presentation.common.ui.network_banner.NetworkStatusBannerPresenter
import network.bisq.mobile.presentation.offer.create_offer.amount.CreateOfferAmountPresenter
import network.bisq.mobile.presentation.offer.create_offer.direction.CreateOfferDirectionPresenter
import network.bisq.mobile.presentation.offer.create_offer.market.CreateOfferMarketPresenter
import network.bisq.mobile.presentation.offer.create_offer.payment_method.CreateOfferPaymentMethodPresenter
import network.bisq.mobile.presentation.offer.create_offer.CreateOfferPresenter
import network.bisq.mobile.presentation.offer.create_offer.price.CreateOfferPricePresenter
import network.bisq.mobile.presentation.offer.create_offer.review.CreateOfferReviewPresenter
import network.bisq.mobile.presentation.guide.trade_guide.TradeGuideOverviewPresenter
import network.bisq.mobile.presentation.guide.trade_guide.TradeGuideProcessPresenter
import network.bisq.mobile.presentation.guide.trade_guide.TradeGuideSecurityPresenter
import network.bisq.mobile.presentation.guide.trade_guide.TradeGuideTradeRulesPresenter
import network.bisq.mobile.presentation.guide.wallet_guide.WalletGuideDownloadPresenter
import network.bisq.mobile.presentation.guide.wallet_guide.WalletGuideIntroPresenter
import network.bisq.mobile.presentation.guide.wallet_guide.WalletGuideNewPresenter
import network.bisq.mobile.presentation.guide.wallet_guide.WalletGuideReceivingPresenter
import network.bisq.mobile.presentation.tabs.offers.OfferbookMarketPresenter
import network.bisq.mobile.presentation.tabs.open_trades.OpenTradeListPresenter
import network.bisq.mobile.presentation.trade.trade_detail.InterruptedTradePresenter
import network.bisq.mobile.presentation.trade.trade_detail.OpenTradePresenter
import network.bisq.mobile.presentation.trade.trade_detail.TradeDetailsHeaderPresenter
import network.bisq.mobile.presentation.trade.trade_detail.TradeFlowPresenter
import network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_1.state_a.BuyerState1aPresenter
import network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_2.state_a.BuyerState2aPresenter
import network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_2.state_b.BuyerState2bPresenter
import network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_3.state_a.BuyerState3aPresenter
import network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_4.BuyerState4Presenter
import network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_3.state_b.BuyerStateLightning3bPresenter
import network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_3.state_b.BuyerStateMainChain3bPresenter
import network.bisq.mobile.presentation.trade.trade_detail.states.seller_state_1.SellerState1Presenter
import network.bisq.mobile.presentation.trade.trade_detail.states.seller_state_2.state_a.SellerState2aPresenter
import network.bisq.mobile.presentation.trade.trade_detail.states.seller_state_2.state_b.SellerState2bPresenter
import network.bisq.mobile.presentation.trade.trade_detail.states.seller_state_3.state_a.SellerState3aPresenter
import network.bisq.mobile.presentation.trade.trade_detail.states.seller_state_4.SellerState4Presenter
import network.bisq.mobile.presentation.trade.trade_detail.states.seller_state_3.state_b.SellerStateLightning3bPresenter
import network.bisq.mobile.presentation.trade.trade_detail.states.seller_state_3.state_b.SellerStateMainChain3bPresenter
import network.bisq.mobile.presentation.trade.trade_detail.states.common.TradeStatesProvider
import network.bisq.mobile.presentation.trade.trade_chat.TradeChatPresenter
import network.bisq.mobile.presentation.report_user.ReportUserPresenter
import network.bisq.mobile.presentation.settings.settings.IGeneralSettingsPresenter
import network.bisq.mobile.presentation.settings.ignored_users.IIgnoredUsersPresenter
import network.bisq.mobile.presentation.settings.payment_accounts.IPaymentAccountSettingsPresenter
import network.bisq.mobile.presentation.settings.user_profile.IUserProfilePresenter
import network.bisq.mobile.presentation.settings.ignored_users.IgnoredUsersPresenter
import network.bisq.mobile.presentation.settings.payment_accounts.PaymentAccountsPresenter
import network.bisq.mobile.presentation.settings.reputation.ReputationPresenter
import network.bisq.mobile.presentation.settings.resources.ResourcesPresenter
import network.bisq.mobile.presentation.settings.settings.SettingsPresenter
import network.bisq.mobile.presentation.settings.support.SupportPresenter
import network.bisq.mobile.presentation.settings.user_profile.UserProfilePresenter
import network.bisq.mobile.presentation.startup.create_profile.CreateProfilePresenter
import network.bisq.mobile.presentation.startup.user_agreement.IAgreementPresenter
import network.bisq.mobile.presentation.startup.user_agreement.UserAgreementPresenter
import network.bisq.mobile.presentation.offer.take_offer.amount.TakeOfferAmountPresenter
import network.bisq.mobile.presentation.offer.take_offer.payment_method.TakeOfferPaymentMethodPresenter
import network.bisq.mobile.presentation.offer.take_offer.TakeOfferPresenter
import network.bisq.mobile.presentation.offer.take_offer.review.TakeOfferReviewPresenter
import org.koin.dsl.bind
import org.koin.dsl.module

val presentationModule = module {
    // Global UI state manager - uses its own scope for UI operations
    single<GlobalUiManager> { GlobalUiManager() }

    single<NetworkStatusBannerPresenter> { NetworkStatusBannerPresenter(get(), get()) }

    factory<UserAgreementPresenter> { UserAgreementPresenter(get(), get()) } bind IAgreementPresenter::class

    single { TabContainerPresenter(get(), get(), get()) } bind ITabContainerPresenter::class

    factory<ReputationPresenter> { ReputationPresenter(get(), get()) }

    single<SupportPresenter> { SupportPresenter(get(), get(), get()) }

    factory<ResourcesPresenter> { ResourcesPresenter(get(), get(), get()) }

    single<UserProfilePresenter> {
        UserProfilePresenter(
            get(),
            get(),
            get()
        )
    } bind IUserProfilePresenter::class

    single<DashboardPresenter> { DashboardPresenter(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }

    single {
        CreateProfilePresenter(
            get(),
            get()
        )
    }

    factory { SettingsPresenter(get(), get(), get()) } bind IGeneralSettingsPresenter::class

    factory { IgnoredUsersPresenter(get(), get()) } bind IIgnoredUsersPresenter::class

    single { PaymentAccountsPresenter(get(), get()) } bind IPaymentAccountSettingsPresenter::class

    // Offerbook
    single<OfferbookMarketPresenter> { OfferbookMarketPresenter(get(), get(), get(), get()) }

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

    factory { TradeChatPresenter(get(), get(), get(), get(), get(), get(), get(), get()) }

    single { TradeGuideOverviewPresenter(get()) } bind TradeGuideOverviewPresenter::class
    single { TradeGuideSecurityPresenter(get()) } bind TradeGuideSecurityPresenter::class
    single { TradeGuideProcessPresenter(get()) } bind TradeGuideProcessPresenter::class
    single { TradeGuideTradeRulesPresenter(get(), get()) } bind TradeGuideTradeRulesPresenter::class
    single { WalletGuideIntroPresenter(get()) } bind WalletGuideIntroPresenter::class
    single { WalletGuideDownloadPresenter(get()) } bind WalletGuideDownloadPresenter::class
    single { WalletGuideNewPresenter(get()) } bind WalletGuideNewPresenter::class
    single { WalletGuideReceivingPresenter(get()) } bind WalletGuideReceivingPresenter::class

    factory<TimeProvider> { getPlatformCurrentTimeProvider() }

    single<NavigationManager> { NavigationManagerImpl(get()) }

    factory { ReportUserPresenter(get(), get()) }
}
