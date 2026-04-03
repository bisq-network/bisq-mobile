package network.bisq.mobile.presentation.common.ui.navigation

import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write
import network.bisq.mobile.presentation.common.ui.navigation.types.PaymentAccountType

val paymentAccountTypeNavType: NavType<PaymentAccountType> =
    object : NavType<PaymentAccountType>(isNullableAllowed = false) {
        @Suppress("DEPRECATION")
        override fun get(
            bundle: SavedState,
            key: String,
        ): PaymentAccountType? {
            val value = bundle.read { if (!contains(key) || isNull(key)) null else getString(key) }
            return value?.let(PaymentAccountType::valueOf)
        }

        override fun parseValue(value: String): PaymentAccountType = PaymentAccountType.valueOf(value)

        override fun put(
            bundle: SavedState,
            key: String,
            value: PaymentAccountType,
        ) {
            bundle.write { putString(key, value.name) }
        }

        override fun serializeAsValue(value: PaymentAccountType): String = value.name
    }
