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

package rkr.simplekeyboard.inputmethod.latin.settings;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.text.InputType;
import android.text.TextUtils;

import rkr.simplekeyboard.inputmethod.R;
import rkr.simplekeyboard.inputmethod.latin.ai.AiPreferences;
import rkr.simplekeyboard.inputmethod.latin.ai.AiProvider;

/**
 * "AI Keyboard" settings sub screen: API keys and model ids for each provider, plus whether the
 * AI toolbar shows above the keyboard. Preference summaries never show a raw API key, only
 * whether one is set.
 */
public final class AiSettingsFragment extends SubScreenFragment {
    @Override
    public void onCreate(final Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.prefs_screen_ai);

        for (final AiProvider provider : AiProvider.values()) {
            if (provider == AiProvider.CUSTOM) {
                continue;
            }
            setupKeyPreference(provider);
            setupModelPreference(provider);
        }
        setupCustomLabelPreference();
        setupCustomEndpointPreference();
        setupKeyPreference(AiProvider.CUSTOM);
        setupModelPreference(AiProvider.CUSTOM);
    }

    private void setupKeyPreference(final AiProvider provider) {
        final EditTextPreference pref = findKeyPreference(provider, "key");
        if (pref == null) {
            return;
        }
        pref.getEditText().setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        refreshKeySummary(pref, pref.getText());
        pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                refreshKeySummary(pref, (String) newValue);
                return true;
            }
        });
    }

    private void setupModelPreference(final AiProvider provider) {
        final EditTextPreference pref = findKeyPreference(provider, "model");
        if (pref == null) {
            return;
        }
        final String defaultModel = provider.defaultModel;
        if (!TextUtils.isEmpty(defaultModel)) {
            pref.getEditText().setHint(defaultModel);
            pref.setDialogMessage(getString(R.string.ai_settings_model_hint, defaultModel));
        }
        refreshModelSummary(pref, provider, pref.getText());
        pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                refreshModelSummary(pref, provider, (String) newValue);
                return true;
            }
        });
    }

    private void setupCustomLabelPreference() {
        final EditTextPreference pref = findKeyPreference(AiProvider.CUSTOM, "label");
        if (pref == null) {
            return;
        }
        refreshPlainSummary(pref, pref.getText(), AiPreferences.displayName(getActivity(), AiProvider.CUSTOM));
        pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                refreshPlainSummary(pref, (String) newValue, AiProvider.CUSTOM.displayName);
                return true;
            }
        });
    }

    private void setupCustomEndpointPreference() {
        final EditTextPreference pref = findKeyPreference(AiProvider.CUSTOM, "endpoint");
        if (pref == null) {
            return;
        }
        pref.getEditText().setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        refreshPlainSummary(pref, pref.getText(), getString(R.string.ai_settings_custom_endpoint));
        pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                refreshPlainSummary(pref, (String) newValue, getString(R.string.ai_settings_custom_endpoint));
                return true;
            }
        });
    }

    private void refreshKeySummary(final EditTextPreference pref, final String value) {
        if (TextUtils.isEmpty(value)) {
            pref.setSummary(R.string.ai_settings_api_key_summary_unset);
            return;
        }
        final int visibleChars = Math.min(4, value.length());
        final String tail = value.substring(value.length() - visibleChars);
        pref.setSummary(getString(R.string.ai_settings_api_key_summary_set, tail));
    }

    private void refreshModelSummary(final EditTextPreference pref, final AiProvider provider,
                                      final String value) {
        final String effective = TextUtils.isEmpty(value) ? provider.defaultModel : value;
        pref.setSummary(effective);
    }

    private void refreshPlainSummary(final EditTextPreference pref, final String value,
                                      final String placeholder) {
        pref.setSummary(TextUtils.isEmpty(value) ? placeholder : value);
    }

    private EditTextPreference findKeyPreference(final AiProvider provider, final String suffix) {
        return (EditTextPreference) findPreference(AiPreferences.prefKeyFor(provider, suffix));
    }
}
