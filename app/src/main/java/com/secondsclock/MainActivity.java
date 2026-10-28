package com.secondsclock;

import android.Manifest;
import android.appwidget.AppWidgetManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.secondsclock.service.ClockTickService;
import com.secondsclock.store.Store;
import com.secondsclock.store.WidgetData;

public class MainActivity extends android.app.Activity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { Manifest.permission.POST_NOTIFICATIONS }, 1);
        }
        setContentView(R.layout.activity_main);

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
