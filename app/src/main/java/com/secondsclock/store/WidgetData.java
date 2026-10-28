package com.secondsclock.store;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.secondsclock.MainActivity;
import com.secondsclock.R;
import com.secondsclock.service.ClockTickService;
import com.secondsclock.widget.Clock1x1;
import com.secondsclock.widget.Clock1x2;
import com.secondsclock.widget.Clock2x2;
import com.secondsclock.widget.Clock3x3;

import java.util.Calendar;

/** Builds RemoteViews for every widget/face combination and drives updates. */
public final class WidgetData {

    private static final String[] NAMES = { "Clock1x1", "Clock1x2", "Clock2x2", "Clock3x3" };

    private static final int[] DIGITAL = {
            R.layout.widget_1x1_digital,
            R.layout.widget_1x2_digital,
            R.layout.widget_2x2_digital,
            R.layout.widget_3x3_digital
    };

    private static final int[] ANALOG = {
            R.layout.widget_1x1_analog,
            R.layout.widget_1x2_analog,
            R.layout.widget_2x2_analog,
            R.layout.widget_3x3_analog
    };

    private WidgetData() {
    }

    private static int indexOfClass(String cls) {
        for (int i = 0; i < NAMES.length; i++) {
            if (cls.endsWith(NAMES[i])) {
                return i;
            }
        }
        return -1;
    }

    public static RemoteViews build(Context ctx, String providerClass, String face, int mode) {
        int idx = indexOfClass(providerClass);
        if (idx < 0) {
            return null;
        }
        boolean analog = Store.FACE_ANALOG.equals(face);
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), analog ? ANALOG[idx] : DIGITAL[idx]);

        PendingIntent pi = PendingIntent.getActivity(ctx, 0, new Intent(ctx, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        rv.setOnClickPendingIntent(R.id.widget_root, pi);

        applyFormats(rv, providerClass, analog, mode);
        if (analog) {
            applyRotations(rv);
        }
        return rv;
    }

    /** RemoteViews update: app opens on tap. */
    private static void applyFormats(RemoteViews rv, String providerClass, boolean analog, int mode) {
        boolean big = providerClass.endsWith("Clock3x3");
        String main24 = (!analog && big) ? "HH:mm" : "HH:mm:ss";
        String main12 = (!analog && big) ? "hh:mm" : "h:mm:ss";
        String main12s = (!analog && big) ? "hh:mm" : "hh:mm:ss";

        String us12, us24, small12, small24;
        if (mode == Store.MODE_24) {
            us12 = main24;
            us24 = main24;
            small12 = "HH:mm:ss";
            small24 = "HH:mm:ss";
        } else if (mode == Store.MODE_12) {
            us12 = main12s;
            us24 = main12s;
            small12 = "hh:mm:ss";
            small24 = "hh:mm:ss";
        } else {
            us12 = main12;
            us24 = main24;
            small12 = "h:mm:ss";
            small24 = "HH:mm:ss";
        }
        setClock(rv, R.id.time_main, us12, us24);
        setClock(rv, R.id.time_small, small12, small24);
        setClock(rv, R.id.sec_line, ":ss", ":ss");
        setClock(rv, R.id.date_line, "EEE, MMM d", "EEE, MMM d");
    }

    private static void setClock(RemoteViews rv, int id, String f12, String f24) {
        rv.setCharSequence(id, "setFormat12Hour", f12);
        rv.setCharSequence(id, "setFormat24Hour", f24);
    }

    /** Rotation angles need updating only on analog faces. */
    private static void applyRotations(RemoteViews rv) {
        Calendar c = Calendar.getInstance();
        float s = c.get(Calendar.SECOND) + c.get(Calendar.MILLISECOND) / 1000f;
        float m = c.get(Calendar.MINUTE) + s / 60f;
        float h = c.get(Calendar.HOUR_OF_DAY) % 12 + m / 60f;
        rv.setFloat(R.id.second_hand, "setRotation", s * 6f);
        rv.setFloat(R.id.minute_hand, "setRotation", m * 6f);
        rv.setFloat(R.id.hour_hand, "setRotation", h * 30f);
    }

    public static void update(Context ctx, int id) {
        AppWidgetManager am = AppWidgetManager.getInstance(ctx);
        AppWidgetProviderInfo info = am.getAppWidgetInfo(id);
        if (info == null) {
            return;
        }
        RemoteViews rv = build(ctx, info.provider.getClassName(),
                Store.face(ctx, id), Store.mode(ctx, id));
        if (rv != null) {
            am.updateAppWidget(id, rv);
        }
    }

    public static void updateAll(Context ctx) {
        AppWidgetManager am = AppWidgetManager.getInstance(ctx);
        for (Class<?> cls : classes()) {
            ComponentName cn = new ComponentName(ctx, cls);
            for (int id : am.getAppWidgetIds(cn)) {
                update(ctx, id);
            }
        }
    }

    /** Redraws every analog widget right now (fresh hand angles). */
    public static void updateAnalogNow(Context ctx) {
        AppWidgetManager am = AppWidgetManager.getInstance(ctx);
        for (Class<?> cls : classes()) {
            ComponentName cn = new ComponentName(ctx, cls);
            for (int id : am.getAppWidgetIds(cn)) {
                if (!Store.isAnalog(ctx, id)) {
                    continue;
                }
                RemoteViews rv = build(ctx, cn.getClassName(), Store.FACE_ANALOG, Store.mode(ctx, id));
                if (rv != null) {
                    am.updateAppWidget(id, rv);
                }
            }
        }
    }

    public static boolean hasAnalogWidgets(Context ctx) {
        AppWidgetManager am = AppWidgetManager.getInstance(ctx);
        for (Class<?> cls : classes()) {
            ComponentName cn = new ComponentName(ctx, cls);
            for (int id : am.getAppWidgetIds(cn)) {
                if (Store.isAnalog(ctx, id)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void ensureService(Context ctx) {
        if (hasAnalogWidgets(ctx) || Store.notifClock(ctx)) {
            ClockTickService.start(ctx);
        }
    }

    private static Class<?>[] classes() {
        return new Class<?>[] { Clock1x1.class, Clock1x2.class, Clock2x2.class, Clock3x3.class };
    }
}
