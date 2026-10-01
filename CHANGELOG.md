# Changelog

# [1.2.0](https://github.com/MONEI/monei-pay-android-sdk/compare/v1.1.0...v1.2.0) (2026-10-01)


### Features

* add payment result fields and decline data ([#1](https://github.com/MONEI/monei-pay-android-sdk/issues/1)) ([d17e9d5](https://github.com/MONEI/monei-pay-android-sdk/commit/d17e9d5260440930090c83c309a7cd39fa43b1af))

* `PaymentResult` has new optional fields: `orderId`, `currency`, `status`, `statusCode`, `statusMessage`, `authorizationCode`, `last4`, `cardType`, `cardCountry`. All default to `null`. Existing constructor calls and destructuring still compile.
* `VIA_MONEI_PAY` mode reads the new MONEI Pay result extras: `order_id`, `currency`, `status`, `status_code`, `status_message`, `authorization_code`, `last4`, `card_type`, `card_country`. If `last4` is missing, the SDK takes it from `masked_card_number`.
* `MoneiPayException.PaymentFailed` has a new optional `payment` property. On a decline (`PAYMENT_FAILED`), it holds the declined payment data (id, status, reason, card).
* `DIRECT` mode maps `authorizationCode`, `last4` and, when present, the `partnerDataMap` fields from the CloudCommerce response.
* Empty strings become `null`. `success` alone tells if the payment is approved.
* `PaymentResult` is display data only. Confirm the payment on your server (signed webhook or `GET /payments/{id}`) before fulfillment.

# [1.1.0](https://github.com/MONEI/monei-pay-android-sdk/compare/v1.0.0...v1.1.0) (2026-05-22)


### Features

* forward orderId and transactionType through SDK to customData ([410db65](https://github.com/MONEI/monei-pay-android-sdk/commit/410db65278d7d2660d4a70bd3bda32fae8b1da04))

# [1.0.0](https://github.com/MONEI/monei-pay-android-sdk/compare/v0.2.2...v1.0.0) (2026-05-22)


### Features

* Android SDK v1.0 — add callbackUrl param + wire to merchantCustomData ([da4ce16](https://github.com/MONEI/monei-pay-android-sdk/commit/da4ce16f1d2ac1d3bdf69a3bf59f473e20b695b8))

## [1.0.0](https://github.com/MONEI/monei-pay-android-sdk/compare/v0.2.2...v1.0.0) (2026-05-21)


### Features

* add optional `callbackUrl` parameter to `MoneiPay.acceptPayment()` for signed webhook delivery on payment completion (trusted server-side fulfillment channel, complementary to the sync `PaymentResult` return path)
* thread `callbackUrl` through DIRECT (CloudCommerce `merchantCustomData`) and VIA_MONEI_PAY (intent extra `callback_url`) paths
* bump `SDK_VERSION` and embedded `sourceVersion` to `1.0.0`


### BREAKING CHANGES

* coordinated v1.0 release across iOS / Android / React Native SDKs; aligns with monei-pay app v3.0 wire format (`callback_url` + `complete_url` replace legacy `callback` deep-link param)

## [0.2.2](https://github.com/MONEI/monei-pay-android-sdk/compare/v0.2.1...v0.2.2) (2026-05-13)


### Features

* **example:** support master account flow via MONEI-Account-ID and User-Agent ([2746972](https://github.com/MONEI/monei-pay-android-sdk/commit/2746972a864161670928295267697516ab0f85ae))

## [0.2.1](https://github.com/MONEI/monei-pay-android-sdk/compare/v0.2.0...v0.2.1) (2026-03-30)


### Bug Fixes

* update test assertion to match v0.2.0 SDK version ([e3757ea](https://github.com/MONEI/monei-pay-android-sdk/commit/e3757ea65843bb3500ea435b933f018dfde297ac))
