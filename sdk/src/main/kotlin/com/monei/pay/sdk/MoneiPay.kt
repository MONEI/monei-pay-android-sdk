package com.monei.pay.sdk

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.monei.pay.sdk.internal.PaymentActivity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex

/**
 * MONEI Pay SDK for accepting NFC payments on Android.
 *
 * Usage:
 * ```kotlin
 * val result = MoneiPay.acceptPayment(
 *     context = this,
 *     token = "eyJ...",
 *     amount = 1500,
 *     mode = PaymentMode.DIRECT
 * )
 * ```
 */
object MoneiPay {

    const val SDK_VERSION = "0.1.0"

    private const val MONEI_PAY_ACTION = "com.monei.pay.ACCEPT_PAYMENT"
    private const val CLOUD_COMMERCE_PACKAGE = "com.mastercard.cpos"

    private val mutex = Mutex()

    @Volatile
    internal var pendingResult: CompletableDeferred<PaymentResult>? = null

    /**
     * Accept an NFC payment.
     *
     * @param context Android context (Activity or Application).
     * @param token Raw JWT auth token (no "Bearer " prefix — SDK adds it internally).
     * @param amount Payment amount in cents (e.g. 1500 = 15.00 EUR).
     * @param description Optional payment description.
     * @param customerName Optional customer name.
     * @param customerEmail Optional customer email.
     * @param customerPhone Optional customer phone.
     * @param mode Payment mode: [PaymentMode.DIRECT] (via CloudCommerce) or [PaymentMode.VIA_MONEI_PAY].
     * @return [PaymentResult] with transaction details.
     * @throws [MoneiPayException] on failure.
     */
    suspend fun acceptPayment(
        context: Context,
        token: String,
        amount: Int,
        description: String? = null,
        customerName: String? = null,
        customerEmail: String? = null,
        customerPhone: String? = null,
        mode: PaymentMode = PaymentMode.DIRECT
    ): PaymentResult {
        if (!mutex.tryLock()) {
            throw MoneiPayException.PaymentInProgress()
        }

        try {
            require(amount > 0) { "amount must be positive" }
            require(token.isNotEmpty()) { "token must not be empty" }

            checkAppAvailable(context, mode)

            val deferred = CompletableDeferred<PaymentResult>()
            pendingResult = deferred

            val intent = Intent(context, PaymentActivity::class.java).apply {
                putExtra(PaymentActivity.EXTRA_TOKEN, token)
                putExtra(PaymentActivity.EXTRA_AMOUNT, amount)
                putExtra(PaymentActivity.EXTRA_DESCRIPTION, description)
                putExtra(PaymentActivity.EXTRA_CUSTOMER_NAME, customerName)
                putExtra(PaymentActivity.EXTRA_CUSTOMER_EMAIL, customerEmail)
                putExtra(PaymentActivity.EXTRA_CUSTOMER_PHONE, customerPhone)
                putExtra(PaymentActivity.EXTRA_MODE, mode.name)
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)

            return deferred.await()
        } catch (e: MoneiPayException) {
            throw e
        } catch (e: IllegalArgumentException) {
            throw MoneiPayException.InvalidParameters(e.message ?: "Invalid parameters")
        } catch (e: Exception) {
            throw MoneiPayException.PaymentFailed(reason = e.message)
        } finally {
            pendingResult = null
            mutex.unlock()
        }
    }

    internal fun completePendingPayment(result: Result<PaymentResult>) {
        val deferred = pendingResult ?: return
        result.fold(
            onSuccess = { deferred.complete(it) },
            onFailure = { deferred.completeExceptionally(it) }
        )
    }

    private fun checkAppAvailable(context: Context, mode: PaymentMode) {
        when (mode) {
            PaymentMode.VIA_MONEI_PAY -> {
                val intent = Intent(MONEI_PAY_ACTION)
                if (!isActivityResolvable(context, intent)) {
                    throw MoneiPayException.MoneiPayNotInstalled()
                }
            }
            PaymentMode.DIRECT -> {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("cloud_payment://cloudcommerce/test")
                    setPackage(CLOUD_COMMERCE_PACKAGE)
                }
                if (!isActivityResolvable(context, intent)) {
                    throw MoneiPayException.CloudCommerceNotInstalled()
                }
            }
        }
    }

    private fun isActivityResolvable(context: Context, intent: Intent): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            context.packageManager.resolveActivity(
                intent,
                PackageManager.ResolveInfoFlags.of(0)
            ) != null
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.resolveActivity(intent, 0) != null
        }
    }
}
