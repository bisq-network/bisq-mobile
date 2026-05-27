package network.bisq.mobile.client.payment_accounts.data.model

/**
 * Base interface for all fiat payment account payload VOs.
 * Each fiat payment rail type (CUSTOM, SEPA, REVOLUT, etc.) will have its own implementation.
 * Mirrors the Java FiatAccountPayloadDto sealed interface.
 *
 * Note: Payload type is automatically determined by the parent FiatAccountDto's concrete type.
 * No custom serializer needed - the parent's serializer handles the complete deserialization.
 */

interface PaymentAccountPayloadDto
