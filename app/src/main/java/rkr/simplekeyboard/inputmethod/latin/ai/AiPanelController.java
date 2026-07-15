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

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.preference.PreferenceActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
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
import rkr.simplekeyboard.inputmethod.latin.utils.DialogUtils;

/**
 * Builds and drives the "type a prompt, get AI text back" dialog opened from the keyboard
 * toolbar. One instance is kept alive for the lifetime of the IME service and reused every time
 * the AI button is tapped.
 */
public final class AiPanelController {

    /** Lets the controller commit generated text into whatever app the keyboard is currently in. */
    public interface TextCommitter {
        void commitText(CharSequence text);
    }

    private final Context mContext;
    private final TextCommitter mCommitter;
    private AlertDialog mDialog;

    public AiPanelController(final Context context, final TextCommitter committer) {
        mContext = context;
        mCommitter = committer;
    }

    public boolean isShowing() {
        return mDialog != null && mDialog.isShowing();
    }

    public void dismiss() {
        if (isShowing()) {
            mDialog.dismiss();
        }
        mDialog = null;
    }

    public void show(final IBinder windowToken) {
        if (windowToken == null || isShowing()) {
            return;
        }

        final Context themedContext = DialogUtils.getPlatformDialogThemeContext(mContext);
        final View view = LayoutInflater.from(themedContext).inflate(R.layout.ai_panel_dialog, null);

        final Spinner providerSpinner = view.findViewById(R.id.ai_provider_spinner);
        final EditText promptInput = view.findViewById(R.id.ai_prompt_input);
        final ProgressBar progress = view.findViewById(R.id.ai_progress);
        final TextView statusText = view.findViewById(R.id.ai_status_text);
        final Button sendButton = view.findViewById(R.id.ai_send_button);
        final View resultGroup = view.findViewById(R.id.ai_result_group);
        final TextView resultText = view.findViewById(R.id.ai_result_text);
        final Button insertButton = view.findViewById(R.id.ai_insert_button);
        final Button copyButton = view.findViewById(R.id.ai_copy_button);

        final AiProvider[] providers = AiProvider.values();
        final String[] labels = new String[providers.length];
        for (int i = 0; i < providers.length; i++) {
            labels[i] = AiPreferences.displayName(mContext, providers[i]);
        }
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(
                themedContext, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        providerSpinner.setAdapter(adapter);

        final AiProvider initialProvider = AiPreferences.getSelectedProvider(mContext);
        final int initialIndex = indexOf(providers, initialProvider);
        providerSpinner.setSelection(initialIndex);

        providerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> parent, final View v, final int position,
                                        final long id) {
                AiPreferences.setSelectedProvider(mContext, providers[position]);
                hideStatus(statusText);
                resultGroup.setVisibility(View.GONE);
            }

            @Override
            public void onNothingSelected(final AdapterView<?> parent) {
                // No-op.
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final AiProvider provider = providers[providerSpinner.getSelectedItemPosition()];
                final String prompt = promptInput.getText().toString().trim();

                if (TextUtils.isEmpty(prompt)) {
                    showStatus(statusText, mContext.getString(R.string.ai_error_empty_prompt), null);
                    return;
                }

                if (!AiPreferences.isConfigured(mContext, provider)) {
                    final String providerName = AiPreferences.displayName(mContext, provider);
                    showStatus(statusText,
                            mContext.getString(R.string.ai_error_no_key, providerName),
                            new View.OnClickListener() {
                                @Override
                                public void onClick(final View statusView) {
                                    dismiss();
                                    openAiSettings(mContext);
                                }
                            });
                    return;
                }

                resultGroup.setVisibility(View.GONE);
                hideStatus(statusText);
                progress.setVisibility(View.VISIBLE);
                sendButton.setEnabled(false);

                final String endpoint = AiPreferences.getEffectiveEndpoint(mContext, provider);
                final String apiKey = AiPreferences.getApiKey(mContext, provider);
                final String model = AiPreferences.getModel(mContext, provider);

                AiClient.send(provider, endpoint, apiKey, model, prompt, new AiClient.Callback() {
                    @Override
                    public void onSuccess(final String text) {
                        progress.setVisibility(View.GONE);
                        sendButton.setEnabled(true);
                        if (TextUtils.isEmpty(text)) {
                            showStatus(statusText, mContext.getString(R.string.ai_error_empty_response), null);
                            return;
                        }
                        resultText.setText(text);
                        resultGroup.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onError(final String message) {
                        progress.setVisibility(View.GONE);
                        sendButton.setEnabled(true);
                        showStatus(statusText, mContext.getString(R.string.ai_error_generic, message), null);
                    }
                });
            }
        });

        insertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                mCommitter.commitText(resultText.getText());
                dismiss();
            }
        });

        copyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final ClipboardManager clipboard =
                        (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("AI result", resultText.getText()));
                    Toast.makeText(mContext, R.string.ai_copied_toast, Toast.LENGTH_SHORT).show();
                }
            }
        });

        final AlertDialog.Builder builder = new AlertDialog.Builder(themedContext);
        builder.setTitle(R.string.ai_dialog_title);
        builder.setView(view);
        builder.setNegativeButton(R.string.ai_close_button, null);

        final AlertDialog dialog = builder.create();
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        final Window window = dialog.getWindow();
        final WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.token = windowToken;
        layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
        window.setAttributes(layoutParams);
        window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

        mDialog = dialog;
        dialog.show();
    }

    public static void openAiSettings(final Context context) {
        final Intent intent = new Intent(context, SettingsActivity.class);
        intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, AiSettingsFragment.class.getName());
        intent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private static void showStatus(final TextView statusText, final String message,
                                    final View.OnClickListener onClick) {
        statusText.setText(message);
        statusText.setVisibility(View.VISIBLE);
        statusText.setOnClickListener(onClick);
        statusText.setClickable(onClick != null);
    }

    private static void hideStatus(final TextView statusText) {
        statusText.setVisibility(View.GONE);
        statusText.setOnClickListener(null);
        statusText.setClickable(false);
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
