# Android Merchant Demo

Minimal Kotlin app showing how to integrate with MONEI Pay SDK for NFC payments.

Supports two payment modes:

- **DIRECT** — SDK launches CloudCommerce directly (no MONEI Pay app needed)
- **VIA_MONEI_PAY** — SDK launches the MONEI Pay app, which handles NFC

## Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- Android device with NFC (min SDK 26)
- MONEI Pay installed on the same device (for VIA_MONEI_PAY mode)

## Setup

1. Open this directory in Android Studio
2. Sync Gradle and build
3. Run on a device with NFC

## How It Works

### 1. Get an Auth Token

Your backend calls the MONEI API to generate a POS auth token:

```bash
curl -X POST https://api.monei.com/v1/pos/auth-token \
  -H "Authorization: YOUR_API_KEY" \
  -H "Content-Type: application/json"
```

### 2. Accept Payment

```kotlin
import com.monei.pay.sdk.MoneiPay
import com.monei.pay.sdk.PaymentMode

val result = MoneiPay.acceptPayment(
    context = this,
    token = authToken,       // JWT from your backend
    amount = 1500,           // cents (15.00 EUR)
    mode = PaymentMode.DIRECT
)

println(result.transactionId)    // "txn_abc123"
println(result.cardBrand)        // "visa"
println(result.maskedCardNumber) // "****1234"
```

### Required Manifest Configuration

```xml
<!-- Network access for token fetch -->
<uses-permission android:name="android.permission.INTERNET" />

<!-- Required for VIA_MONEI_PAY mode -->
<uses-permission android:name="com.monei.pay.permission.ACCEPT_PAYMENT" />

<!-- Required on API 30+ for intent resolution -->
<queries>
    <!-- For VIA_MONEI_PAY mode -->
    <intent>
        <action android:name="com.monei.pay.ACCEPT_PAYMENT" />
    </intent>
    <!-- For DIRECT mode (CloudCommerce) -->
    <package android:name="com.mastercard.cpos" />
</queries>
```

For DIRECT mode, the SDK also uses the `cloud_payment://` deep link scheme to receive payment results from CloudCommerce. The SDK handles this internally.

### Payment Result

The SDK returns a `PaymentResult` with:

| Field              | Type    | Description                              |
| ------------------ | ------- | ---------------------------------------- |
| `transactionId`    | String  | Unique transaction identifier            |
| `success`          | Boolean | `true` if approved, `false` if declined  |
| `amount`           | Int?    | Amount in cents                          |
| `cardBrand`        | String? | Card brand (e.g. "visa")                 |
| `maskedCardNumber` | String? | Masked card number (e.g. "\*\*\*\*1234") |

### Error Handling

```kotlin
try {
    val result = MoneiPay.acceptPayment(...)
} catch (e: MoneiPayException) {
    println(e.code)    // e.g. "NOT_AUTHENTICATED"
    println(e.message) // Human-readable description
}
```

## Project Structure

```
app/src/main/
├── AndroidManifest.xml                         # Permissions + queries declaration
├── java/com/example/merchantdemo/
│   └── MainActivity.kt                         # Single activity: input, pay, display result
└── res/
    ├── layout/activity_main.xml                # UI layout
    └── values/
        ├── strings.xml                         # String resources
        └── themes.xml                          # Material 3 theme
```
