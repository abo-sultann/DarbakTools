# Darbak Core — Integration Guide

## Existing Groovy Android project
1. Add `:darbak-core` module or import the module as a shared source/library.
2. Add `implementation project(':darbak-core')`.
3. Call at the start of the launcher Activity:
```java
DarbakCore.install(this);
DarbakCore.prepareCarScreen(this);
```
4. Open the standard About page with `DarbakAboutActivity`.
5. Configure the update manifest URL in Application meta-data using key:
`com.abosultan.darbak.UPDATE_MANIFEST_URL`.
6. Permissions are opt-in. Darbak Core does not silently add sensitive permissions to every host app.

## Permissions by feature
For apps that use the online update engine, the host manifest explicitly adds:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
```
Apps that do not use online updates must not add these solely because Core is present. Other runtime permissions are added only when an actual app feature requires them.

## Existing Kotlin / Compose project
The current Core is deliberately Android-View/Java compatible for API 25 and can coexist with Compose. Integrate the runtime pieces first (crash, diagnostics, update, backup), then reproduce Darbak design tokens in Compose. Do not force legacy View screens into an otherwise Compose-only app if that causes architectural duplication.

## Splash
Optional `DarbakSplashActivity` reads application meta-data key:
`com.abosultan.darbak.SPLASH_TARGET`
The host app decides whether to use it as launcher. Never create two launcher Activities.

## About
Use the standard About screen or implement the same content natively in Compose:
- app name/version
- update
- platform
- Darbak ownership mark
- hidden long-press diagnostics entry

## Diagnostics
`DarbakDiagnostics.inspect(context)` is safe to call without UI.
`DarbakDiagnostics.export(context)` stores a local report under app internal storage.

## Update
1. Host publishes manifest JSON.
2. Core checks versionCode.
3. APK downloads to cache with `.part` suffix.
4. Final releases publish SHA-256 and it must match before install.
5. Only verified complete file is renamed to `update.apk`.
6. Installer is launched through Darbak's dedicated FileProvider, allowing the host app to keep its own FileProvider without manifest collision.

## Migration order for current apps
1. DarbakTools — reference implementation.
2. Launcher 2026 — highest-use app, migrate cautiously and preserve current behavior.
3. DarbakMaintenance.
4. DarbakKidsTV.
5. Laqqinni.
6. DarbakAdhkar.
7. DarbakMaps.
8. DarbAlSout2.

Each migration must pass QA_CHECKLIST.md before merging to main.
