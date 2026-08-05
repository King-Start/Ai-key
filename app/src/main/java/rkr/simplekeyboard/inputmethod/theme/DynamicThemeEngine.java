package rkr.simplekeyboard.inputmethod.theme;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import rkr.simplekeyboard.inputmethod.latin.settings.Settings;

public class DynamicThemeEngine {

    private static final String KEY_KEYBOARD_COLOR = "pref_keyboard_color";

    public static int getKeyboardColor(Context context) {
        SharedPreferences prefs = Settings.getDeviceSharedPreferences(context);
        return prefs.getInt(KEY_KEYBOARD_COLOR, Color.parseColor("#263238")); // Default dark
    }

    public static void setKeyboardColor(Context context, int color) {
        SharedPreferences prefs = Settings.getDeviceSharedPreferences(context);
        prefs.edit().putInt(KEY_KEYBOARD_COLOR, color).apply();
    }

    // Material You dynamic color (Android 12+)
    public static boolean isDynamicColorSupported() {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S;
    }
}