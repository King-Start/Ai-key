/*
 * Copyright (C) 2026 Raimondas Rimkus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rkr.simplekeyboard.inputmethod.latin.ai;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import rkr.simplekeyboard.inputmethod.compat.PreferenceManagerCompat;

/**
 * Reads and writes AI feature settings (which provider is active, API keys, model ids). Kept
 * separate from {@link rkr.simplekeyboard.inputmethod.latin.settings.Settings} on purpose: that
 * class feeds the performance-sensitive per-keystroke {@code SettingsValues} snapshot, and these
 * values are only ever read when the AI dialog is opened, so there's no reason to couple them.
 *
 * Keys are stored in the same private, per-app SharedPreferences used by the rest of the keyboard
 * (device-protected storage, not synced or shared with other apps). They are not encrypted at
 * rest beyond normal Android app-sandboxing.
 */
public final class AiPreferences {
    private static final String KEY_SELECTED_PROVIDER = "pref_ai_selected_provider";
    private static final String KEY_SHOW_TOOLBAR = "pref_ai_show_toolbar";
    private static final String KEY_CUSTOM_LABEL = "pref_ai_custom_label";
    private static final String KEY_CUSTOM_ENDPOINT = "pref_ai_custom_endpoint";
    private static final String DEFAULT_CUSTOM_LABEL = "Custom";

    private AiPreferences() {
        // Not instantiable.
    }

    private static SharedPreferences prefs(final Context context) {
        return PreferenceManagerCompat.getDeviceSharedPreferences(context);
    }

    public static boolean getShowToolbar(final Context context) {
        return prefs(context).getBoolean(KEY_SHOW_TOOLBAR, true);
    }

    public static AiProvider getSelectedProvider(final Context context) {
        final String id = prefs(context).getString(KEY_SELECTED_PROVIDER, AiProvider.OPENAI.id);
        return AiProvider.fromId(id);
    }

    public static void setSelectedProvider(final Context context, final AiProvider provider) {
        prefs(context).edit().putString(KEY_SELECTED_PROVIDER, provider.id).apply();
    }

    public static String getApiKey(final Context context, final AiProvider provider) {
        return prefs(context).getString(prefKeyFor(provider, "key"), "");
    }

    public static String getModel(final Context context, final AiProvider provider) {
        final String stored = prefs(context).getString(prefKeyFor(provider, "model"), "");
        return TextUtils.isEmpty(stored) ? provider.defaultModel : stored;
    }

    public static String getCustomLabel(final Context context) {
        final String stored = prefs(context).getString(KEY_CUSTOM_LABEL, "");
        return TextUtils.isEmpty(stored) ? DEFAULT_CUSTOM_LABEL : stored;
    }

    public static String getCustomEndpoint(final Context context) {
        return prefs(context).getString(KEY_CUSTOM_ENDPOINT, "");
    }

    /** The endpoint {@link AiClient} should call for this provider. */
    public static String getEffectiveEndpoint(final Context context, final AiProvider provider) {
        if (provider == AiProvider.CUSTOM) {
            return getCustomEndpoint(context);
        }
        return provider.defaultEndpoint;
    }

    /** Whether enough info is present (API key, and for CUSTOM an endpoint) to attempt a call. */
    public static boolean isConfigured(final Context context, final AiProvider provider) {
        final String key = getApiKey(context, provider);
        if (TextUtils.isEmpty(key)) {
            return false;
        }
        if (provider == AiProvider.CUSTOM) {
            return !TextUtils.isEmpty(getCustomEndpoint(context));
        }
        return true;
    }

    public static String displayName(final Context context, final AiProvider provider) {
        return provider == AiProvider.CUSTOM ? getCustomLabel(context) : provider.displayName;
    }

    /** Builds the SharedPreferences key for a given provider + field (e.g. "openai" + "key"). */
    public static String prefKeyFor(final AiProvider provider, final String suffix) {
        return "pref_ai_" + provider.id + "_" + suffix;
    }
}
