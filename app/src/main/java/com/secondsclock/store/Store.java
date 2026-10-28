package com.secondsclock.store;

import android.content.Context;
import android.content.SharedPreferences;

/** Per-widget face/time-format options plus app-wide defaults. */
public final class Store {

    public static final int MODE_AUTO = 0;
    public static final int MODE_24 = 1;
    public static final int MODE_12 = 2;

    public static final String FACE_ANALOG = "analog";
    public static final String FACE_DIGITAL = "digital";

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("clock_store", Context.MODE_PRIVATE);
    }

    public static void setDefaultFace(Context c, String face) {
        sp(c).edit().putString("default_face", face).apply();
    }

    public static String defaultFace(Context c) {
        return sp(c).getString("default_face", FACE_DIGITAL);
    }

    public static void setDefaultMode(Context c, int mode) {
        sp(c).edit().putInt("default_mode", mode).apply();
    }

    public static int defaultMode(Context c) {
        return sp(c).getInt("default_mode", MODE_AUTO);
    }

    public static String face(Context c, int id) {
        return sp(c).getString("face_" + id, defaultFace(c));
    }

    public static void setFace(Context c, int id, String face) {
        sp(c).edit().putString("face_" + id, face).apply();
    }

    public static int mode(Context c, int id) {
        return sp(c).getInt("mode_" + id, defaultMode(c));
    }

    public static void setMode(Context c, int id, int mode) {
        sp(c).edit().putInt("mode_" + id, mode).apply();
    }

    public static void clearFor(Context c, int id) {
        sp(c).edit().remove("face_" + id).remove("mode_" + id).apply();
    }

    public static boolean isAnalog(Context c, int id) {
        return FACE_ANALOG.equals(face(c, id));
    }
}
