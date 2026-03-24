package com.monei.pay.sdk

/**
 * Result of a payment processed via MONEI Pay SDK.
 */
data class PaymentResult(
    /** Unique transaction identifier. */
    val transactionId: String,
    /** Whether the payment was approved. */
    val success: Boolean,
    /** Payment amount in cents. */
    val amount: Int?,
    /** Card brand (e.g. "visa", "mastercard"). */
    val cardBrand: String?,
    /** Masked card number (e.g. "****1234"). */
    val maskedCardNumber: String?
)
