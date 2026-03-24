package com.monei.pay.sdk.internal

import org.json.JSONObject
import java.util.Base64

/**
 * Decodes JWT claims from a token string.
 * Uses java.util.Base64 (available on API 26+) for JVM testability.
 */
internal object JwtDecoder {

    /**
     * Decode the payload section of a JWT.
     * @param token Raw JWT or "Bearer <jwt>" prefixed token.
     * @return JSONObject of claims, or null if invalid.
     */
    fun decode(token: String): JSONObject? {
        val jwt = token.removePrefix("Bearer ")
        val parts = jwt.split(".")
        if (parts.size < 3) return null
        return try {
            val decoded = Base64.getUrlDecoder().decode(parts[1])
            JSONObject(String(decoded, Charsets.UTF_8))
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Check if a JWT token has expired (with 5-min buffer).
     * Returns true if expired or if token is invalid.
     */
    fun isExpired(token: String): Boolean {
        val claims = decode(token) ?: return true
        val exp = claims.optLong("exp", 0)
        if (exp == 0L) return true
        return exp < (System.currentTimeMillis() / 1000) + 300
    }
}
