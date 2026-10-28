package com.secondsclock;

import android.Manifest;
import android.appwidget.AppWidgetManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import com.secondsclock.service.ClockTickService;
import com.secondsclock.shizuku.ClockUserService;
import com.secondsclock.store.Store;
import com.secondsclock.store.WidgetData;

import rikka.shizuku.Shizuku;

public class MainActivity extends android.app.Activity {

    private static final int REQ_SHIZUKU = 9888;
    private static final String SHIZUKU_PKG = "moe.shizuku.privileged.api";
    private static final String SECURE_KEY = "clock_seconds";

    private Button statusButton;
    private Boolean pendingEnable;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private IShizukuClock clockService;

    private final Shizuku.UserServiceArgs userServiceArgs = new Shizuku.UserServiceArgs(
            new ComponentName(getPackageName(), ClockUserService.class.getCanonicalName()))
            .processNameSuffix("sec_clock")
            .version(1);

    private final android.content.ServiceConnection userServiceConnection =
            new android.content.ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, android.os.IBinder binder) {
                    clockService = IShizukuClock.Stub.asInterface(binder);
                    Boolean run = pendingEnable;
                    pendingEnable = null;
                    if (run != null) {
                        runClock(run);
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    clockService = null;
                }
            };

    private final Shizuku.OnRequestPermissionResultListener shizukuListener =
            new Shizuku.OnRequestPermissionResultListener() {
                @Override
                public void onRequestPermissionResult(int requestCode, int grantResult) {
                    if (requestCode != REQ_SHIZUKU) {
                        return;
                    }
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        Boolean run = pendingEnable;
                        pendingEnable = null;
                        if (run != null) {
                            bindUserClock(run);
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Shizuku permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
            };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (android.os.Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { Manifest.permission.POST_NOTIFICATIONS }, 1);
        }
        setContentView(R.layout.activity_main);
        Shizuku.addRequestPermissionResultListener(shizukuListener);

        RadioGroup face = findViewById(R.id.rg_default_face);
        RadioGroup time = findViewById(R.id.rg_default_time);
        if (Store.FACE_ANALOG.equals(Store.defaultFace(this))) {
            face.check(R.id.rb_face_analog);
        } else {
            face.check(R.id.rb_face_digital);
        }
        int m = Store.defaultMode(this);
        time.check(m == Store.MODE_24 ? R.id.rb_time_24 : m == Store.MODE_12 ? R.id.rb_time_12 : R.id.rb_time_auto);
        face.setOnCheckedChangeListener((g, checkId) -> Store.setDefaultFace(this,
                checkId == R.id.rb_face_analog ? Store.FACE_ANALOG : Store.FACE_DIGITAL));
        time.setOnCheckedChangeListener((g, checkId) -> {
            int mode = checkId == R.id.rb_time_24 ? Store.MODE_24
                    : checkId == R.id.rb_time_12 ? Store.MODE_12 : Store.MODE_AUTO;
            Store.setDefaultMode(this, mode);
            WidgetData.updateAll(this);
        });

        bindAddButton(R.id.b_add_1x1, 0, R.string.add_1x1);
        bindAddButton(R.id.b_add_1x2, 1, R.string.add_1x2);
        bindAddButton(R.id.b_add_2x2, 2, R.string.add_2x2);
        bindAddButton(R.id.b_add_3x3, 3, R.string.add_3x3);

        Button battery = findViewById(R.id.b_battery);
        battery.setText(R.string.btn_battery);
        battery.setOnClickListener(v -> openBatteryOptimizations());
        Button autostart = findViewById(R.id.b_autostart);
        autostart.setText(R.string.btn_autostart);
        autostart.setOnClickListener(v -> openAutoStart());
        Button refresh = findViewById(R.id.b_refresh);
        refresh.setText(R.string.btn_refresh);
        refresh.setOnClickListener(v -> {
            WidgetData.updateAll(this);
            ClockTickService.start(this);
            Toast.makeText(this, R.string.toast_refreshed, Toast.LENGTH_SHORT).show();
        });

        Switch notifClock = findViewById(R.id.sw_notif_clock);
        notifClock.setChecked(Store.notifClock(this));
        notifClock.setOnCheckedChangeListener((btn, on) -> {
            Store.setNotifClock(this, on);
            if (on) {
                ClockTickService.start(this);
                Toast.makeText(this, R.string.toast_fgs_start, Toast.LENGTH_SHORT).show();
            } else {
                ClockTickService.stopIfUnused(this);
            }
        });

        statusButton = findViewById(R.id.b_status_seconds);
        statusButton.setOnClickListener(v -> onStatusSeconds());
        refreshStatusButton();
    }

    @Override
    protected void onDestroy() {
        Shizuku.removeRequestPermissionResultListener(shizukuListener);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatusButton();
    }

    private void bindAddButton(int buttonId, final int idx, int label) {
        Button b = findViewById(buttonId);
        b.setText(label);
        b.setOnClickListener(v -> {
            AppWidgetManager am = AppWidgetManager.getInstance(this);
            Class<?> cls = idx == 0 ? com.secondsclock.widget.Clock1x1.class
                    : idx == 1 ? com.secondsclock.widget.Clock1x2.class
                    : idx == 2 ? com.secondsclock.widget.Clock2x2.class
                    : com.secondsclock.widget.Clock3x3.class;
            if (!am.isRequestPinAppWidgetSupported()
                    || !am.requestPinAppWidget(new ComponentName(this, cls), null, null)) {
                Toast.makeText(this, R.string.toast_pin_unsupported, Toast.LENGTH_LONG).show();
            }
        });
    }

    // ---------------- status-bar seconds ----------------

    private boolean appHasWriteSecure() {
        return checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private int readSecure() {
        try {
            return Settings.Secure.getInt(getContentResolver(), SECURE_KEY, 0);
        } catch (Exception e) {
            return 0;
        }
    }

    private void refreshStatusButton() {
        if (statusButton == null) {
            return;
        }
        boolean on = readSecure() == 1;
        statusButton.setText(on ? R.string.status_seconds_disable : R.string.status_seconds_enable);
    }

    /** App runs itself when possible, otherwise it walks the user through the on-device helper. */
    private void onStatusSeconds() {
        boolean wantOn = readSecure() != 1;
        if (appHasWriteSecure()) {
            applyDirect(wantOn);
            refreshStatusButton();
            return;
        }
        applyViaShizuku(wantOn);
    }

    private void applyDirect(boolean enable) {
        try {
            Settings.Secure.putInt(getContentResolver(), SECURE_KEY, 0);
            if (enable) {
                Settings.Secure.putInt(getContentResolver(), SECURE_KEY, 1);
            }
            Toast.makeText(this, R.string.toast_status_applied, Toast.LENGTH_LONG).show();
        } catch (SecurityException e) {
            applyViaShizuku(enable);
        }
    }

    private void applyViaShizuku(boolean enable) {
        pendingEnable = enable;
        if (!Shizuku.pingBinder()) {
            showShizukuGuide();
            return;
        }
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            try {
                Shizuku.requestPermission(REQ_SHIZUKU);
            } catch (Throwable t) {
                showShizukuGuide();
                return;
            }
            return; // continues in the permission-result listener
        }
        if (clockService != null) {
            Boolean run = pendingEnable;
            pendingEnable = null;
            runClock(run);
        } else {
            try {
                Shizuku.bindUserService(userServiceArgs, userServiceConnection);
            } catch (Throwable t) {
                showShizukuGuide();
            }
        }
    }

    private void bindUserClock(boolean enable) {
        pendingEnable = enable;
        if (clockService != null) {
            Boolean run = pendingEnable;
            pendingEnable = null;
            runClock(run);
            return;
        }
        try {
            Shizuku.bindUserService(userServiceArgs, userServiceConnection);
        } catch (Throwable t) {
            showShizukuGuide();
        }
    }

    /** Flips 0 then 1 so SystemUI notices the change even on stubborn builds. */
    private void runClock(boolean enable) {
        try {
            clockService.setClockSeconds(0);
            if (enable) {
                clockService.setClockSeconds(1);
            }
        } catch (Throwable t) {
            clockService = null;
            showShizukuGuide();
            return;
        }
        Toast.makeText(this, R.string.toast_status_applied, Toast.LENGTH_LONG).show();
        ui.postDelayed(this::refreshStatusButton, 700);
    }

    private void showShizukuGuide() {
        String pkg = SHIZUKU_PKG;
        Intent launch = getPackageManager().getLaunchIntentForPackage(pkg);
        new android.app.AlertDialog.Builder(this)
                .setTitle(launch != null ? R.string.shizuku_start_needed_title : R.string.shizuku_missing_title)
                .setMessage(launch != null ? R.string.shizuku_start_needed_text : R.string.shizuku_missing_text)
                .setPositiveButton(launch != null ? R.string.btn_open_shizuku : R.string.btn_install_shizuku,
                        (d, w) -> openShizuku())
                .setNeutralButton(R.string.btn_copy_adb, (d, w) -> copyAdbCommand())
                .setNegativeButton(R.string.action_close, null)
                .show();
    }

    private void openShizuku() {
        String pkg = SHIZUKU_PKG;
        Intent launch = getPackageManager().getLaunchIntentForPackage(pkg);
        try {
            startActivity(launch != null ? launch
                    : new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pkg)));
        } catch (ActivityNotFoundException e) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=" + pkg)));
            } catch (Exception ignore) {
                Toast.makeText(this, "Play Store not found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void copyAdbCommand() {
        final String cmd = "adb shell pm grant com.secondsclock android.permission.WRITE_SECURE_SETTINGS";
        android.content.ClipboardManager cm =
                (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        cm.setPrimaryClip(android.content.ClipData.newPlainText("adb grant", cmd));
        Toast.makeText(this, R.string.toast_copied, Toast.LENGTH_SHORT).show();
    }

    // ---------------- battery / autostart helpers ----------------

    private void openBatteryOptimizations() {
        String pkg = getPackageName();
        android.os.PowerManager pm = (android.os.PowerManager) getSystemService(POWER_SERVICE);
        try {
            if (!pm.isIgnoringBatteryOptimizations(pkg)) {
                Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:" + pkg));
                startActivity(i);
            } else {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            }
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Battery settings not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void openAutoStart() {
        Intent[] candidates = new Intent[] {
                new Intent().setClassName("com.coloros.safecenter",
                        "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
                new Intent().setClassName("com.coloros.safecenter",
                        "com.coloros.safecenter.startup.StartupAppListActivity"),
                new Intent().setClassName("com.oppo.safe",
                        "com.oppo.safe.permission.startup.StartupAppListActivity"),
                new Intent().setClassName("com.samsung.android.sm",
                        "com.samsung.android.sm.ui.battery.BatteryActivity"),
                new Intent().setClassName("com.samsung.android.lool",
                        "com.samsung.android.sm.battery.ui.BatteryActivity")
        };
        for (Intent i : candidates) {
            try {
                startActivity(i);
                return;
            } catch (Exception ignore) {
                // try next candidate
            }
        }
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName())));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "App info not found", Toast.LENGTH_SHORT).show();
        }
    }
}
