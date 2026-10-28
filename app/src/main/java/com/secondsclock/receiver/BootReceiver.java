package com.secondsclock.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.secondsclock.store.WidgetData;

/** Refreshes widgets after boot or app update. */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctx, Intent intent) {
        WidgetData.updateAll(ctx);
        WidgetData.ensureService(ctx);
    }
}
