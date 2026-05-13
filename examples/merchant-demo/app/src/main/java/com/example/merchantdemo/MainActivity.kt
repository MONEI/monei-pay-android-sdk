package com.example.merchantdemo

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.merchantdemo.databinding.ActivityMainBinding
import com.monei.pay.sdk.MoneiPay
import com.monei.pay.sdk.MoneiPayException
import com.monei.pay.sdk.PaymentMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal demo showing how to integrate with MONEI Pay SDK for NFC payments.
 *
 * Flow: Enter API key + POS ID → fetch token → enter amount → accept payment.
 *
 * Supports two modes:
 * - DIRECT: SDK launches CloudCommerce directly (no MONEI Pay app needed)
 * - VIA_MONEI_PAY: SDK launches MONEI Pay app, which handles NFC
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val DEFAULT_USER_AGENT = "MONEI/MerchantDemoAndroid/0.2.1"
    }

    private lateinit var binding: ActivityMainBinding
    private var authToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.fetchTokenButton.setOnClickListener { fetchToken() }
        binding.acceptPaymentButton.setOnClickListener { acceptPayment() }
        updatePaymentButtonState()
    }

    /**
     * Fetch POS auth token from MONEI API using the API key.
     */
    private fun fetchToken() {
        val apiKey = binding.apiKeyInput.text.toString().trim()
        val posId = binding.posIdInput.text.toString().trim()
        val accountId = binding.accountIdInput.text.toString().trim()
        val userAgent = binding.userAgentInput.text.toString().trim()

        if (apiKey.isEmpty()) {
            Toast.makeText(this, "Enter API key", Toast.LENGTH_SHORT).show()
            return
        }

        if (accountId.isNotEmpty() && userAgent.isEmpty()) {
            Toast.makeText(this, "User-Agent must be provided when using Account ID", Toast.LENGTH_LONG).show()
            return
        }

        val effectiveUserAgent = if (userAgent.isNotEmpty()) userAgent else DEFAULT_USER_AGENT

        binding.fetchTokenButton.isEnabled = false
        binding.tokenStatus.text = "Fetching token..."
        binding.tokenStatus.visibility = android.view.View.VISIBLE

        lifecycleScope.launch {
            try {
                val token = withContext(Dispatchers.IO) {
                    val url = URL("https://api.monei.com/v1/pos/auth-token")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Authorization", apiKey)
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.setRequestProperty("User-Agent", effectiveUserAgent)
                    if (accountId.isNotEmpty()) {
                        conn.setRequestProperty("MONEI-Account-ID", accountId)
                    }
                    conn.doOutput = true

                    val body = JSONObject().apply {
                        if (posId.isNotEmpty()) put("pointOfSaleId", posId)
                    }
                    conn.outputStream.use { it.write(body.toString().toByteArray()) }

                    if (conn.responseCode == 200) {
                        val responseBody = conn.inputStream.bufferedReader().readText().trim()
                        JSONObject(responseBody).getString("token")
                    } else {
                        val error = conn.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                        throw Exception("HTTP ${conn.responseCode}: $error")
                    }
                }

                authToken = token
                binding.tokenStatus.text = "Token fetched ✓ (${token.take(20)}...)"
                binding.tokenStatus.setTextColor(getColor(android.R.color.holo_green_dark))
                updatePaymentButtonState()
            } catch (e: Exception) {
                binding.tokenStatus.text = "Error: ${e.message}"
                binding.tokenStatus.setTextColor(getColor(android.R.color.holo_red_dark))
            } finally {
                binding.fetchTokenButton.isEnabled = true
            }
        }
    }

    /**
     * Accept an NFC payment using the MONEI Pay SDK.
     */
    private fun acceptPayment() {
        val token = authToken
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Fetch a token first", Toast.LENGTH_SHORT).show()
            return
        }

        val amountText = binding.amountInput.text.toString()
        val amount = amountText.toIntOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Enter a valid amount in cents", Toast.LENGTH_SHORT).show()
            return
        }

        val mode = if (binding.modeDirect.isChecked) PaymentMode.DIRECT else PaymentMode.VIA_MONEI_PAY

        binding.resultCard.visibility = android.view.View.GONE
        binding.progressIndicator.visibility = android.view.View.VISIBLE
        binding.acceptPaymentButton.isEnabled = false

        lifecycleScope.launch {
            try {
                val result = MoneiPay.acceptPayment(
                    context = this@MainActivity,
                    token = token,
                    amount = amount,
                    mode = mode
                )
                showResult(
                    success = result.success,
                    message = buildString {
                        appendLine("Transaction ID: ${result.transactionId}")
                        if (result.amount != null) appendLine("Amount: ${result.amount} cents")
                        if (!result.cardBrand.isNullOrEmpty()) appendLine("Card Brand: ${result.cardBrand}")
                        if (!result.maskedCardNumber.isNullOrEmpty()) appendLine("Card Number: ${result.maskedCardNumber}")
                    }
                )
            } catch (e: MoneiPayException) {
                showResult(success = false, message = e.message ?: "Payment failed")
            } catch (e: Exception) {
                showResult(success = false, message = "Unexpected error: ${e.message}")
            } finally {
                binding.progressIndicator.visibility = android.view.View.GONE
                binding.acceptPaymentButton.isEnabled = true
            }
        }
    }

    private fun showResult(success: Boolean, message: String) {
        binding.resultCard.visibility = android.view.View.VISIBLE
        binding.resultStatus.text = if (success) "Payment Successful" else "Payment Failed"
        binding.resultStatus.setTextColor(
            getColor(if (success) android.R.color.holo_green_dark else android.R.color.holo_red_dark)
        )
        binding.resultDetails.text = message
    }

    private fun updatePaymentButtonState() {
        binding.acceptPaymentButton.isEnabled = !authToken.isNullOrEmpty()
    }
}
