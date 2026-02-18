# AGENTS.md

This file is for automated agents working in this repository.
Follow project conventions and keep changes minimal, focused, and consistent.

## Repository overview
- Capacitor plugin that wraps @capacitor/http with logging.
- Native iOS plugin (Swift) with URLProtocol-based logging.
- Native Android plugin (Kotlin) with inspector activity + notifications.
- Native inspector UI on iOS (Swift) and Android (Kotlin).
- Sample app in examples/sample-app/ for manual testing.

## Commands

### JS/TypeScript (repo root)
- Install: `npm install`
- Build: `npm run build`
- Clean: `npm run clean`

Notes
- NPM publish must be done manually due to OTP/2FA requirements.
- To publish from a terminal: `npm publish` (or `npm publish --otp <code>` if prompted).

Notes
- The root package.json only includes TypeScript build/clean.
- There is no lint or unit test script configured at the root.

### Sample app (examples/sample-app/)
- Install: `cd examples/sample-app && npm install`
- Dev server: `cd examples/sample-app && npm run dev`
- Build: `cd examples/sample-app && npm run build`

### iOS sample app (manual)
- Open `examples/sample-app/ios/App/App.xcworkspace` in Xcode
- Ensure pods are installed via `pod install` from `examples/sample-app/ios/App`
- Run on a simulator or device

### Android sample app (manual)
- Open `examples/sample-app/android` in Android Studio
- Sync Gradle and run the app

## Code style guidelines

### General
- Prefer small, focused changes in the primary file(s) involved.
- Keep behaviors backward compatible; this is a library used by apps.
- Avoid adding debug logging; remove temporary logs before finalizing.
- Prefer deterministic behavior and explicit defaults.

### TypeScript (src/)
- Use ES modules and named exports.
- Prefer `const` and `type` imports (`import type { ... }`).
- Keep functions pure and side effects localized.
- Favor explicit return types on exported functions.
- Use `async/await` and try/catch for native plugin calls.
- Swallow native plugin errors only when the plugin is optional; leave comments.
- Use camelCase for variables and functions, PascalCase for types.

### Swift (ios/Plugin/)
- Follow Swift 5 conventions; use `guard` for early exits.
- Keep plugin methods small and `@objc` as required by Capacitor.
- Avoid force unwraps; use optional binding.
- Use `DispatchQueue.main.async` for UI presentation.
- Use `CAPBridgedPlugin` methods list for exported API.
- Keep file-level `#if canImport(...)` blocks consistent.
- Prefer `let` over `var` unless mutation is necessary.
- Naming: classes/structs PascalCase, methods/properties camelCase.

### Android (android/)
- Keep plugin behavior guarded and non-invasive; do not crash if optional APIs are unavailable.


## Formatting and imports

### TypeScript
- Use standard TypeScript formatting (2 spaces).
- Group imports: external, internal, then types.
- Prefer `type` imports when used only for types.

### Swift
- One import per line.
- Keep type extensions near the type they extend.
- Use 4 spaces for indentation; align with Xcode defaults.

### Kotlin
- Follow Kotlin official style (4 spaces, braces on same line).
- Keep imports explicit; avoid wildcard imports.
- Order imports: kotlin/java, then external, then project.

## Error handling
- JS wrapper should not throw when the native plugin is absent; ignore errors.
- For required parameters, validate early and call `reject` with a clear message.
- For optional behaviors, fail closed and return defaults.
- Avoid swallowing errors without a reason; add a short comment if needed.

## Testing guidance
- There are no unit tests configured yet.
- Prefer manual verification in the sample app after changes.
- When adding tests, document the command in this file.

## Repo-specific notes
- The root podspec is `CapacitorPicaNetworkLogger.podspec`.
- The sample app references the plugin via `file:../../`.
- For URL logging on iOS, use `InspectorURLProtocol` and the logger.
- IMPORTANT: Store verification artifacts (screenshots, logs, recordings) in `tmp/` at the repo root.

## Cursor/Copilot rules
- No `.cursor/rules`, `.cursorrules`, or `.github/copilot-instructions.md` found.
