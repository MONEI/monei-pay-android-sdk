package com.monei.pay.sdk.internal

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.monei.pay.sdk.MoneiPay
import com.monei.pay.sdk.MoneiPayException
import com.monei.pay.sdk.PaymentMode
import com.monei.pay.sdk.PaymentResult

/**
 * Transparent activity that launches payment intents and returns results.
 * Not meant to be used directly — [MoneiPay.acceptPayment] manages this.
 */
internal class PaymentActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TOKEN = "token"
        const val EXTRA_AMOUNT = "amount"
        const val EXTRA_DESCRIPTION = "description"
        const val EXTRA_CUSTOMER_NAME = "customer_name"
        const val EXTRA_CUSTOMER_EMAIL = "customer_email"
        const val EXTRA_CUSTOMER_PHONE = "customer_phone"
        const val EXTRA_MODE = "mode"
        internal const val EXTRA_CALLBACK_URL = "callback_url"
        internal const val EXTRA_ORDER_ID = "order_id"
        internal const val EXTRA_TRANSACTION_TYPE = "transaction_type"

        private const val MONEI_PAY_ACTION = "com.monei.pay.ACCEPT_PAYMENT"
        private const val CLOUD_COMMERCE_PACKAGE = "com.mastercard.cpos"
    }

    private var resultDelivered = false

    private val paymentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> handleResult(result.resultCode, result.data) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Avoid re-launching after process death restore
        if (savedInstanceState != null) {
            completeWithError(MoneiPayException.PaymentCancelled())
            return
        }

        val token = intent.getStringExtra(EXTRA_TOKEN)
        if (token.isNullOrEmpty()) {
            completeWithError(MoneiPayException.InvalidParameters("Missing token"))
            return
        }

        val amount = intent.getIntExtra(EXTRA_AMOUNT, -1)
        if (amount <= 0) {
            completeWithError(MoneiPayException.InvalidParameters("Invalid amount"))
            return
        }

        val mode = try {
            PaymentMode.valueOf(intent.getStringExtra(EXTRA_MODE) ?: PaymentMode.DIRECT.name)
        } catch (_: Exception) {
            PaymentMode.DIRECT
        }

        val description = intent.getStringExtra(EXTRA_DESCRIPTION)
        val customerName = intent.getStringExtra(EXTRA_CUSTOMER_NAME)
        val customerEmail = intent.getStringExtra(EXTRA_CUSTOMER_EMAIL)
        val customerPhone = intent.getStringExtra(EXTRA_CUSTOMER_PHONE)
        val callbackUrl = intent.getStringExtra(EXTRA_CALLBACK_URL)
        val merchantOrderId = intent.getStringExtra(EXTRA_ORDER_ID)
        val transactionType = intent.getStringExtra(EXTRA_TRANSACTION_TYPE)

        val paymentIntent = when (mode) {
            PaymentMode.VIA_MONEI_PAY -> buildMoneiPayIntent(
                token, amount, description, customerName, customerEmail, customerPhone,
                callbackUrl, merchantOrderId, transactionType
            )
            PaymentMode.DIRECT -> buildDirectIntent(
                token, amount, description, customerName, customerEmail, customerPhone,
                callbackUrl, merchantOrderId, transactionType
            )
        }

        if (paymentIntent == null) return // error already delivered

        paymentLauncher.launch(paymentIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!resultDelivered) {
            MoneiPay.completePendingPayment(
                Result.failure(MoneiPayException.PaymentCancelled())
            )
        }
    }

    private fun buildMoneiPayIntent(
        token: String,
        amount: Int,
        description: String?,
        customerName: String?,
        customerEmail: String?,
        customerPhone: String?,
        callbackUrl: String?,
        merchantOrderId: String?,
        transactionType: String?
    ): Intent {
        // raw JWT in auth_token — MONEI Pay handles Bearer prefix
        val extras = PayloadBuilder.buildMoneiPayExtras(
            token, amount, description, customerName, customerEmail, customerPhone,
            callbackUrl, merchantOrderId, transactionType
        )
        return Intent(MONEI_PAY_ACTION).apply {
            for ((k, v) in extras) {
                when (v) {
                    is Int -> putExtra(k, v)
                    is String -> putExtra(k, v)
                }
            }
        }
    }

    private fun buildDirectIntent(
        token: String,
        amount: Int,
        description: String?,
        customerName: String?,
        customerEmail: String?,
        customerPhone: String?,
        callbackUrl: String?,
        merchantOrderId: String?,
        transactionType: String?
    ): Intent? {
        val claims = JwtDecoder.decode(token)
        if (claims == null) {
            completeWithError(MoneiPayException.InvalidToken("Could not decode JWT"))
            return null
        }

        val accountId = claims.optString("account_id", "")
        if (accountId.isEmpty()) {
            completeWithError(MoneiPayException.InvalidToken("Missing account_id claim"))
            return null
        }

        if (JwtDecoder.isExpired(token)) {
            completeWithError(MoneiPayException.InvalidToken("Token expired"))
            return null
        }

        val companyName = claims.optString("company_name", "MONEI Pay")
        val mcc = claims.optString("mcc", "5999")
        val orderId = if (!merchantOrderId.isNullOrEmpty()) merchantOrderId else PayloadBuilder.generateOrderId()
        val locale = PayloadBuilder.getDeviceLocale()

        val uri = PayloadBuilder.buildCloudCommerceUri(
            token = token,
            amount = amount,
            accountId = accountId,
            companyName = companyName,
            mcc = mcc,
            description = description,
            customerName = customerName,
            customerEmail = customerEmail,
            customerPhone = customerPhone,
            orderId = orderId,
            locale = locale,
            deviceModel = Build.MODEL,
            osVersion = Build.VERSION.RELEASE,
            callbackUrl = callbackUrl,
            transactionType = transactionType
        )

        return Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(uri)
            setPackage(CLOUD_COMMERCE_PACKAGE)
        }
    }

    private fun handleResult(resultCode: Int, data: Intent?) {
        val mode = try {
            PaymentMode.valueOf(intent.getStringExtra(EXTRA_MODE) ?: PaymentMode.DIRECT.name)
        } catch (_: Exception) {
            PaymentMode.DIRECT
        }

        try {
            val paymentResult = when (mode) {
                PaymentMode.VIA_MONEI_PAY -> parseMoneiPayResult(resultCode, data)
                PaymentMode.DIRECT -> parseDirectResult(data)
            }
            completeWithSuccess(paymentResult)
        } catch (e: MoneiPayException) {
            completeWithError(e)
        } catch (e: Exception) {
            completeWithError(MoneiPayException.PaymentFailed(reason = e.message))
        }
    }

    private fun parseMoneiPayResult(resultCode: Int, data: Intent?): PaymentResult {
        if (data == null) {
            throw MoneiPayException.PaymentCancelled()
        }

        val extras = mapOf<String, Any?>(
            "transaction_id" to data.getStringExtra("transaction_id"),
            "success" to data.getBooleanExtra("success", false),
            "amount" to data.takeIf { it.hasExtra("amount") }?.getIntExtra("amount", 0),
            "card_brand" to data.getStringExtra("card_brand"),
            "masked_card_number" to data.getStringExtra("masked_card_number"),
            "order_id" to data.getStringExtra("order_id"),
            "currency" to data.getStringExtra("currency"),
            "status" to data.getStringExtra("status"),
            "status_code" to data.getStringExtra("status_code"),
            "status_message" to data.getStringExtra("status_message"),
            "authorization_code" to data.getStringExtra("authorization_code"),
            "last4" to data.getStringExtra("last4"),
            "card_type" to data.getStringExtra("card_type"),
            "card_country" to data.getStringExtra("card_country"),
            "error_code" to data.getStringExtra("error_code"),
            "error_message" to data.getStringExtra("error_message")
        )

        // Check for error extras even on RESULT_CANCELED
        val errorCode = data.getStringExtra("error_code")
        if (!errorCode.isNullOrEmpty()) {
            return ResponseParser.parseMoneiPayResult(extras)
        }

        if (resultCode != RESULT_OK) {
            throw MoneiPayException.PaymentCancelled()
        }

        return ResponseParser.parseMoneiPayResult(extras)
    }

    private fun parseDirectResult(data: Intent?): PaymentResult {
        val response = data?.getStringExtra("response")
        if (response.isNullOrEmpty()) {
            throw MoneiPayException.PaymentFailed(reason = "No response from CloudCommerce")
        }
        val amount = intent.getIntExtra(EXTRA_AMOUNT, 0)
        return ResponseParser.parseCloudCommerceResponse(response, amount)
    }

    private fun completeWithSuccess(result: PaymentResult) {
        resultDelivered = true
        MoneiPay.completePendingPayment(Result.success(result))
        finish()
    }

    private fun completeWithError(error: MoneiPayException) {
        resultDelivered = true
        MoneiPay.completePendingPayment(Result.failure(error))
        finish()
    }
}
