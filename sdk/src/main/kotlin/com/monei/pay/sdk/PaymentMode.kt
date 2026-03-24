package com.monei.pay.sdk

/**
 * Payment processing mode for Android.
 */
enum class PaymentMode {
    /** Launch CloudCommerce app directly for NFC payment. No MONEI Pay needed. */
    DIRECT,

    /** Launch MONEI Pay app, which handles the NFC payment. */
    VIA_MONEI_PAY
}
