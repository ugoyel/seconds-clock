package com.secondsclock.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

import com.secondsclock.R;
import com.secondsclock.store.WidgetData;

/**
 * Foreground service that ticks every second, rotating the hands of all
 * analog clock widgets. Pauses work while the screen is off.
 */
public class ClockTickService extends Service {

    private static final String CHANNEL = "clock_widget_service";
    private static final int NOTIF_ID = 1001;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private BroadcastReceiver screenReceiver;
    private boolean ticking = false;

    public static void start(Context c) {
        Intent i = new Intent(c, ClockTickService.class);
        if (Build.VERSION.SDK_INT >= 26) {
            c.startForegroundService(i);
        } else {
            c.startService(i);
        }
    }

    /** Starts the ticker only when at least one analog widget needs it. */
    public static void ensure(Context c) {
        if (WidgetData.hasAnalogWidgets(c)) {
            start(c);
        }
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!WidgetData.hasAnalogWidgets(ClockTickService.this)) {
                stopSelf();
                return;
            }
            if (isScreenOn()) {
                WidgetData.updateAnalogNow(ClockTickService.this);
            }
            long delay = 1000 - (System.currentTimeMillis() % 1000) + 15;
            handler.postDelayed(this, delay);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createChannel();
        startForegroundCompat();
        registerScreenReceiver();
        if (!ticking) {
            ticking = true;
            handler.postDelayed(tick, 100);
        }
        return START_STICKY;
    }

    private void startForegroundCompat() {
        Notification n = new Notification.Builder(this, CHANNEL)
                .setContentTitle(getString(R.string.notif_title))
                .setContentText(getString(R.string.notif_text))
                .setSmallIcon(R.drawable.ic_stat_clock)
                .setOngoing(true)
                .build();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIF_ID, n);
        }
    }

    private boolean isScreenOn() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        return pm.isInteractive();
    }

    private void registerScreenReceiver() {
        if (screenReceiver != null) {
            return;
        }
        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                String a = i.getAction();
                if (Intent.ACTION_SCREEN_OFF.equals(a)) {
                    handler.removeCallbacks(tick);
                } else if (Intent.ACTION_SCREEN_ON.equals(a)) {
                    handler.removeCallbacks(tick);
                    handler.postDelayed(tick, 100);
                }
            }
        };
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_SCREEN_ON);
        f.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenReceiver, f);
    }

    private void createChannel() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel ch = new NotificationChannel(CHANNEL,
                getString(R.string.notif_channel), NotificationManager.IMPORTANCE_MIN);
        ch.setDescription(getString(R.string.notif_channel_desc));
        ch.setShowBadge(false);
        nm.createNotificationChannel(ch);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(tick);
        ticking = false;
        if (screenReceiver != null) {
            unregisterReceiver(screenReceiver);
            screenReceiver = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
