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

### GitHub Packages (recommended)

GitHub Packages Maven needs auth (PAT with `read:packages` or `GITHUB_TOKEN` in CI). Add to `gradle.properties`:

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_TOKEN
```

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/MONEI/monei-pay-android-sdk")
            credentials {
                username = providers.gradleProperty("gpr.user")
                    .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                    .get()
                password = providers.gradleProperty("gpr.key")
                    .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                    .get()
            }
        }
    }
}

// app/build.gradle.kts
dependencies {
    implementation("com.monei:monei-pay-sdk:1.2.0")
}
```

### JitPack (no auth)

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

// app/build.gradle.kts
dependencies {
    implementation("com.github.MONEI.monei-pay-android-sdk:sdk:v1.2.0")
}
```

[![](https://jitpack.io/v/MONEI/monei-pay-android-sdk.svg)](https://jitpack.io/#MONEI/monei-pay-android-sdk)

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
} catch (e: MoneiPayException.PaymentFailed) {
    // Declined. e.payment has the decline data when MONEI Pay sends it.
    println("Payment declined: ${e.payment?.statusMessage ?: e.reason}")
} catch (e: MoneiPayException) {
    println("Payment failed: ${e.message}")
}
```

## API Reference

### `MoneiPay.acceptPayment(...)`

Accepts an NFC payment. Suspending function — call from a coroutine.

| Parameter       | Type          | Required | Description                              |
| --------------- | ------------- | -------- | ---------------------------------------- |
| `context`       | `Context`     | Yes      | Activity or Application context          |
| `token`         | `String`      | Yes      | Raw JWT auth token (no "Bearer " prefix) |
| `amount`        | `Int`         | Yes      | Amount in cents                          |
| `description`   | `String?`     | No       | Payment description                      |
| `customerName`  | `String?`     | No       | Customer name                            |
| `customerEmail` | `String?`     | No       | Customer email                           |
| `customerPhone` | `String?`     | No       | Customer phone                           |
| `callbackUrl`   | `String?`     | No       | HTTPS endpoint for the signed webhook. Trusted channel — use for fulfillment. Must be `https://`, max 2048 chars. |
| `orderId`        | `String?`     | No       | Merchant order reference. Surfaced in the webhook callback for reconciliation. Max 2048 chars. If omitted, the SDK generates one. |
| `transactionType`| `String?`     | No       | Optional: `SALE` (default), `AUTH`, `REFUND`, `CAPTURE`, `CANCEL`, `PAYOUT`, `VERIF`. Server-validated. |
| `mode`          | `PaymentMode` | No       | `DIRECT` (default) or `VIA_MONEI_PAY`    |

Returns `PaymentResult`. Throws `MoneiPayException`.

### `PaymentResult`

| Property           | Type      | Description                         |
| ------------------ | --------- | ----------------------------------- |
| `transactionId`    | `String`  | Unique transaction ID               |
| `success`          | `Boolean` | Whether payment was approved        |
| `amount`           | `Int?`    | Amount in cents                     |
| `cardBrand`        | `String?` | Card brand (visa, mastercard, etc.) |
| `maskedCardNumber` | `String?` | Masked card number (****1234)       |
| `orderId`           | `String?` | Merchant order reference |
| `currency`          | `String?` | ISO 4217 currency code (`EUR`) |
| `status`            | `String?` | MONEI payment status (`SUCCEEDED`, `AUTHORIZED`, `FAILED`, ...) |
| `statusCode`        | `String?` | MONEI status code (`E000`, `E301`, ...) |
| `statusMessage`     | `String?` | MONEI status message (`Insufficient funds`) |
| `authorizationCode` | `String?` | Issuer authorization code. Approved payments only. |
| `last4`             | `String?` | Last 4 digits of the card |
| `cardType`          | `String?` | `credit`, `debit` or `prepaid` |
| `cardCountry`       | `String?` | Card country, ISO 3166-1 alpha-2 (`ES`) |

The new fields are optional. A field is `null` when the payment app does not send it:

- `VIA_MONEI_PAY`: MONEI Pay sends the fields. Older MONEI Pay versions do not send them.
- `DIRECT`: the SDK reads `authorizationCode` and `last4` from the CloudCommerce response. It reads `status`, `statusCode`, `statusMessage`, `cardType` and `cardCountry` only when the response has a `partnerDataMap` object. `orderId` and `currency` are always `null`.

Use `success` to know if the payment is approved. Do not use `status` or `statusCode` for this.

> **Important:** `PaymentResult` is display data only. Before you fulfill the order, confirm the payment on your server with the signed webhook (`callbackUrl`) or `GET /payments/{id}`.

### `PaymentMode`

| Mode            | Description                                           |
| --------------- | ----------------------------------------------------- |
| `DIRECT`        | Launches CloudCommerce directly — no MONEI Pay needed |
| `VIA_MONEI_PAY` | Launches MONEI Pay, which handles the NFC payment     |

### `MoneiPayException`

| Type                        | Description                                     |
| --------------------------- | ----------------------------------------------- |
| `MoneiPayNotInstalled`      | MONEI Pay not on device (VIA_MONEI_PAY mode)    |
| `CloudCommerceNotInstalled` | CloudCommerce not on device (DIRECT mode)       |
| `PaymentInProgress`         | Another payment is active                       |
| `PaymentCancelled`          | User cancelled                                  |
| `PaymentFailed`             | Payment declined/failed (has `reason` property). On a decline in `VIA_MONEI_PAY` mode, `payment` has the declined `PaymentResult` (id, status, reason, card) when MONEI Pay sends it. |
| `InvalidParameters`         | Invalid input parameters                        |
| `InvalidToken`              | Auth token expired or invalid                   |

## Example App

The [`examples/merchant-demo/`](examples/merchant-demo/) directory contains a minimal Android app demonstrating the full payment flow:

1. Enter your MONEI API key and optional POS ID
2. Fetch a POS auth token
3. Enter an amount and select payment mode (DIRECT or VIA_MONEI_PAY)
4. Accept an NFC payment

To run it:

```bash
cd examples/merchant-demo
./gradlew assembleDebug
```

Then install on a device with NFC. The demo uses a local `includeBuild` reference to the SDK, so changes to the SDK are reflected immediately.

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
