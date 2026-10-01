package com.monei.pay.sdk

/**
 * Errors thrown by the MONEI Pay SDK.
 */
sealed class MoneiPayException(message: String) : Exception(message) {

    /** MONEI Pay is not installed on this device. */
    class MoneiPayNotInstalled : MoneiPayException(
        "MONEI Pay is not installed. Install it from Google Play to accept NFC payments."
    )

    /** CloudCommerce app is not installed (required for DIRECT mode). */
    class CloudCommerceNotInstalled : MoneiPayException(
        "CloudCommerce app is not installed. Required for direct NFC payments."
    )

    /** A payment is already in progress. */
    class PaymentInProgress : MoneiPayException(
        "A payment is already in progress. Wait for it to complete."
    )

    /** The payment was cancelled by the user. */
    class PaymentCancelled : MoneiPayException("Payment was cancelled.")

    /**
     * The payment was declined or failed.
     * [payment] holds the declined payment data (id, status, reason, card) when MONEI Pay
     * sends it, so you can show why the payment was declined.
     */
    class PaymentFailed @JvmOverloads constructor(
        val reason: String? = null,
        val payment: PaymentResult? = null
    ) : MoneiPayException(
        if (reason != null) "Payment failed: $reason" else "Payment failed."
    )

    /** Invalid input parameters. */
    class InvalidParameters(detail: String) : MoneiPayException(
        "Invalid parameters: $detail"
    )

    /** Auth token is invalid or expired. */
    class InvalidToken(detail: String) : MoneiPayException(
        "Invalid token: $detail"
    )
}
