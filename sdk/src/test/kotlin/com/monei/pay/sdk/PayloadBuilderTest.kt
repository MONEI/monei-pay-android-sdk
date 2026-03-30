package com.monei.pay.sdk

import com.monei.pay.sdk.internal.PayloadBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PayloadBuilderTest {

    @Test
    fun `buildPayloadJson includes all required fields`() {
        val payload = PayloadBuilder.buildPayloadJson(
            token = "eyJtest",
            amount = 1500,
            accountId = "acc_123",
            companyName = "Test Store",
            mcc = "5411",
            description = null,
            customerName = null,
            customerEmail = null,
            customerPhone = null,
            orderId = "ABC123DEF456",
            locale = "en_US",
            deviceModel = "Pixel 8",
            osVersion = "14"
        )

        assertEquals("Bearer eyJtest", payload.getString("authToken"))
        assertEquals("000000001500", payload.getString("amountAuthorizedNumeric"))
        assertEquals("ABC123DEF456", payload.getString("orderId"))
        assertEquals("0978", payload.getString("transactionCurrencyCode"))
        assertEquals("00", payload.getString("transactionType"))
        assertEquals("724", payload.getString("merchantCountryCode"))
        assertEquals("978", payload.getString("merchantCurrencyCode"))
        assertEquals("5411", payload.getString("merchantCategoryCode"))
        assertEquals("Test Store", payload.getString("merchantDisplayName"))
        assertEquals("true", payload.getString("isFlowAuto"))
        assertEquals("#171717", payload.getString("colorPrimary"))
        assertEquals("false", payload.getString("fullAccess"))
        assertEquals("en_US", payload.getString("locale"))
    }

    @Test
    fun `buildPayloadJson merchantCustomData has correct structure`() {
        val payload = PayloadBuilder.buildPayloadJson(
            token = "eyJtest",
            amount = 2000,
            accountId = "acc_456",
            companyName = "My Shop",
            mcc = "5999",
            description = "Order #42",
            customerName = "John Doe",
            customerEmail = "john@example.com",
            customerPhone = "+34600000000",
            orderId = "AABBCCDDEE11",
            locale = "es_ES",
            deviceModel = "Samsung S24",
            osVersion = "15"
        )

        val customData = payload.getJSONObject("merchantCustomData")
        assertEquals("acc_456", customData.getString("accountId"))
        assertEquals("AABBCCDDEE11", customData.getString("orderId"))
        assertEquals("es", customData.getString("lang"))
        assertEquals("mobile", customData.getString("deviceType"))
        assertEquals("Samsung S24", customData.getString("deviceModel"))
        assertEquals("Android", customData.getString("os"))
        assertEquals("15", customData.getString("osVersion"))
        assertEquals("monei-pay-sdk", customData.getString("source"))
        assertEquals("0.2.0", customData.getString("sourceVersion"))
        assertEquals("Order #42", customData.getString("description"))
        assertEquals("John Doe", customData.getString("customerName"))
        assertEquals("john@example.com", customData.getString("customerEmail"))
        assertEquals("+34600000000", customData.getString("customerPhone"))
    }

    @Test
    fun `buildPayloadJson omits empty optional customer fields`() {
        val payload = PayloadBuilder.buildPayloadJson(
            token = "eyJtest",
            amount = 500,
            accountId = "acc_789",
            companyName = "Store",
            mcc = "5999",
            description = null,
            customerName = "",
            customerEmail = null,
            customerPhone = null,
            orderId = "ORDER123ABCD",
            locale = "en_US",
            deviceModel = "Test",
            osVersion = "14"
        )

        val customData = payload.getJSONObject("merchantCustomData")
        assertFalse(customData.has("description"))
        assertFalse(customData.has("customerName"))
        assertFalse(customData.has("customerEmail"))
        assertFalse(customData.has("customerPhone"))
    }

    @Test
    fun `buildPayloadJson prepends Bearer to token`() {
        val payload = PayloadBuilder.buildPayloadJson(
            token = "eyJraw_token",
            amount = 100,
            accountId = "acc",
            companyName = "S",
            mcc = "5999",
            description = null,
            customerName = null,
            customerEmail = null,
            customerPhone = null,
            orderId = "O",
            locale = "en_US",
            deviceModel = "T",
            osVersion = "1"
        )
        assertEquals("Bearer eyJraw_token", payload.getString("authToken"))
    }

    @Test
    fun `buildPayloadJson does not double Bearer prefix`() {
        val payload = PayloadBuilder.buildPayloadJson(
            token = "Bearer eyJalready_prefixed",
            amount = 100,
            accountId = "acc",
            companyName = "S",
            mcc = "5999",
            description = null,
            customerName = null,
            customerEmail = null,
            customerPhone = null,
            orderId = "O",
            locale = "en_US",
            deviceModel = "T",
            osVersion = "1"
        )
        assertEquals("Bearer eyJalready_prefixed", payload.getString("authToken"))
    }

    @Test
    fun `buildPayloadJson pads amount to 12 characters`() {
        val payload = PayloadBuilder.buildPayloadJson(
            token = "t",
            amount = 1,
            accountId = "a",
            companyName = "s",
            mcc = "5999",
            description = null,
            customerName = null,
            customerEmail = null,
            customerPhone = null,
            orderId = "o",
            locale = "en_US",
            deviceModel = "t",
            osVersion = "1"
        )
        assertEquals("000000000001", payload.getString("amountAuthorizedNumeric"))
    }

    @Test
    fun `buildPayloadJson pads large amount correctly`() {
        val payload = PayloadBuilder.buildPayloadJson(
            token = "t",
            amount = 999999999,
            accountId = "a",
            companyName = "s",
            mcc = "5999",
            description = null,
            customerName = null,
            customerEmail = null,
            customerPhone = null,
            orderId = "o",
            locale = "en_US",
            deviceModel = "t",
            osVersion = "1"
        )
        assertEquals("000999999999", payload.getString("amountAuthorizedNumeric"))
    }

    @Test
    fun `generateOrderId returns 12 uppercase hex characters`() {
        val orderId = PayloadBuilder.generateOrderId()
        assertEquals(12, orderId.length)
        assertTrue(orderId.matches(Regex("[0-9A-F]+")))
    }

    @Test
    fun `generateOrderId returns unique values`() {
        val ids = (1..100).map { PayloadBuilder.generateOrderId() }.toSet()
        assertEquals(100, ids.size)
    }

    @Test
    fun `buildCloudCommerceUri has correct scheme prefix`() {
        val uri = PayloadBuilder.buildCloudCommerceUri(
            token = "eyJtest",
            amount = 1500,
            accountId = "acc_123",
            companyName = "Store",
            mcc = "5999",
            description = null,
            customerName = null,
            customerEmail = null,
            customerPhone = null,
            orderId = "ABC123DEF456",
            locale = "en_US",
            deviceModel = "Pixel",
            osVersion = "14"
        )
        assertTrue(uri.startsWith("cloud_payment://cloudcommerce/json:"))
    }
}
