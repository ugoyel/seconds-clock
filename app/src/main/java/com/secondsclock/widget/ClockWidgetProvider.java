package com.secondsclock.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;

import com.secondsclock.store.Store;
import com.secondsclock.store.WidgetData;

/** Base provider: renders the chosen face and keeps the seconds service alive. */
public abstract class ClockWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context ctx, AppWidgetManager awm, int[] ids) {
        for (int id : ids) {
            WidgetData.update(ctx, id);
        }
        WidgetData.ensureService(ctx);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context ctx, AppWidgetManager awm, int id, Bundle options) {
        WidgetData.update(ctx, id);
    }

    @Override
    public void onDeleted(Context ctx, int[] ids) {
        for (int id : ids) {
            Store.clearFor(ctx, id);
        }
    }
}
