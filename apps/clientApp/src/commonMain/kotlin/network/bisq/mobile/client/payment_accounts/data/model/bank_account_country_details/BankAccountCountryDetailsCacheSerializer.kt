package network.bisq.mobile.client.payment_accounts.data.model.bank_account_country_details

import androidx.datastore.core.okio.OkioSerializer
import network.bisq.mobile.data.datastore.serializer.jsonDataStoreSerializer

object BankAccountCountryDetailsCacheSerializer : OkioSerializer<BankAccountCountryDetailsCache> by jsonDataStoreSerializer(
    defaultValue = BankAccountCountryDetailsCache(),
    typeName = "BankAccountCountryDetailsCache",
)
