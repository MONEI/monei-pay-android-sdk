package com.monei.pay.sdk

import com.monei.pay.sdk.internal.JwtDecoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class JwtDecoderTest {

    private fun createJwt(claims: String): String {
        val header = Base64.getUrlEncoder().withoutPadding().encodeToString(
            """{"alg":"HS256","typ":"JWT"}""".toByteArray()
        )
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
            claims.toByteArray()
        )
        return "$header.$payload.signature"
    }

    @Test
    fun `decode extracts claims from valid JWT`() {
        val jwt = createJwt("""{"account_id":"acc_123","company_name":"Test Store","mcc":"5411"}""")
        val claims = JwtDecoder.decode(jwt)

        assertNotNull(claims)
        assertEquals("acc_123", claims!!.getString("account_id"))
        assertEquals("Test Store", claims.getString("company_name"))
        assertEquals("5411", claims.getString("mcc"))
    }

    @Test
    fun `decode strips Bearer prefix`() {
        val jwt = createJwt("""{"account_id":"acc_456"}""")
        val claims = JwtDecoder.decode("Bearer $jwt")

        assertNotNull(claims)
        assertEquals("acc_456", claims!!.getString("account_id"))
    }

    @Test
    fun `decode returns null for invalid token`() {
        assertNull(JwtDecoder.decode("not-a-jwt"))
        assertNull(JwtDecoder.decode(""))
        assertNull(JwtDecoder.decode("only.two"))
        assertNull(JwtDecoder.decode("single"))
    }

    @Test
    fun `decode returns null for invalid base64 payload`() {
        assertNull(JwtDecoder.decode("header.!!!invalid!!!.sig"))
    }

    @Test
    fun `isExpired returns true for expired token`() {
        val pastExp = (System.currentTimeMillis() / 1000) - 1000
        val jwt = createJwt("""{"exp":$pastExp}""")
        assertTrue(JwtDecoder.isExpired(jwt))
    }

    @Test
    fun `isExpired returns true for token expiring within 5 minutes`() {
        // 100 seconds from now, within 300-second buffer
        val soonExp = (System.currentTimeMillis() / 1000) + 100
        val jwt = createJwt("""{"exp":$soonExp}""")
        assertTrue(JwtDecoder.isExpired(jwt))
    }

    @Test
    fun `isExpired returns false for valid token`() {
        val futureExp = (System.currentTimeMillis() / 1000) + 3600
        val jwt = createJwt("""{"exp":$futureExp}""")
        assertFalse(JwtDecoder.isExpired(jwt))
    }

    @Test
    fun `isExpired returns true for missing exp claim`() {
        val jwt = createJwt("""{"account_id":"acc_123"}""")
        assertTrue(JwtDecoder.isExpired(jwt))
    }

    @Test
    fun `isExpired returns true for invalid token`() {
        assertTrue(JwtDecoder.isExpired("invalid"))
    }
}
