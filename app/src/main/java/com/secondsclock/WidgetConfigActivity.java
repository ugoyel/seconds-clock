package com.secondsclock;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;

import com.secondsclock.service.ClockTickService;
import com.secondsclock.store.Store;
import com.secondsclock.store.WidgetData;

import java.util.concurrent.atomic.AtomicInteger;

/** Per-widget configuration: face + time format, shown when a widget is added. */
public class WidgetConfigActivity extends android.app.Activity {

    private final AtomicInteger widgetId = new AtomicInteger(AppWidgetManager.INVALID_APPWIDGET_ID);

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        int id = getIntent() != null
                ? getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID)
                : AppWidgetManager.INVALID_APPWIDGET_ID;
        widgetId.set(id);
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }
        setResult(RESULT_CANCELED);
        setContentView(R.layout.activity_config);

        String face = Store.face(this, id);
        int mode = Store.mode(this, id);

        RadioGroup rgFace = findViewById(R.id.rg_face);
        RadioGroup rgTime = findViewById(R.id.rg_time);
        rgFace.check(Store.FACE_ANALOG.equals(face) ? R.id.rb_analog : R.id.rb_digital);
        rgTime.check(mode == Store.MODE_24 ? R.id.rb_24 : mode == Store.MODE_12 ? R.id.rb_12 : R.id.rb_auto);

        findViewById(R.id.b_cancel).setOnClickListener(v -> finish());
        findViewById(R.id.b_save).setOnClickListener(v -> save(rgFace.getCheckedRadioButtonId(),
                rgTime.getCheckedRadioButtonId()));
    }

    private void save(int faceCheck, int timeCheck) {
        int id = widgetId.get();
        Store.setFace(this, id, faceCheck == R.id.rb_analog ? Store.FACE_ANALOG : Store.FACE_DIGITAL);
        Store.setMode(this, id, timeCheck == R.id.rb_24 ? Store.MODE_24
                : timeCheck == R.id.rb_12 ? Store.MODE_12 : Store.MODE_AUTO);
        WidgetData.update(this, id);
        ClockTickService.ensure(this);
        Intent out = new Intent();
        out.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        setResult(RESULT_OK, out);
        finish();
    }
}
