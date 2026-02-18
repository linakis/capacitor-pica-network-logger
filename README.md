<p align="center">
  <img src="assets/capacitor-pica-logo.png" alt="Capacitor Pica Network Logger logo" width="240" />
</p>

# capacitor-pica-network-logger

Capacitor HTTP inspector with debug-only native capture, a KMP Compose viewer, and JS wrapper that mirrors `@capacitor/http`.

## Tech stack

- Capacitor 7
- Kotlin Multiplatform + Compose Multiplatform
- SQLDelight
- Swift (URLProtocol logging on iOS)
- Kotlin (HttpURLConnection reflection logging on Android)
- TypeScript wrapper for `@capacitor/http`

## Features

- Debug-only request/response logging
- KMP inspector UI (standalone Activity/UIViewController)
- Notifications to open inspector
- Redaction + max body size from Capacitor config
- cURL/JSON/HAR exports + copy/share
- Save cURL/JSON/HAR locally
- Copy all logs as HAR

## Project layout

- `src/`: JS wrapper and plugin typings
- `android/`: Capacitor Android plugin + reflection hook
- `ios/`: Capacitor iOS plugin + URLProtocol logger
- `kmp/`: KMP shared UI + SQLDelight schema
- `examples/sample-app/`: demo app

## Installation

```bash
npm install capacitor-pica-network-logger
```

```bash
npx cap sync
```

## Configuration

Add to your app's `capacitor.config.ts`:

```ts
plugins: {
  PicaNetworkLogger: {
    enabled: true,
    maxBodySize: 131072,
    notify: true,
    redactHeaders: ["authorization", "cookie"],
    redactJsonFields: ["password", "token"]
  }
}
```

## Usage

Import the wrapper and use it in place of `@capacitor/http`:

```ts
import { CapacitorHttp, PicaNetworkLogger } from 'capacitor-pica-network-logger';

await CapacitorHttp.get({ url: 'https://example.com' });
await PicaNetworkLogger.openInspector();
```

If you want to keep call sites unchanged, alias `@capacitor/http` to this package in your bundler.

### Minimal changes (existing `@capacitor/http` usage)

If you already use `@capacitor/http`, you can keep your code as-is by adding a module alias so imports resolve to this package.

**Vite**

```ts
// vite.config.ts
import { defineConfig } from 'vite';

export default defineConfig({
  resolve: {
    alias: {
      '@capacitor/http': 'capacitor-pica-network-logger'
    }
  }
});
```

**Webpack**

```js
// webpack.config.js
module.exports = {
  resolve: {
    alias: {
      '@capacitor/http': 'capacitor-pica-network-logger'
    }
  }
};
```

**TypeScript paths (editor/typecheck)**

```json
// tsconfig.json
{
  "compilerOptions": {
    "paths": {
      "@capacitor/http": ["node_modules/capacitor-pica-network-logger"]
    }
  }
}
```

## Build

From the repo root:

```bash
npm install
npm run build
```

KMP module:

```bash
cd kmp
./gradlew :shared:build
```

## Sample app

```bash
cd examples/sample-app
npm install
npm run dev
```

## Modifications

- JS wrapper: `src/httpWithLogging.ts`
- Android hook: `android/src/main/java/com/linakis/capacitorpicanetworklogger/ReflectionHook.kt`
- iOS logger: `ios/Plugin/InspectorURLProtocol.swift`
- UI: `kmp/shared/src/commonMain/kotlin/com/linakis/capacitorpicanetworklogger/kmp/InspectorScreen.kt`
- DB schema: `kmp/shared/src/commonMain/sqldelight/com/linakis/capacitorpicanetworklogger/kmp/db/InspectorDatabase.sq`

## Notes

- Android reflection hook uses `URLStreamHandlerFactory` and will fail if another library set one first.
- The JS wrapper is still authoritative on Android; reflection logging is best-effort.
