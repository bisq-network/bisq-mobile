package network.bisq.mobile.presentation.common.ui.navigation

import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write
import network.bisq.mobile.presentation.common.model.account.PaymentMethodVO
import network.bisq.mobile.presentation.common.ui.navigation.types.PaymentAccountType

private fun parsePaymentAccountType(value: String): PaymentAccountType? = runCatching { PaymentAccountType.valueOf(value) }.getOrNull()

val paymentAccountTypeNavType: NavType<PaymentAccountType> =
    object : NavType<PaymentAccountType>(isNullableAllowed = false) {
        @Suppress("DEPRECATION")
        override fun get(
            bundle: SavedState,
            key: String,
        ): PaymentAccountType? {
            val value = bundle.read { if (!contains(key) || isNull(key)) null else getString(key) }
            return value?.let(::parsePaymentAccountType)
        }

        override fun parseValue(value: String): PaymentAccountType = parsePaymentAccountType(value) ?: throw IllegalArgumentException("Unknown PaymentAccountType: $value")

        override fun put(
            bundle: SavedState,
            key: String,
            value: PaymentAccountType,
        ) {
            bundle.write { putString(key, value.name) }
        }

        override fun serializeAsValue(value: PaymentAccountType): String = value.name
    }

val paymentMethodNavType: NavType<PaymentMethodVO> =
    object : NavType<PaymentMethodVO>(isNullableAllowed = false) {
        @Suppress("DEPRECATION")
        override fun get(
            bundle: SavedState,
            key: String,
        ): PaymentMethodVO? {
            val value = bundle.read { if (!contains(key) || isNull(key)) null else getString(key) }
            return value?.let(PaymentMethodVO::valueOf)
        }

        override fun parseValue(value: String): PaymentMethodVO = PaymentMethodVO.valueOf(value)

        override fun put(
            bundle: SavedState,
            key: String,
            value: PaymentMethodVO,
        ) {
            bundle.write { putString(key, value.name) }
        }

        override fun serializeAsValue(value: PaymentMethodVO): String = value.name
    }
