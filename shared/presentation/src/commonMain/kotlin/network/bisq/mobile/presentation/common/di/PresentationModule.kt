package network.bisq.mobile.presentation.common.di

import network.bisq.mobile.presentation.common.ui.alert.AlertNotificationBannerPresenter
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.WebLinkConfirmationDialogPresenter
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManagerImpl
import network.bisq.mobile.presentation.common.ui.network_banner.NetworkStatusBannerPresenter
import network.bisq.mobile.presentation.common.ui.platform.getPlatformCurrentTimeProvider
import network.bisq.mobile.presentation.common.ui.utils.TimeProvider
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.SelectPaymentMethodPresenter
import network.bisq.mobile.presentation.guide.trade_guide.TradeGuideOverviewPresenter
import network.bisq.mobile.presentation.guide.trade_guide.TradeGuideProcessPresenter
import network.bisq.mobile.presentation.guide.trade_guide.TradeGuideSecurityPresenter
import network.bisq.mobile.presentation.guide.trade_guide.TradeGuideTradeRulesPresenter
import network.bisq.mobile.presentation.guide.wallet_guide.WalletGuideDownloadPresenter
import network.bisq.mobile.presentation.guide.wallet_guide.WalletGuideIntroPresenter
import network.bisq.mobile.presentation.guide.wallet_guide.WalletGuideNewPresenter
import network.bisq.mobile.presentation.guide.wallet_guide.WalletGuideReceivingPresenter
import network.bisq.mobile.presentation.offer.create_offer.CreateOfferCoordinator
import network.bisq.mobile.presentation.offer.create_offer.amount.CreateOfferAmountPresenter
import network.bisq.mobile.presentation.offer.create_offer.direction.CreateOfferDirectionPresenter
import network.bisq.mobile.presentation.offer.create_offer.market.CreateOfferMarketPresenter
import network.bisq.mobile.presentation.offer.create_offer.payment_method.CreateOfferPaymentMethodPresenter
import network.bisq.mobile.presentation.offer.create_offer.price.CreateOfferPricePresenter
import network.bisq.mobile.presentation.offer.create_offer.review.CreateOfferReviewPresenter
import network.bisq.mobile.presentation.offer.take_offer.TakeOfferCoordinator
import network.bisq.mobile.presentation.offer.take_offer.amount.TakeOfferAmountPresenter
import network.bisq.mobile.presentation.offer.take_offer.payment_method.TakeOfferPaymentMethodPresenter
import network.bisq.mobile.presentation.offer.take_offer.review.TakeOfferReviewPresenter
import network.bisq.mobile.presentation.report_user.ReportUserPresenter
import network.bisq.mobile.presentation.settings.ignored_users.IIgnoredUsersPresenter
import network.bisq.mobile.presentation.settings.ignored_users.IgnoredUsersPresenter
import network.bisq.mobile.presentation.settings.payment_accounts.PaymentAccountsPresenter
import network.bisq.mobile.presentation.settings.payment_accounts_musig.PaymentAccountsMusigPresenter
import network.bisq.mobile.presentation.settings.reputation.ReputationPresenter
import network.bisq.mobile.presentation.settings.resources.ResourcesPresenter
import network.bisq.mobile.presentation.settings.settings.SettingsPresenter
import network.bisq.mobile.presentation.settings.support.SupportPresenter
import network.bisq.mobile.presentation.settings.user_profile.IUserProfilePresenter
import network.bisq.mobile.presentation.settings.user_profile.UserProfilePresenter
import network.bisq.mobile.presentation.startup.create_profile.CreateProfilePresenter
import network.bisq.mobile.presentation.startup.user_agreement.IAgreementPresenter
import network.bisq.mobile.presentation.startup.user_agreement.UserAgreementPresenter
import network.bisq.mobile.presentation.tabs.dashboard.DashboardPresenter
import network.bisq.mobile.presentation.tabs.offers.OfferbookMarketPresenter
import network.bisq.mobile.presentation.tabs.offers.usecase.ComputeOfferbookMarketListUseCase
import network.bisq.mobile.presentation.tabs.open_trades.OpenTradeListPresenter
import network.bisq.mobile.presentation.tabs.tab.ITabContainerPresenter
import network.bisq.mobile.presentation.tabs.tab.TabContainerPresenter
import network.bisq.mobile.presentation.trade.trade_chat.TradeChatPresenter
import network.bisq.mobile.presentation.trade.trade_detail.InterruptedTradePresenter
import network.bisq.mobile.presentation.trade.trade_detail.OpenTradePresenter
import network.bisq.mobile.presentation.trade.trade_detail.TradeDetailsHeaderPresenter
import network.bisq.mobile.presentation.trade.trade_detail.TradeFlowPresenter
import network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_1.state_a.BuyerState1aPresenter
import network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_2.state_a.BuyerState2aPresenter
import network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_2.state_b.BuyerState2bPresenter
import network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_3.state_a.BuyerState3aPresenter
import network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_3.state_b.BuyerStateLightning3bPresenter
import network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_3.state_b.BuyerStateMainChain3bPresenter
import network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_4.BuyerState4Presenter
import network.bisq.mobile.presentation.trade.trade_detail.states.common.TradeStatesProvider
import network.bisq.mobile.presentation.trade.trade_detail.states.seller_state_1.SellerState1Presenter
import network.bisq.mobile.presentation.trade.trade_detail.states.seller_state_2.state_a.SellerState2aPresenter
import network.bisq.mobile.presentation.trade.trade_detail.states.seller_state_2.state_b.SellerState2bPresenter
import network.bisq.mobile.presentation.trade.trade_detail.states.seller_state_3.state_a.SellerState3aPresenter
import network.bisq.mobile.presentation.trade.trade_detail.states.seller_state_3.state_b.SellerStateLightning3bPresenter
import network.bisq.mobile.presentation.trade.trade_detail.states.seller_state_3.state_b.SellerStateMainChain3bPresenter
import network.bisq.mobile.presentation.trade.trade_detail.states.seller_state_4.SellerState4Presenter
import org.koin.dsl.bind
import org.koin.dsl.module

val presentationModule =
    module {
        // Global UI state manager - uses its own scope for UI operations
        single<GlobalUiManager> { GlobalUiManager() }

        factory<NetworkStatusBannerPresenter> { NetworkStatusBannerPresenter(get(), get()) }
        factory<AlertNotificationBannerPresenter> { AlertNotificationBannerPresenter(get(), get()) }

        factory<UserAgreementPresenter> {
            UserAgreementPresenter(
                get(),
                get(),
            )
        } bind IAgreementPresenter::class

        factory { TabContainerPresenter(get(), get(), get(), get()) } bind ITabContainerPresenter::class

        factory<ReputationPresenter> { ReputationPresenter(get(), get()) }

        factory<SupportPresenter> { SupportPresenter(get(), get(), get()) }

        factory<ResourcesPresenter> { ResourcesPresenter(get(), get(), get()) }

        factory<UserProfilePresenter> {
            UserProfilePresenter(
                get(),
                get(),
                get(),
            )
        } bind IUserProfilePresenter::class

        factory<DashboardPresenter> {
            DashboardPresenter(
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
                get(), // pushNotificationServiceFacade
            )
        }

        factory {
            CreateProfilePresenter(
                get(),
                get(),
            )
        }

        factory { SettingsPresenter(get(), get(), get()) }

        factory { IgnoredUsersPresenter(get(), get()) } bind IIgnoredUsersPresenter::class

        factory { PaymentAccountsPresenter(get(), get()) }

        factory { PaymentAccountsMusigPresenter(get(), get()) }
        factory { SelectPaymentMethodPresenter(get(), get()) }

        factory { ComputeOfferbookMarketListUseCase(get()) }

        // Offerbook
        single<OfferbookMarketPresenter> { OfferbookMarketPresenter(get(), get(), get(), get(), get(), get()) }

        // Take offer
        single { TakeOfferCoordinator(get(), get()) }
        factory { TakeOfferAmountPresenter(get(), get(), get()) }
        factory { TakeOfferPaymentMethodPresenter(get(), get()) }
        factory { TakeOfferReviewPresenter(get(), get(), get()) }

        // Create offer
        single { CreateOfferCoordinator(get(), get(), get()) }
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
        factory { SellerState3aPresenter(get(), get()) }
        factory { SellerStateMainChain3bPresenter(get(), get(), get()) }
        factory { SellerStateLightning3bPresenter(get(), get()) }
        factory { SellerState4Presenter(get(), get(), get()) }

        // Trade Buyer
        factory { BuyerState1aPresenter(get(), get()) }
        // BuyerState1bPresenter does not exist as it a static UI
        factory { BuyerState2aPresenter(get(), get()) }
        factory { BuyerState2bPresenter(get(), get()) }
        factory { BuyerState3aPresenter(get(), get()) }
        factory { BuyerStateMainChain3bPresenter(get(), get(), get()) }
        factory { BuyerStateLightning3bPresenter(get(), get()) }
        factory { BuyerState4Presenter(get(), get(), get()) }

        // Trade General process
        factory {
            TradeStatesProvider(
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
            )
        }
        factory { OpenTradeListPresenter(get(), get(), get(), get()) }
        factory { TradeDetailsHeaderPresenter(get(), get(), get(), get()) }
        factory { InterruptedTradePresenter(get(), get(), get(), get()) }
        factory { TradeFlowPresenter(get(), get(), get()) }
        factory { OpenTradePresenter(get(), get(), get(), get(), get()) }

        factory { TradeChatPresenter(get(), get(), get(), get(), get(), get(), get(), get()) }

        factory { TradeGuideOverviewPresenter(get()) } bind TradeGuideOverviewPresenter::class
        factory { TradeGuideSecurityPresenter(get()) } bind TradeGuideSecurityPresenter::class
        factory { TradeGuideProcessPresenter(get()) } bind TradeGuideProcessPresenter::class
        factory { TradeGuideTradeRulesPresenter(get(), get()) } bind TradeGuideTradeRulesPresenter::class
        factory { WalletGuideIntroPresenter(get()) } bind WalletGuideIntroPresenter::class
        factory { WalletGuideDownloadPresenter(get()) } bind WalletGuideDownloadPresenter::class
        factory { WalletGuideNewPresenter(get()) } bind WalletGuideNewPresenter::class
        factory { WalletGuideReceivingPresenter(get()) } bind WalletGuideReceivingPresenter::class

        factory<TimeProvider> { getPlatformCurrentTimeProvider() }

        single<NavigationManager> { NavigationManagerImpl(get()) }

        factory { ReportUserPresenter(get(), get()) }

        factory { WebLinkConfirmationDialogPresenter(get(), get()) }
    }
