package network.bisq.mobile.client.service.market

import io.ktor.util.collections.ConcurrentMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.websocket.subscription.WebSocketEventPayload
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.data.model.MarketPriceItem
import network.bisq.mobile.domain.data.model.offerbook.MarketListItem
import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountVO
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.domain.formatters.MarketPriceFormatter
import network.bisq.mobile.domain.service.accounts.AccountsServiceFacade
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.utils.Logging

class ClientAccountsServiceFacade(
    private val apiGateway: AccountsApiGateway,
    private val json: Json
) : AccountsServiceFacade, Logging {

    private val _accounts = MutableStateFlow<List<UserDefinedFiatAccountVO>>(emptyList())
    override val accounts: StateFlow<List<UserDefinedFiatAccountVO>> get() = _accounts

    private val _selectedAccount = MutableStateFlow<UserDefinedFiatAccountVO?>(null)
    override val selectedAccount: StateFlow<UserDefinedFiatAccountVO?> get() = _selectedAccount

    private val backgroundScope = CoroutineScope(BackgroundDispatcher)

    override suspend fun getAccounts(): List<UserDefinedFiatAccountVO> {
        val result = apiGateway.getPaymentAccounts()
        if (result.isSuccess) {
            result.getOrThrow().let {
                _accounts.value = it
            }
        }
        return _accounts.value
    }

    override suspend fun addAccount(account: UserDefinedFiatAccountVO) {
        TODO("Not yet implemented")
    }

    override suspend fun saveAccount(account: UserDefinedFiatAccountVO) {
        TODO("Not yet implemented")
    }

    override suspend fun removeAccount(account: UserDefinedFiatAccountVO) {
        TODO("Not yet implemented")
    }

    override suspend fun setSelectedAccount(account: UserDefinedFiatAccountVO) {
        TODO("Not yet implemented")
    }

}