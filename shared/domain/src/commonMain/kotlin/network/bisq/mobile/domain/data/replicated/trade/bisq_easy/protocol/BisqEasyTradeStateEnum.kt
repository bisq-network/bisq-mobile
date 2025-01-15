package network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol

import kotlinx.serialization.Serializable

@Serializable
enum class BisqEasyTradeStateEnum(val isFinalState: Boolean = false) {
    INIT,

    // Take offer
    TAKER_SENT_TAKE_OFFER_REQUEST,


    // BUYER AS TAKER *****************************/
    // Branch 1: Buyer receives take offer response first
    TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,

    // Branch 1.1.: Buyer sends Btc address first
    TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,

    // Branch 1.2.: Buyer receives account data first
    TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,

    // Branch 2: Buyer sends Btc address first, then receives take offer response
    TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,
    TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA_,

    // Unique final converging step (all three states have been completed)
    TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
    // *********************************************/


    // SELLER AS MAKER *****************************/
    // Branch 1: Seller receives take offer request first
    MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,

    // Branch 1.1.: Seller receives Btc address first
    MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,

    // Branch 1.2.: Seller sends account data first
    MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,

    // Branch 2: Seller receives Btc address first, then take offer request
    MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,
    MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS_,

    // Unique final converging step (the three states have been completed)
    MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,
    // *********************************************/


    // SELLER AS TAKER *****************************/
    // Branch 1: Seller receives take offer response first
    TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,

    // Branch 1.1.: Seller sends account data first
    TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,

    // Branch 1.2.: Seller receives Btc address first
    TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,

    // Branch 2: Seller sends account data first, then receives take offer response
    TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
    TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS_,

    // Unique final converging step (all three states have been completed)
    TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,
    // *********************************************/


    // BUYER AS MAKER *****************************/
    // Branch 1: Buyer receives take offer request first
    MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,

    // Branch 1.1.: Buyer sends Btc address first
    MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,

    // Branch 1.2. Buyer receives account data first
    MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,

    // Branch 2: Buyer receives account data first
    MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
    MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA_,

    // Unique final converging step (all three states have been completed)
    MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
    // *********************************************/


    // Fiat payment
    BUYER_SENT_FIAT_SENT_CONFIRMATION,
    SELLER_RECEIVED_FIAT_SENT_CONFIRMATION,
    SELLER_CONFIRMED_FIAT_RECEIPT,
    BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION,

    // BTC transfer
    SELLER_SENT_BTC_SENT_CONFIRMATION,
    BUYER_RECEIVED_BTC_SENT_CONFIRMATION,
    BTC_CONFIRMED(true),

    REJECTED(true),
    PEER_REJECTED(true),

    CANCELLED(true),
    PEER_CANCELLED(true),

    FAILED(true),
    FAILED_AT_PEER(true);
}