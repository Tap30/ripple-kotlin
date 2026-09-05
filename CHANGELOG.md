# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed
- Deprecated `FOREGROUND` and `BACKGROUND` app states. Explicit use still sends the event and logs a warning; use `OPENED` or `CLOSED` instead.
- Removed automatic Android foreground/background lifecycle tracking. Use `appOpened()` and `appClosed()` to track app lifecycle explicitly.

## [2.0.0] - 2026-06-20

### Added
- Persistent anonymous and user identity support, with Android-backed identity storage.
- Android automatic app-state tracking.
- Android `screen()` helper with Activity title auto-fill.
- Automatic SDK telemetry reporting.
- Predefined event specs and convenience tracking methods for product, cart, checkout, order, coupon, promotion, payment, referral, incentive, challenge, screen, click, view, and app-state events.
- `BatchOptions`, `RetryOptions`, bounded buffer controls, event TTL, event sampling, and telemetry hooks for enqueue, flush, retry, drop, send success, and send failure.
- `JsonPayloadMapper` for converting untyped `Map<String, Any>` payloads to `JsonObject` while preserving nested objects and arrays.
- Expanded unit coverage for dispatcher behavior, predefined event specs, JSON payload mapping, config validation, metadata, and client lifecycle.

### Changed
- Dispatcher now uses a bounded `ArrayDeque`, single scheduled executor, payload-size-aware batching, scheduled interval flushes, and focused retry/requeue persistence helpers.
- Metadata reads now return cached immutable snapshots to reduce repeated map allocations.
- File storage now uses Kotlin serialization for persisted event data.
- Untyped `track()` payloads are serialized through `JsonObject`; type-safe events expose `getPayload()` and optional schema versions.
- Samples now demonstrate v2 predefined events across Android, Spring Kotlin, and Spring Java.

### Fixed
- OkHttp and WebClient adapters now handle HTTP 204 and empty response bodies without trying to read missing content.
- Failed batches preserve FIFO order when they are requeued and persisted.
- Dispatcher retry delays now honor configurable retry options and max delay.
- Configuration validation now rejects blank endpoints/API keys and invalid batch, retry, and buffer settings earlier.

### Removed
- Removed session IDs from the event model.
- Removed `getSessionId()`.

[Unreleased]: https://github.com/Tap30/ripple-kotlin/compare/2.0.0...HEAD
[2.0.0]: https://github.com/Tap30/ripple-kotlin/releases/tag/2.0.0
