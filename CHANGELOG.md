# Changelog

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
