package com.monei.pay.sdk

import com.monei.pay.sdk.internal.ResponseParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ResponseParserTest {

    // --- MONEI Pay result parsing (VIA_MONEI_PAY mode) ---

    @Test
    fun `parseMoneiPayResult extracts success data`() {
        val extras = mapOf<String, Any?>(
            "transaction_id" to "tx_123",
            "success" to true,
            "amount" to 1500,
            "card_brand" to "visa",
            "masked_card_number" to "****1234",
            "error_code" to null,
            "error_message" to null
        )

        val result = ResponseParser.parseMoneiPayResult(extras)
        assertEquals("tx_123", result.transactionId)
        assertTrue(result.success)
        assertEquals(1500, result.amount)
        assertEquals("visa", result.cardBrand)
        assertEquals("****1234", result.maskedCardNumber)
    }

    @Test(expected = MoneiPayException.PaymentCancelled::class)
    fun `parseMoneiPayResult throws cancelled for USER_DENIED`() {
        ResponseParser.parseMoneiPayResult(
            mapOf("error_code" to "USER_DENIED", "error_message" to "Denied")
        )
    }

    @Test(expected = MoneiPayException.PaymentCancelled::class)
    fun `parseMoneiPayResult throws cancelled for CANCELLED`() {
        ResponseParser.parseMoneiPayResult(
            mapOf("error_code" to "CANCELLED", "error_message" to null)
        )
    }

    @Test(expected = MoneiPayException.PaymentCancelled::class)
    fun `parseMoneiPayResult throws cancelled for USER_CANCELLED`() {
        ResponseParser.parseMoneiPayResult(
            mapOf("error_code" to "USER_CANCELLED", "error_message" to null)
        )
    }

    @Test(expected = MoneiPayException.InvalidToken::class)
    fun `parseMoneiPayResult throws invalidToken for TOKEN_EXPIRED`() {
        ResponseParser.parseMoneiPayResult(
            mapOf("error_code" to "TOKEN_EXPIRED", "error_message" to "Token expired")
        )
    }

    @Test(expected = MoneiPayException.InvalidToken::class)
    fun `parseMoneiPayResult throws invalidToken for NOT_AUTHENTICATED`() {
        ResponseParser.parseMoneiPayResult(
            mapOf("error_code" to "NOT_AUTHENTICATED", "error_message" to null)
        )
    }

    @Test(expected = MoneiPayException.PaymentFailed::class)
    fun `parseMoneiPayResult throws for generic error code`() {
        ResponseParser.parseMoneiPayResult(
            mapOf("error_code" to "SOME_ERROR", "error_message" to "Something went wrong")
        )
    }

    @Test(expected = MoneiPayException.PaymentFailed::class)
    fun `parseMoneiPayResult throws for missing transaction_id`() {
        ResponseParser.parseMoneiPayResult(
            mapOf(
                "transaction_id" to null,
                "success" to false,
                "error_code" to null,
                "error_message" to null
            )
        )
    }

    @Test
    fun `parseMoneiPayResult handles empty card fields`() {
        val result = ResponseParser.parseMoneiPayResult(
            mapOf(
                "transaction_id" to "tx_789",
                "success" to true,
                "amount" to 500,
                "card_brand" to "",
                "masked_card_number" to "",
                "error_code" to null,
                "error_message" to null
            )
        )
        assertEquals("tx_789", result.transactionId)
        assertNull(result.cardBrand)
        assertNull(result.maskedCardNumber)
    }

    @Test
    fun `parseMoneiPayResult extracts payment result fields`() {
        val result = ResponseParser.parseMoneiPayResult(
            mapOf(
                "transaction_id" to "tx_123",
                "success" to true,
                "order_id" to "ORDER1",
                "currency" to "EUR",
                "status" to "SUCCEEDED",
                "status_code" to "E000",
                "status_message" to "Transaction approved",
                "authorization_code" to "123456",
                "last4" to "4242",
                "masked_card_number" to "****1234",
                "card_type" to "credit",
                "card_country" to "ES"
            )
        )
        assertEquals("ORDER1", result.orderId)
        assertEquals("EUR", result.currency)
        assertEquals("SUCCEEDED", result.status)
        assertEquals("E000", result.statusCode)
        assertEquals("Transaction approved", result.statusMessage)
        assertEquals("123456", result.authorizationCode)
        assertEquals("4242", result.last4)
        assertEquals("credit", result.cardType)
        assertEquals("ES", result.cardCountry)
    }

    @Test
    fun `parseMoneiPayResult takes last4 from masked card number when last4 is empty`() {
        val result = ResponseParser.parseMoneiPayResult(
            mapOf(
                "transaction_id" to "tx_123",
                "success" to true,
                "last4" to "",
                "masked_card_number" to "541333xxxxxx1034",
                "status_code" to ""
            )
        )
        assertEquals("1034", result.last4)
        assertNull(result.statusCode)
    }

    @Test
    fun `parseMoneiPayResult exposes decline data on PaymentFailed`() {
        try {
            ResponseParser.parseMoneiPayResult(
                mapOf(
                    "error_code" to "PAYMENT_FAILED",
                    "error_message" to "Insufficient funds",
                    "success" to false,
                    "transaction_id" to "tx_declined",
                    "order_id" to "ORDER1",
                    "status" to "FAILED",
                    "status_code" to "E301",
                    "status_message" to "Insufficient funds",
                    "masked_card_number" to "****1234",
                    "card_type" to "debit",
                    "card_country" to "ES"
                )
            )
            fail("Expected PaymentFailed")
        } catch (e: MoneiPayException.PaymentFailed) {
            assertEquals("Insufficient funds", e.reason)
            val payment = e.payment!!
            assertEquals("tx_declined", payment.transactionId)
            assertFalse(payment.success)
            assertEquals("ORDER1", payment.orderId)
            assertEquals("FAILED", payment.status)
            assertEquals("E301", payment.statusCode)
            assertEquals("Insufficient funds", payment.statusMessage)
            assertEquals("1234", payment.last4)
            assertEquals("debit", payment.cardType)
            assertEquals("ES", payment.cardCountry)
        }
    }

    @Test
    fun `parseMoneiPayResult attaches no payment data when outcome is unknown`() {
        // An unknown outcome may still be approved, so it must not look like a decline.
        try {
            ResponseParser.parseMoneiPayResult(
                mapOf(
                    "error_code" to "OUTCOME_UNKNOWN",
                    "error_message" to "Check the payment status",
                    "transaction_id" to "tx_unknown"
                )
            )
            fail("Expected PaymentFailed")
        } catch (e: MoneiPayException.PaymentFailed) {
            assertNull(e.payment)
        }
    }

    // --- CloudCommerce response parsing (DIRECT mode) ---

    @Test
    fun `parseCloudCommerceJson parses success response`() {
        val json = """{"success":true,"transactionId":"tx_456","cardBrandName":"mastercard","maskedCardNumber":"****5678"}"""
        val result = ResponseParser.parseCloudCommerceJson(json, 2000)

        assertEquals("tx_456", result.transactionId)
        assertTrue(result.success)
        assertEquals(2000, result.amount)
        assertEquals("mastercard", result.cardBrand)
        assertEquals("****5678", result.maskedCardNumber)
    }

    @Test
    fun `parseCloudCommerceJson maps partnerDataMap and authorizationCode`() {
        val json = """{"success":true,"transactionId":"tx_456","maskedCardNumber":"541333xxxxxx1034",""" +
            """"authorizationCode":"313145","partnerDataMap":{"status":"SUCCEEDED","statusCode":"E000",""" +
            """"statusMessage":"Transaction approved","cardType":"credit","cardCountry":""}}"""
        val result = ResponseParser.parseCloudCommerceJson(json, 2000)

        assertEquals("SUCCEEDED", result.status)
        assertEquals("E000", result.statusCode)
        assertEquals("Transaction approved", result.statusMessage)
        assertEquals("313145", result.authorizationCode)
        assertEquals("1034", result.last4)
        assertEquals("credit", result.cardType)
        assertNull(result.cardCountry)
    }

    @Test(expected = MoneiPayException.PaymentFailed::class)
    fun `parseCloudCommerceJson throws for error array`() {
        val json = """[{"ReasonCode":"DECLINED","Description":"Card declined"}]"""
        ResponseParser.parseCloudCommerceJson(json, 1000)
    }

    @Test(expected = MoneiPayException.PaymentFailed::class)
    fun `parseCloudCommerceJson throws for error object in response`() {
        val json = """{"success":false,"error":{"ReasonCode":"TIMEOUT","Description":"Timed out"}}"""
        ResponseParser.parseCloudCommerceJson(json, 1000)
    }

    @Test(expected = MoneiPayException.PaymentFailed::class)
    fun `parseCloudCommerceJson throws for missing transactionId`() {
        val json = """{"success":true}"""
        ResponseParser.parseCloudCommerceJson(json, 1000)
    }

    @Test
    fun `parseCloudCommerceJson handles null card fields`() {
        val json = """{"success":true,"transactionId":"tx_789","cardBrandName":"","maskedCardNumber":""}"""
        val result = ResponseParser.parseCloudCommerceJson(json, 500)

        assertEquals("tx_789", result.transactionId)
        assertNull(result.cardBrand)
        assertNull(result.maskedCardNumber)
    }

    @Test
    fun `parseCloudCommerceResponse decodes base64 and parses`() {
        val json = """{"success":true,"transactionId":"tx_b64","cardBrandName":"visa","maskedCardNumber":"****9999"}"""
        val base64 = java.util.Base64.getEncoder().encodeToString(json.toByteArray())

        val result = ResponseParser.parseCloudCommerceResponse(base64, 3000)
        assertEquals("tx_b64", result.transactionId)
        assertTrue(result.success)
        assertEquals(3000, result.amount)
        assertEquals("visa", result.cardBrand)
    }

    @Test(expected = MoneiPayException.PaymentFailed::class)
    fun `parseCloudCommerceResponse throws for invalid base64`() {
        ResponseParser.parseCloudCommerceResponse("not-valid-base64!!!", 1000)
    }

    @Test
    fun `parseCloudCommerceJson error message includes ReasonCode`() {
        try {
            val json = """[{"ReasonCode":"DECLINED","Description":"Insufficient funds"}]"""
            ResponseParser.parseCloudCommerceJson(json, 1000)
        } catch (e: MoneiPayException.PaymentFailed) {
            assertTrue(e.message!!.contains("DECLINED"))
            assertTrue(e.message!!.contains("Insufficient funds"))
        }
    }
}
