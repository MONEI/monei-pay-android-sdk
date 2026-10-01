package com.monei.pay.sdk

/**
 * Result of a payment processed via MONEI Pay SDK.
 *
 * Display data only. Confirm the payment on your server (signed webhook or
 * `GET /payments/{id}`) before fulfillment.
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
    val maskedCardNumber: String?,
    /** Merchant order reference. */
    val orderId: String? = null,
    /** ISO 4217 currency code (e.g. "EUR"). */
    val currency: String? = null,
    /** MONEI payment status (e.g. "SUCCEEDED", "FAILED"). Use [success] for approved or declined. */
    val status: String? = null,
    /** MONEI status code (e.g. "E000"). */
    val statusCode: String? = null,
    /** MONEI status message (e.g. "Insufficient funds"). */
    val statusMessage: String? = null,
    /** Issuer authorization code. */
    val authorizationCode: String? = null,
    /** Last 4 digits of the card number. */
    val last4: String? = null,
    /** Card type (e.g. "credit", "debit", "prepaid"). */
    val cardType: String? = null,
    /** Card country, ISO 3166-1 alpha-2 (e.g. "ES"). */
    val cardCountry: String? = null
)
