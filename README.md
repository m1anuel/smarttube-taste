# SmartTube-Taste

Die linke Taste der Xiaomi-Fernbedienung, deren Symbol ein Viereck aus mehreren kleinen Vierecken ist, öffnet SmartTube. Gedacht für den Xiaomi TV Stick 4K (MiTV-AYFR0, Android TV 11), wenn der Original-Launcher aus ist und diese Taste deshalb nichts mehr tut.

Die App schickt nichts ins Internet. Sie hat kein Netz-Recht. Sie liest auf dem Stick das Systemprotokoll, erkennt genau diese Taste und startet SmartTube.

## Installieren

Einmal pro Stick, vom Rechner aus. `smarttube-taste.apk` liegt in `dist/`.

```bash
adb install dist/smarttube-taste.apk
adb shell pm grant com.manuel.appskey android.permission.READ_LOGS
adb shell settings put secure enabled_accessibility_services com.manuel.appskey/com.manuel.appskey.AppsKeyService
adb shell settings put secure accessibility_enabled 1
```

Das Leserecht fürs Protokoll kann nur ADB vergeben. Der normale Installationsdialog kann das nicht. Unter den Bedienungshilfen heißt der Dienst „SmartTube-Taste“.

Prüfsumme der mitgelieferten APK, SHA-256:

```text
3b5030b7b23db6995df3f4e772660ffb5fc4c76420652671ea972c6c57189865
```

Diese Fassung (versionCode 6) lief auf zwei Sticks dieses Modells. Die echte Taste hat SmartTube geöffnet. Ein selbst gebautes Paket hat eine andere Signatur. Dann zuerst die alte App entfernen und danach neu installieren.

## Prüfen

```bash
adb shell input keyevent 3
```

Der Launcher bleibt vorn. Im Protokoll der App stehen `AppsKey: connected` und `AppsKey: watch`, und nicht `AppsKey: open`.

```bash
adb shell input keyevent 192
```

SmartTube kommt nach vorn, und im Protokoll steht `AppsKey: open`. Danach mit `adb shell input keyevent 3` zurück zum Launcher.

## Rückgängig

```bash
adb shell settings put secure enabled_accessibility_services null
adb shell settings put secure accessibility_enabled 0
adb uninstall com.manuel.appskey
```

Home und die Pfeiltasten bleiben unangetastet. Die App ändert keine Systempakete und setzt den Stick nicht zurück.

## Was die Taste wirklich ist

Auf diesem Stick ist die Taste `KEYCODE_BUTTON_5` (192). In `/vendor/usr/keylayout/Vendor_2717_Product_32b9.kl` steht sie als `key 150 BUTTON_5` und `key 580 BUTTON_5`. Der Code 150 ist die Linux-Taste KEY_WWW.

Sie ist nicht `KEYCODE_ALL_APPS` (284) und nicht der Scancode 216. Scancode 216 liegt in derselben Datei auf `ALL_APPS` und ist eine andere Taste.

Beim Druck schreibt WindowManager zwei Zeilen und startet danach keine App:

```text
WindowManager: interceptKeyTq keycode=192
WindowManager: KEYCODE_BUTTON_5 region = de
```

Die zweite Zeile kommt aus `services.jar`. Für diese Taste gibt es dort kein Ziel mehr, sobald der Xiaomi-Launcher aus ist. Die Pfeiltasten bekommen danach noch `interceptKeyTi` und kommen in der App an. Taste 192 bekommt das nicht.

Ein Bedienungshilfen-Dienst sieht Taste 192 in `onKeyEvent` nicht. Das System verbraucht sie vorher. `adb shell input keyevent` läuft ebenfalls an `onKeyEvent` vorbei, schreibt aber dieselben WindowManager-Zeilen. Damit kann man den Protokoll-Wächter prüfen, nicht `onKeyEvent`.

Die App ist deshalb ein Bedienungshilfen-Dienst, der `logcat` mitliest. `READ_LOGS` ist auf diesem Stick per `pm grant` genug. Der Wächter merkt sich seine Startzeit und ignoriert ältere Zeilen, sonst öffnet ein alter Protokolleintrag SmartTube schon beim Einschalten des Dienstes. Ein Tastendruck schreibt die Zeile doppelt, deshalb wartet die App 500 ms, bevor sie ein zweites Mal öffnet.

SmartTube wird über diese Activity gestartet:

```text
org.smarttube.stable/com.liskovsoft.smartyoutubetv2.tv.ui.main.SplashActivity
```

Die kürzere Klasse `...tv.ui.SplashActivity` gibt es in dieser Fassung nicht. Nach dem Installieren einer neuen APK verbindet sich der Dienst erst wieder, wenn man die Bedienungshilfe aus- und wieder einschaltet.

`targetSdk` bleibt 29. Mit `targetSdk` 30 bricht die Installation auf diesem Stick mit dem Fehler -124 ab, weil `resources.arsc` anders ausgerichtet sein muss.

## Irrwege

Diese Versuche haben die Taste nicht belegt. Sie müssen nicht wiederholt werden.

- `KEYCODE_ALL_APPS` in `onKeyEvent` legen. Die echte Taste sendet 192, in einem Stoß, ohne 284.
- Darauf warten, dass `onKeyEvent` die 192 sieht. Auch ein nachgebauter Tastendruck für KEY_WWW erzeugt nur die WindowManager-Zeilen. Der Fokus bleibt auf dem Launcher, der Dienst bekommt das Ereignis nicht.
- `sendevent` auf das Gerät „Xiaomi RC Keyboard“ (`/dev/input/event*`, Vendor 2717, Produkt 32b9). Das blockiert SELinux, auch wenn die Shell in der Gruppe `input` ist.
- `uinput` über `ioctl` aus `app_process`. Der Aufruf bekommt einen Zeiger und liefert EINVAL.
- `logcat -T` mit Uhrzeit oder Epoch-Sekunden als alleinigen Filter. Der Puffer wird trotzdem noch einmal abgespielt, und SmartTube öffnet sich beim Start. Zusätzlich entstehen zwei Leser, wenn die Startmarke nicht `static` ist.
- Die Epoch-Zeile ungeprüft zerlegen. `logcat -v epoch` ist nur ein Zusatz zum normalen Format. Die Zeit steht rechtsbündig mit führenden Leerzeichen, zum Beispiel `         1795599317.999   557   618 D ...`. Erst `trim()`, dann die erste Zahl als Sekunden lesen, mal 1000, und mit `System.currentTimeMillis()` vom Start des Wächters vergleichen.
- Die beiden Treffer verwechseln. Es zählen `keycode=192 ` mit Leerzeichen dahinter und `KEYCODE_BUTTON_5`. Ein vertauschter Vergleich öffnet SmartTube bei jeder anderen Zeile oder bei keiner.
- Ein breites Register in smali mit dem Ziel eines `div-long` überlappen. Der Verifier beendet den Dienst dann sofort.

## Bauen

Java-Quelle unter `app/`. Getestet mit OpenJDK 17, Android SDK Platform 34, Build-Tools 34.0.0 und Gradle 8.9. `targetSdk` 29 nicht anheben.

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
gradle assembleDebug
```

`local.properties` zeigt auf das eigene SDK und gehört nicht in die Veröffentlichung.

## English

The left remote button with a square-of-squares icon does nothing on a Xiaomi TV Stick 4K (MiTV-AYFR0, codename soul, Android TV 11) once the stock launcher is disabled. On this remote that button is `KEYCODE_BUTTON_5` (192), keylayout `key 150 BUTTON_5` and `key 580 BUTTON_5` in `/vendor/usr/keylayout/Vendor_2717_Product_32b9.kl`. It is not `KEYCODE_ALL_APPS` (284) and not scan code 216.

WindowManager logs `interceptKeyTq keycode=192` and `KEYCODE_BUTTON_5 region = de`, then starts no activity. The string lives in `services.jar`. Accessibility `onKeyEvent` never receives 192, because the policy consumes it first. D-pad keys still get `interceptKeyTi`. `adb shell input keyevent` also bypasses the accessibility filter, but it does write those WindowManager lines, so it is a valid test of the log watcher only.

The working approach is an accessibility service with `READ_LOGS` granted by `pm grant`. It follows `logcat -v epoch -b main -b system WindowManager:D *:S`, ignores lines older than the watcher start, and opens `org.smarttube.stable/com.liskovsoft.smartyoutubetv2.tv.ui.main.SplashActivity` when a new line contains `keycode=192 ` or `KEYCODE_BUTTON_5`. Debounce is 500 ms. Keep one watcher thread per process. Keep `targetSdk` 29. There is no network permission.

Confirmed on two sticks of this model: after the service starts, the launcher stays in front and the log has no `AppsKey: open`. `input keyevent 192` then brings up SmartTube and logs `AppsKey: open`. The physical button does the same.

Do not retry: binding `onKeyEvent` to 284, expecting `onKeyEvent` to see 192, `sendevent` on the Xiaomi RC input node, `uinput` ioctl from `app_process`, or `logcat -T` alone as the backlog filter. Epoch lines are right-aligned and need `trim()` before the timestamp is parsed.
