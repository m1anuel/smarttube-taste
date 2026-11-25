package com.manuel.appskey;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * The grid button on the Xiaomi TV Stick 4K remote is KEYCODE_BUTTON_5 (192).
 * PhoneWindowManager consumes it before an accessibility filter can see it, so
 * onKeyEvent is not the path that works. The press always logs
 * "keycode=192 " and "KEYCODE_BUTTON_5" through WindowManager. This service
 * follows those lines and opens SmartTube.
 *
 * Lines from before the watcher started are ignored. Otherwise a restart
 * replays the log buffer and opens SmartTube on its own.
 */
public class AppsKeyService extends AccessibilityService implements Runnable {
    private static final String TAG = "AppsKey";
    private static final long DEBOUNCE_MS = 500;
    private static boolean watchStarted;

    private long lastLaunch;
    private long startMs;

    private void openSmartTube() {
        long now = SystemClock.uptimeMillis();
        if (now - lastLaunch < DEBOUNCE_MS) {
            return;
        }
        lastLaunch = now;
        try {
            Intent intent = new Intent();
            intent.setClassName(
                    "org.smarttube.stable",
                    "com.liskovsoft.smartyoutubetv2.tv.ui.main.SplashActivity");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            Log.i(TAG, "open");
        } catch (Exception e) {
            Log.e(TAG, "open fail", e);
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
            setServiceInfo(info);
        }
        if (!watchStarted) {
            watchStarted = true;
            new Thread(this, "appskey-log").start();
            Log.i(TAG, "connected");
        }
    }

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        int code = event.getKeyCode();
        // 192 never arrives here on this stick. 284 is a different key.
        if (code != KeyEvent.KEYCODE_BUTTON_5 && code != KeyEvent.KEYCODE_ALL_APPS) {
            return false;
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            openSmartTube();
        }
        return true;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void run() {
        Log.i(TAG, "watch");
        startMs = System.currentTimeMillis();
        while (true) {
            Process process = null;
            try {
                process = new ProcessBuilder(
                        "/system/bin/logcat",
                        "-v", "epoch",
                        "-b", "main",
                        "-b", "system",
                        "WindowManager:D",
                        "*:S")
                        .redirectErrorStream(false)
                        .start();
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    if (isNewButtonPress(line)) {
                        openSmartTube();
                    }
                }
                BufferedReader err = new BufferedReader(
                        new InputStreamReader(process.getErrorStream()));
                String error = err.readLine();
                if (error != null) {
                    Log.e(TAG, error);
                }
                Thread.sleep(2000);
            } catch (Throwable t) {
                Log.e(TAG, "watch fail", t);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return;
                }
            } finally {
                if (process != null) {
                    process.destroy();
                }
            }
        }
    }

    private boolean isNewButtonPress(String raw) {
        String line = raw.trim();
        int space = line.indexOf(' ');
        if (space <= 0) {
            return false;
        }
        long timestamp;
        try {
            timestamp = Math.round(Double.parseDouble(line.substring(0, space)) * 1000.0);
        } catch (NumberFormatException e) {
            return false;
        }
        if (timestamp < startMs) {
            return false;
        }
        return line.contains("keycode=192 ") || line.contains("KEYCODE_BUTTON_5");
    }
}
