package com.monei.pay.sdk.internal

import org.json.JSONObject
import java.util.Base64
import java.util.UUID

/**
 * Builds CloudCommerce payment payloads for DIRECT mode.
 * No Android framework dependencies — fully testable on JVM.
 */
internal object PayloadBuilder {

    /**
     * Build a CloudCommerce deep link URI string.
     */
    fun buildCloudCommerceUri(
        token: String,
        amount: Int,
        accountId: String,
        companyName: String,
        mcc: String,
        description: String?,
        customerName: String?,
        customerEmail: String?,
        customerPhone: String?,
        orderId: String,
        locale: String,
        deviceModel: String,
        osVersion: String
    ): String {
        val payload = buildPayloadJson(
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
            deviceModel = deviceModel,
            osVersion = osVersion
        )
        val base64 = Base64.getEncoder().encodeToString(
            payload.toString().toByteArray(Charsets.UTF_8)
        )
        return "cloud_payment://cloudcommerce/json:$base64"
    }

    /**
     * Build the payload JSON object.
     * All parameters are explicit — no Android framework references.
     */
    fun buildPayloadJson(
        token: String,
        amount: Int,
        accountId: String,
        companyName: String,
        mcc: String,
        description: String?,
        customerName: String?,
        customerEmail: String?,
        customerPhone: String?,
        orderId: String,
        locale: String,
        deviceModel: String,
        osVersion: String
    ): JSONObject {
        val bearerToken = if (token.startsWith("Bearer ")) token else "Bearer $token"

        val customData = JSONObject().apply {
            put("accountId", accountId)
            put("orderId", orderId)
            put("lang", locale.split("_").firstOrNull()?.lowercase() ?: "en")
            put("deviceType", "mobile")
            put("deviceModel", deviceModel)
            put("os", "Android")
            put("osVersion", osVersion)
            put("source", "monei-pay-sdk")
            put("sourceVersion", "0.1.0")
            if (!description.isNullOrEmpty()) put("description", description)
            if (!customerName.isNullOrEmpty()) put("customerName", customerName)
            if (!customerEmail.isNullOrEmpty()) put("customerEmail", customerEmail)
            if (!customerPhone.isNullOrEmpty()) put("customerPhone", customerPhone)
        }

        return JSONObject().apply {
            put("authToken", bearerToken)
            put("amountAuthorizedNumeric", amount.toString().padStart(12, '0'))
            put("orderId", orderId)
            put("merchantCustomData", customData)
            put("transactionCurrencyCode", "0978")
            put("transactionType", "00")
            put("merchantCountryCode", "724")
            put("merchantCurrencyCode", "978")
            put("merchantCategoryCode", mcc)
            put("merchantDisplayName", companyName)
            put("isFlowAuto", "true")
            put("colorPrimary", "#171717")
            put("fullAccess", "false")
            put("locale", locale)
        }
    }

    fun generateOrderId(): String {
        return UUID.randomUUID().toString().replace("-", "").take(12).uppercase()
    }

    fun getDeviceLocale(): String {
        val locale = java.util.Locale.getDefault()
        val lang = locale.language
        val country = locale.country
        return if (country.isNotEmpty()) "${lang}_${country}" else "en_US"
    }
}
