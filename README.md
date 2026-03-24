# MONEI Pay Android SDK

Accept NFC tap-to-pay payments in your Android app via [MONEI Pay](https://monei.com/monei-pay/).

Two integration modes:
- **DIRECT** — SDK launches CloudCommerce directly, no MONEI Pay needed
- **VIA_MONEI_PAY** — SDK launches MONEI Pay, which handles the NFC payment

## Requirements

- Android 8.0+ (API 26)
- POS auth token from your backend (`POST /v1/pos/auth-token`)
- CloudCommerce app (DIRECT mode) or MONEI Pay app (VIA_MONEI_PAY mode) installed

## Installation

### Gradle (GitHub Packages)

Add the repository and dependency:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/MONEI/monei-pay-android-sdk")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_USER")
                password = providers.gradleProperty("gpr.token").orNull ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

// app/build.gradle.kts
dependencies {
    implementation("com.monei:monei-pay-sdk:0.1.0")
}
```

## Usage

```kotlin
import com.monei.pay.sdk.MoneiPay
import com.monei.pay.sdk.PaymentMode
import com.monei.pay.sdk.MoneiPayException

// In a coroutine scope (e.g. viewModelScope, lifecycleScope)
try {
    val result = MoneiPay.acceptPayment(
        context = this,                    // Activity or Application context
        token = "eyJ...",                  // Raw JWT from your backend (no "Bearer " prefix)
        amount = 1500,                     // Amount in cents (1500 = 15.00 EUR)
        description = "Order #123",        // Optional
        customerName = "John Doe",         // Optional
        customerEmail = "john@example.com",// Optional
        customerPhone = "+34600000000",    // Optional
        mode = PaymentMode.DIRECT          // or PaymentMode.VIA_MONEI_PAY
    )

    println("Payment approved: ${result.transactionId}")
    println("Card: ${result.cardBrand} ${result.maskedCardNumber}")
} catch (e: MoneiPayException.MoneiPayNotInstalled) {
    // Prompt user to install MONEI Pay (VIA_MONEI_PAY mode)
} catch (e: MoneiPayException.CloudCommerceNotInstalled) {
    // Prompt user to install CloudCommerce (DIRECT mode)
} catch (e: MoneiPayException.PaymentCancelled) {
    // User cancelled
} catch (e: MoneiPayException.InvalidToken) {
    // Token expired or invalid — refresh from backend
} catch (e: MoneiPayException) {
    println("Payment failed: ${e.message}")
}
```

## API Reference

### `MoneiPay.acceptPayment(...)`

Accepts an NFC payment. Suspending function — call from a coroutine.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `context` | `Context` | Yes | Activity or Application context |
| `token` | `String` | Yes | Raw JWT auth token (no "Bearer " prefix) |
| `amount` | `Int` | Yes | Amount in cents |
| `description` | `String?` | No | Payment description |
| `customerName` | `String?` | No | Customer name |
| `customerEmail` | `String?` | No | Customer email |
| `customerPhone` | `String?` | No | Customer phone |
| `mode` | `PaymentMode` | No | `DIRECT` (default) or `VIA_MONEI_PAY` |

Returns `PaymentResult`. Throws `MoneiPayException`.

### `PaymentResult`

| Property | Type | Description |
|----------|------|-------------|
| `transactionId` | `String` | Unique transaction ID |
| `success` | `Boolean` | Whether payment was approved |
| `amount` | `Int?` | Amount in cents |
| `cardBrand` | `String?` | Card brand (visa, mastercard, etc.) |
| `maskedCardNumber` | `String?` | Masked card number (****1234) |

### `PaymentMode`

| Mode | Description |
|------|-------------|
| `DIRECT` | Launches CloudCommerce directly — no MONEI Pay needed |
| `VIA_MONEI_PAY` | Launches MONEI Pay, which handles the NFC payment |

### `MoneiPayException`

| Type | Description |
|------|-------------|
| `MoneiPayNotInstalled` | MONEI Pay not on device (VIA_MONEI_PAY mode) |
| `CloudCommerceNotInstalled` | CloudCommerce not on device (DIRECT mode) |
| `PaymentInProgress` | Another payment is active |
| `PaymentCancelled` | User cancelled |
| `PaymentFailed` | Payment declined/failed (has `reason` property) |
| `InvalidParameters` | Invalid input parameters |
| `InvalidToken` | Auth token expired or invalid |

## Token Generation

Your backend generates POS auth tokens via the MONEI API:

```bash
curl -X POST https://api.monei.com/v1/pos/auth-token \
  -H "Authorization: YOUR_API_KEY" \
  -H "Content-Type: application/json"
```

See the [MONEI API docs](https://docs.monei.com) for details.

## License

MIT
