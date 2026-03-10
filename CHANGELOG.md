# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

## [0.2.8] - 2026-03-10

### Fixed
- iOS: Set `s.swift_version = '5.1'` in `CapacitorPicaNetworkLogger.podspec` to improve compatibility with Xcode 26/CocoaPods Swift toolchain resolution.

## [0.2.7] - 2026-03-05

### Fixed
- iOS: `enabled` and `notify` config options were not being read due to use of the deprecated `plugin.getConfig()` API which always returned an empty dictionary. Switched to `bridge?.config.getPluginConfig()` with the typed `getBoolean`/`getInt` API so all config values are correctly respected.
- iOS: `notify: false` was silently ignored because `notify` was missing from the config provider, causing notification permission to always be requested.
- iOS: When `enabled: false`, the `notify` flag was never applied to the logger singleton, leaving it in a notifying state.

## [0.2.6] - 2026-02-24

### Added
- `enabled` config option (`true` by default). Set to `false` to fully disable the plugin — no SQLite, no logging, no notifications, all methods resolve as no-ops.

## [0.2.5] - 2026-02-23

### Changed
- Redaction headers and JSON fields now default to empty (no redaction unless explicitly configured). Previously defaulted to `["authorization", "cookie"]` and `["password", "token"]`.
- Fixed inconsistent redaction behavior between iOS and Android when no config was provided.
- Binary response bodies (images, videos, audio, PDFs, etc.) are no longer stored in SQLite. A placeholder message is shown instead (e.g. `[Image preview not yet supported (image/png)]`).

## [0.2.4] - 2026-02-22

### Added
- Body text search with highlighting in both Android and iOS inspector UIs.
- Podspec now reads version from `package.json` automatically.

### Changed
- Persist logs across inspector sessions and align JVM targets.
- Reflow inspector list items for more readable path display.

## [0.2.3]

### Fixed
- Android notification grouping, silence, and body parsing improvements.

## [0.2.2]

### Fixed
- Restore Android plugin registration and make notifications silent on both platforms.

## [0.2.1]

### Added
- Streamlined bridge logging API.

### Fixed
- Fix Compose plugin registration.

### Docs
- Add walkthrough gif to README.

## [0.1.6]

### Fixed
- Fix Compose plugin.

### Docs
- Add walkthrough gif to README.

## [0.1.5]

_Release packaging only._

## [0.1.4]

_Release packaging only._

## [0.1.3]

_Release packaging only._

## [0.1.2]

_Release packaging only._

## [0.1.1]

### Added
- Silent and aligned inspector notifications.

### Fixed
- iOS safe-area view layout.
- Remove explicit permission request (library handles it).

## [0.1.0]

### Added
- Initial release.
- Capacitor HTTP inspector with debug-only native capture.
- Native inspector UI on iOS (UIKit) and Android (Jetpack Compose).
- Notification tap to open inspector.
- JS wrapper mirroring `@capacitor/http`.
- cURL, plain text, and HAR export/share support.
- Configurable header and JSON field redaction.
- Max body size configuration.
