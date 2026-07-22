package network.bisq.mobile.domain.service.trades

import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.BTC_CONFIRMED
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.BUYER_RECEIVED_BTC_SENT_CONFIRMATION
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.BUYER_SENT_FIAT_SENT_CONFIRMATION
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA_
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS_
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.SELLER_CONFIRMED_FIAT_RECEIPT
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.SELLER_RECEIVED_FIAT_SENT_CONFIRMATION
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.SELLER_SENT_BTC_SENT_CONFIRMATION
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA_
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS_
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.TAKER_SENT_TAKE_OFFER_REQUEST
import network.bisq.mobile.domain.analytics.AnalyticsEvent.Trade.Phase

/**
 * Maps a protocol [BisqEasyTradeStateEnum] to the coarse 8-phase funnel [Phase], mirroring the
 * grouping the trade-detail UI uses (see `TradeFlowPresenter.tradeStateChanged`). The role is baked
 * into most state names; [isSeller] only disambiguates the two role-agnostic states
 * (take-offer request and BTC confirmed). Terminal/interrupt states (init, rejected, cancelled,
 * failed) map to null.
 *
 * If the protocol adds/renames states, `TradePhaseMappingTest` fails loudly so this stays in sync.
 */
fun BisqEasyTradeStateEnum.toTradePhase(isSeller: Boolean): Phase? =
    when (this) {
        // --- Phase 1: account-data / bitcoin-address exchange ---
        TAKER_SENT_TAKE_OFFER_REQUEST -> if (isSeller) Phase.SELLER_1 else Phase.BUYER_1

        MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
        MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,
        MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS_,
        MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,
        TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
        TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,
        -> Phase.SELLER_1

        MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,
        MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
        MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
        MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA_,
        TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,
        TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
        TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,
        TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA_,
        TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,
        MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,
        -> Phase.BUYER_1

        // --- Phase 2: fiat payment ---
        MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
        MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,
        TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
        TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS_,
        TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
        TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,
        SELLER_RECEIVED_FIAT_SENT_CONFIRMATION,
        -> Phase.SELLER_2

        TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
        MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
        BUYER_SENT_FIAT_SENT_CONFIRMATION,
        -> Phase.BUYER_2

        // --- Phase 3: bitcoin settlement ---
        SELLER_CONFIRMED_FIAT_RECEIPT,
        SELLER_SENT_BTC_SENT_CONFIRMATION,
        -> Phase.SELLER_3

        BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION,
        BUYER_RECEIVED_BTC_SENT_CONFIRMATION,
        -> Phase.BUYER_3

        // --- Phase 4: complete ---
        BTC_CONFIRMED -> if (isSeller) Phase.SELLER_4 else Phase.BUYER_4

        // Init / interrupt / failed states carry no phase.
        else -> null
    }
