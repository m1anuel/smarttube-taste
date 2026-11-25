# SmartTube-Taste

Öffnet SmartTube mit der linken Taste der Xiaomi-Fernbedienung, deren Symbol aus mehreren Vierecken besteht. Für den Xiaomi TV Stick 4K (MiTV-AYFR0, Android TV 11), wenn diese Taste nach dem Wechsel des Launchers ohne Funktion ist.

Die App hat kein Netz-Recht. Sie erkennt die Taste auf dem Stick und startet SmartTube.

## Installation

Die APK liegt unter `dist/smarttube-taste.apk`.

```bash
adb install dist/smarttube-taste.apk
adb shell pm grant com.manuel.appskey android.permission.READ_LOGS
adb shell settings put secure enabled_accessibility_services com.manuel.appskey/com.manuel.appskey.AppsKeyService
adb shell settings put secure accessibility_enabled 1
```

Das Leserecht für das Systemprotokoll kann nur ADB vergeben. In den Bedienungshilfen heißt der Dienst SmartTube-Taste.

SHA-256: `3b5030b7b23db6995df3f4e772660ffb5fc4c76420652671ea972c6c57189865`

## Funktion

Die Taste sendet `KEYCODE_BUTTON_5` (192). Das System verarbeitet sie, bevor ein Bedienungshilfen-Dienst sie sieht, und startet danach keine App. Der Dienst liest die zugehörige WindowManager-Zeile und öffnet `org.smarttube.stable`.

## Build

Quelle unter `app/`. `targetSdk` bleibt 29. Getestet mit OpenJDK 17, Android SDK 34 und Gradle 8.9.

```bash
./gradlew assembleDebug
```

## English

Opens SmartTube from the grid button on the Xiaomi TV Stick 4K remote. The button sends `KEYCODE_BUTTON_5`. The system handles it before an accessibility service can see it, so this app reads that log line and starts SmartTube. It has no network permission.
