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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import rkr.simplekeyboard.inputmethod.R;
import rkr.simplekeyboard.inputmethod.latin.settings.AiSettingsFragment;
import rkr.simplekeyboard.inputmethod.latin.settings.SettingsActivity;

/**
 * AI panel with Gboard-like quick actions:
 * Jawab, Perbaiki, Lanjutkan, Ringkas, Formal, Santai.
 */
public final class AiPanelController {

    public interface TextCommitter {
        void commitText(CharSequence text);
    }

    private final Context mContext;
    private final TextCommitter mCommitter;

    private View mToolbar;
    private View mPanel;
    private Spinner mProviderSpinner;
    private EditText mPromptInput;
    private ProgressBar mProgress;
    private TextView mStatusText;
    private Button mSendButton;
    private View mResultGroup;
    private TextView mResultText;
    private AiProvider[] mProviders;
    private boolean mBound;

    public AiPanelController(final Context context, final TextCommitter committer) {
        mContext = context;
        mCommitter = committer;
    }

    public void attach(final View rootView) {
        mBound = false;
        mToolbar = rootView.findViewById(R.id.keyboard_toolbar);
        mPanel = rootView.findViewById(R.id.ai_panel_inline);
        if (mToolbar != null) {
            mToolbar.setVisibility(AiPreferences.getShowToolbar(mContext) ? View.VISIBLE : View.GONE);
        }
        if (mPanel == null) {
            return;
        }
        mPanel.setVisibility(View.GONE);

        mProviderSpinner = mPanel.findViewById(R.id.ai_provider_spinner);
        mPromptInput = mPanel.findViewById(R.id.ai_prompt_input);
        mProgress = mPanel.findViewById(R.id.ai_progress);
        mStatusText = mPanel.findViewById(R.id.ai_status_text);
        mSendButton = mPanel.findViewById(R.id.ai_send_button);
        mResultGroup = mPanel.findViewById(R.id.ai_result_group);
        mResultText = mPanel.findViewById(R.id.ai_result_text);

        final Button insertButton = mPanel.findViewById(R.id.ai_insert_button);
        final Button copyButton = mPanel.findViewById(R.id.ai_copy_button);
        final Button collapseButton = mPanel.findViewById(R.id.ai_panel_collapse_button);

        // Quick action buttons
        final Button btnJawab = mPanel.findViewById(R.id.ai_btn_jawab);
        final Button btnPerbaiki = mPanel.findViewById(R.id.ai_btn_perbaiki);
        final Button btnLanjutkan = mPanel.findViewById(R.id.ai_btn_lanjutkan);
        final Button btnRingkas = mPanel.findViewById(R.id.ai_btn_ringkas);
        final Button btnFormal = mPanel.findViewById(R.id.ai_btn_formal);
        final Button btnSantai = mPanel.findViewById(R.id.ai_btn_santai);

        if (mProviderSpinner == null || mPromptInput == null || mProgress == null
                || mStatusText == null || mSendButton == null || mResultGroup == null
                || mResultText == null || insertButton == null || copyButton == null) {
            return;
        }

        setUpProviderSpinner();
        setUpSendButton();

        // Quick actions
        if (btnJawab != null) {
            btnJawab.setOnClickListener(v -> runQuickAction(
                    "Jawab pertanyaan atau soal berikut dengan jelas dan lengkap:\n\n"));
        }
        if (btnPerbaiki != null) {
            btnPerbaiki.setOnClickListener(v -> runQuickAction(
                    "Perbaiki ejaan, tata bahasa, dan kejelasan teks berikut. " +
                    "Kembalikan hanya teks yang sudah diperbaiki, tanpa penjelasan tambahan:\n\n"));
        }
        if (btnLanjutkan != null) {
            btnLanjutkan.setOnClickListener(v -> runQuickAction(
                    "Lanjutkan tulisan berikut secara natural dan koheren:\n\n"));
        }
        if (btnRingkas != null) {
            btnRingkas.setOnClickListener(v -> runQuickAction(
                    "Ringkas teks berikut agar lebih singkat dan jelas. " +
                    "Kembalikan hanya hasil ringkasannya:\n\n"));
        }
        if (btnFormal != null) {
            btnFormal.setOnClickListener(v -> runQuickAction(
                    "Ubah teks berikut menjadi lebih formal dan sopan. " +
                    "Kembalikan hanya teks yang sudah diubah:\n\n"));
        }
        if (btnSantai != null) {
            btnSantai.setOnClickListener(v -> runQuickAction(
                    "Ubah teks berikut menjadi lebih santai dan natural. " +
                    "Kembalikan hanya teks yang sudah diubah:\n\n"));
        }

        insertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                mCommitter.commitText(mResultText.getText());
                collapse();
            }
        });

        copyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final ClipboardManager clipboard =
                        (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("AI result", mResultText.getText()));
                    Toast.makeText(mContext, R.string.ai_copied_toast, Toast.LENGTH_SHORT).show();
                }
            }
        });

        if (collapseButton != null) {
            collapseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    collapse();
                }
            });
        }

        mBound = true;
    }

    /** Jalankan aksi cepat (Jawab / Perbaiki / dll). */
    private void runQuickAction(final String prefix) {
        final String userText = mPromptInput.getText().toString().trim();
        if (TextUtils.isEmpty(userText)) {
            showStatus("Ketik soal atau teks dulu di kotak atas", null);
            return;
        }
        // Isi prompt dengan instruksi + teks user, lalu kirim
        mPromptInput.setText(prefix + userText);
        mSendButton.performClick();
    }

    private void setUpProviderSpinner() {
        mProviders = AiProvider.values();
        final String[] labels = new String[mProviders.length];
        for (int i = 0; i < mProviders.length; i++) {
            labels[i] = AiPreferences.displayName(mContext, mProviders[i]);
        }
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(
                mContext, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mProviderSpinner.setAdapter(adapter);
        mProviderSpinner.setSelection(indexOf(mProviders, AiPreferences.getSelectedProvider(mContext)));

        mProviderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> parent, final View v, final int position,
                                        final long id) {
                AiPreferences.setSelectedProvider(mContext, mProviders[position]);
                hideStatus();
                mResultGroup.setVisibility(View.GONE);
            }

            @Override
            public void onNothingSelected(final AdapterView<?> parent) {
                // No-op.
            }
        });
    }

    private void setUpSendButton() {
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final AiProvider provider = mProviders[mProviderSpinner.getSelectedItemPosition()];
                final String prompt = mPromptInput.getText().toString().trim();

                if (TextUtils.isEmpty(prompt)) {
                    showStatus(mContext.getString(R.string.ai_error_empty_prompt), null);
                    return;
                }

                if (!AiPreferences.isConfigured(mContext, provider)) {
                    final String providerName = AiPreferences.displayName(mContext, provider);
                    showStatus(mContext.getString(R.string.ai_error_no_key, providerName),
                            new View.OnClickListener() {
                                @Override
                                public void onClick(final View statusView) {
                                    openAiSettings(mContext);
                                }
                            });
                    return;
                }

                mResultGroup.setVisibility(View.GONE);
                hideStatus();
                mProgress.setVisibility(View.VISIBLE);
                mSendButton.setEnabled(false);

                final String endpoint = AiPreferences.getEffectiveEndpoint(mContext, provider);
                final String apiKey = AiPreferences.getApiKey(mContext, provider);
                final String model = AiPreferences.getModel(mContext, provider);

                AiClient.send(provider, endpoint, apiKey, model, prompt, new AiClient.Callback() {
                    @Override
                    public void onSuccess(final String text) {
                        mProgress.setVisibility(View.GONE);
                        mSendButton.setEnabled(true);
                        if (TextUtils.isEmpty(text)) {
                            showStatus(mContext.getString(R.string.ai_error_empty_response), null);
                            return;
                        }
                        mResultText.setText(text);
                        mResultGroup.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onError(final String message) {
                        mProgress.setVisibility(View.GONE);
                        mSendButton.setEnabled(true);
                        showStatus(mContext.getString(R.string.ai_error_generic, message), null);
                    }
                });
            }
        });
    }

    public boolean isExpanded() {
        return mBound && mPanel.getVisibility() == View.VISIBLE;
    }

    public void expand() {
        if (!mBound) {
            return;
        }
        if (mToolbar != null) {
            mToolbar.setVisibility(View.GONE);
        }
        mPanel.setVisibility(View.VISIBLE);
    }

    public void collapse() {
        if (!mBound) {
            return;
        }
        mPanel.setVisibility(View.GONE);
        if (mToolbar != null && AiPreferences.getShowToolbar(mContext)) {
            mToolbar.setVisibility(View.VISIBLE);
        }
    }

    public void toggle() {
        if (isExpanded()) {
            collapse();
        } else {
            expand();
        }
    }

    public static void openAiSettings(final Context context) {
        final Intent intent = new Intent(context, SettingsActivity.class);
        intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, AiSettingsFragment.class.getName());
        intent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private void showStatus(final String message, final View.OnClickListener onClick) {
        mStatusText.setText(message);
        mStatusText.setVisibility(View.VISIBLE);
        mStatusText.setOnClickListener(onClick);
        mStatusText.setClickable(onClick != null);
    }

    private void hideStatus() {
        mStatusText.setVisibility(View.GONE);
        mStatusText.setOnClickListener(null);
        mStatusText.setClickable(false);
    }

    private static int indexOf(final AiProvider[] providers, final AiProvider target) {
        for (int i = 0; i < providers.length; i++) {
            if (providers[i] == target) {
                return i;
            }
        }
        return 0;
    }
}