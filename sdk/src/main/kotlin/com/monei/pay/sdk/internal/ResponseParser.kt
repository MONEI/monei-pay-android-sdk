package com.monei.pay.sdk.internal

import com.monei.pay.sdk.MoneiPayException
import com.monei.pay.sdk.PaymentResult
import org.json.JSONArray
import org.json.JSONObject
import java.util.Base64

/**
 * Parses responses from CloudCommerce and MONEI Pay.
 * No Android framework dependencies — fully testable on JVM.
 */
internal object ResponseParser {

    /**
     * Parse a MONEI Pay intent result (VIA_MONEI_PAY mode).
     * @param extras Map of intent extras with keys: transaction_id, success, amount,
     *               card_brand, masked_card_number, error_code, error_message
     */
    fun parseMoneiPayResult(extras: Map<String, Any?>): PaymentResult {
        val errorCode = extras["error_code"] as? String
        if (!errorCode.isNullOrEmpty()) {
            val errorMessage = extras["error_message"] as? String
            throwForErrorCode(errorCode, errorMessage)
        }

        val transactionId = extras["transaction_id"] as? String
        if (transactionId.isNullOrEmpty()) {
            throw MoneiPayException.PaymentFailed(reason = "Missing transaction_id in response")
        }

        return PaymentResult(
            transactionId = transactionId,
            success = extras["success"] as? Boolean ?: false,
            amount = extras["amount"] as? Int,
            cardBrand = (extras["card_brand"] as? String)?.ifEmpty { null },
            maskedCardNumber = (extras["masked_card_number"] as? String)?.ifEmpty { null }
        )
    }

    /**
     * Parse a CloudCommerce Base64-encoded response (DIRECT mode).
     */
    fun parseCloudCommerceResponse(base64Response: String, requestedAmount: Int): PaymentResult {
        val decoded = try {
            String(Base64.getDecoder().decode(base64Response), Charsets.UTF_8)
        } catch (e: Exception) {
            throw MoneiPayException.PaymentFailed(reason = "Failed to decode response: ${e.message}")
        }
        return parseCloudCommerceJson(decoded, requestedAmount)
    }

    /**
     * Parse decoded CloudCommerce JSON string.
     */
    fun parseCloudCommerceJson(json: String, requestedAmount: Int): PaymentResult {
        val parsed = try {
            JSONObject(json)
        } catch (_: Exception) {
            // Response might be a JSON array (error case)
            try {
                val arr = JSONArray(json)
                if (arr.length() > 0) {
                    val errorObj = arr.getJSONObject(0)
                    val reasonCode = errorObj.optString("ReasonCode", "UNKNOWN")
                    val description = errorObj.optString("Description", "Payment failed")
                    throw MoneiPayException.PaymentFailed(reason = "$reasonCode: $description")
                }
                throw MoneiPayException.PaymentFailed(reason = "Empty error response")
            } catch (e: MoneiPayException) {
                throw e
            } catch (e: Exception) {
                throw MoneiPayException.PaymentFailed(reason = "Failed to parse response: ${e.message}")
            }
        }

        // Check for error object
        val error = parsed.optJSONObject("error")
        val success = parsed.optBoolean("success", false)
        val transactionId = parsed.optString("transactionId", "")

        if (!success && transactionId.isEmpty() && error != null) {
            val reasonCode = error.optString("ReasonCode", "UNKNOWN")
            val description = error.optString("Description", "Payment failed")
            throw MoneiPayException.PaymentFailed(reason = "$reasonCode: $description")
        }

        if (transactionId.isEmpty()) {
            throw MoneiPayException.PaymentFailed(reason = "No transaction ID in response")
        }

        return PaymentResult(
            transactionId = transactionId,
            success = success,
            amount = requestedAmount,
            cardBrand = parsed.optString("cardBrandName", "").ifEmpty { null },
            maskedCardNumber = parsed.optString("maskedCardNumber", "").ifEmpty { null }
        )
    }

    private fun throwForErrorCode(code: String, message: String?): Nothing {
        when (code) {
            "USER_DENIED", "CANCELLED", "USER_CANCELLED" ->
                throw MoneiPayException.PaymentCancelled()
            "NOT_AUTHENTICATED" ->
                throw MoneiPayException.InvalidToken("Not authenticated")
            "TOKEN_EXPIRED" ->
                throw MoneiPayException.InvalidToken("Token expired")
            "INVALID_TOKEN" ->
                throw MoneiPayException.InvalidToken(message ?: "Invalid token")
            else ->
                throw MoneiPayException.PaymentFailed(reason = message ?: code)
        }
    }
}
