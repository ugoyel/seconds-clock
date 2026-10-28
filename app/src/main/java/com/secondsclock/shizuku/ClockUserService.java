package com.secondsclock.shizuku;

import com.secondsclock.IShizukuClock;

/** Runs inside a Shizuku-spawned process (shell UID) and flips the hidden clock-seconds key. */
public class ClockUserService extends IShizukuClock.Stub {

    @Override
    public void setClockSeconds(int value) {
        try {
            Runtime.getRuntime().exec(new String[] {
                    "settings", "put", "secure", "clock_seconds", String.valueOf(value)
            });
        } catch (Throwable ignored) {
            // readback in the app decides the button state
        }
    }
}
