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

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Sends a single prompt to an {@link AiProvider} and returns the generated text. No streaming,
 * no conversation history: this is a one-shot "type a prompt, get text back" tool, not a chat
 * client, so every call is a fresh request with just one user message.
 *
 * Uses only {@link HttpURLConnection} and {@code org.json} (both built into Android) so the AI
 * feature doesn't pull in any new Gradle dependency.
 */
public final class AiClient {

    public interface Callback {
        void onSuccess(String text);
        void onError(String message);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_THREAD = new Handler(Looper.getMainLooper());
    private static final int TIMEOUT_MS = 30_000;
    private static final int MAX_TOKENS = 1024;

    private AiClient() {
        // Not instantiable.
    }

    /** Fires the request on a background thread; {@code callback} is always invoked on the main thread. */
    public static void send(final AiProvider provider, final String endpoint, final String apiKey,
                             final String model, final String prompt, final Callback callback) {
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final String result = request(provider, endpoint, apiKey, model, prompt);
                    postSuccess(callback, result);
                } catch (final Exception e) {
                    postError(callback, describeError(e));
                }
            }
        });
    }

    private static void postSuccess(final Callback callback, final String text) {
        MAIN_THREAD.post(new Runnable() {
            @Override
            public void run() {
                callback.onSuccess(text);
            }
        });
    }

    private static void postError(final Callback callback, final String message) {
        MAIN_THREAD.post(new Runnable() {
            @Override
            public void run() {
                callback.onError(message);
            }
        });
    }

    private static String describeError(final Exception e) {
        if (e instanceof UnknownHostException) {
            return "No internet connection.";
        }
        if (e instanceof SocketTimeoutException) {
            return "Request timed out. Try again.";
        }
        final String message = e.getMessage();
        return message != null && message.length() > 0 ? message : e.getClass().getSimpleName();
    }

    private static String request(final AiProvider provider, final String endpoint, final String apiKey,
                                   final String model, final String prompt)
            throws IOException, JSONException {
        switch (provider.format) {
            case GEMINI:
                return requestGemini(endpoint, apiKey, model, prompt);
            case ANTHROPIC:
                return requestAnthropic(endpoint, apiKey, model, prompt);
            case OPENAI:
            default:
                return requestOpenAiStyle(endpoint, apiKey, model, prompt);
        }
    }

    // OpenAI, DeepSeek, and any OpenAI-compatible custom endpoint all speak this dialect.
    private static String requestOpenAiStyle(final String endpoint, final String apiKey,
                                              final String model, final String prompt)
            throws IOException, JSONException {
        final JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", prompt);
        final JSONArray messages = new JSONArray();
        messages.put(message);

        final JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("messages", messages);
        body.put("max_tokens", MAX_TOKENS);

        final HttpURLConnection connection = openConnection(endpoint);
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        final JSONObject response = new JSONObject(postJson(connection, body.toString()));
        return response.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim();
    }

    private static String requestGemini(final String endpointBase, final String apiKey,
                                         final String model, final String prompt)
            throws IOException, JSONException {
        final JSONObject part = new JSONObject();
        part.put("text", prompt);
        final JSONArray parts = new JSONArray();
        parts.put(part);
        final JSONObject content = new JSONObject();
        content.put("parts", parts);
        final JSONArray contents = new JSONArray();
        contents.put(content);
        final JSONObject body = new JSONObject();
        body.put("contents", contents);

        final String url = endpointBase + model + ":generateContent";
        final HttpURLConnection connection = openConnection(url);
        connection.setRequestProperty("x-goog-api-key", apiKey);
        final JSONObject response = new JSONObject(postJson(connection, body.toString()));
        return response.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim();
    }

    private static String requestAnthropic(final String endpoint, final String apiKey,
                                            final String model, final String prompt)
            throws IOException, JSONException {
        final JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", prompt);
        final JSONArray messages = new JSONArray();
        messages.put(message);

        final JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("max_tokens", MAX_TOKENS);
        body.put("messages", messages);

        final HttpURLConnection connection = openConnection(endpoint);
        connection.setRequestProperty("x-api-key", apiKey);
        connection.setRequestProperty("anthropic-version", "2023-06-01");
        final JSONObject response = new JSONObject(postJson(connection, body.toString()));
        return response.getJSONArray("content")
                .getJSONObject(0)
                .getString("text")
                .trim();
    }

    private static HttpURLConnection openConnection(final String urlString) throws IOException {
        final URL url = new URL(urlString);
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);
        connection.setDoOutput(true);
        return connection;
    }

    private static String postJson(final HttpURLConnection connection, final String jsonBody) throws IOException {
        try {
            final OutputStream outputStream = connection.getOutputStream();
            try {
                outputStream.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            } finally {
                outputStream.close();
            }

            final int status = connection.getResponseCode();
            final InputStream inputStream = (status >= 200 && status < 300)
                    ? connection.getInputStream() : connection.getErrorStream();
            final String responseBody = readStream(inputStream);

            if (status < 200 || status >= 300) {
                throw new IOException(extractErrorMessage(responseBody, status));
            }
            return responseBody;
        } finally {
            connection.disconnect();
        }
    }

    private static String readStream(final InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        final StringBuilder builder = new StringBuilder();
        final BufferedReader reader =
                new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        } finally {
            reader.close();
        }
        return builder.toString();
    }

    private static String extractErrorMessage(final String responseBody, final int status) {
        try {
            final JSONObject json = new JSONObject(responseBody);
            if (json.has("error")) {
                final Object error = json.get("error");
                if (error instanceof JSONObject && ((JSONObject) error).has("message")) {
                    return "HTTP " + status + ": " + ((JSONObject) error).getString("message");
                }
                return "HTTP " + status + ": " + error.toString();
            }
            if (json.has("message")) {
                return "HTTP " + status + ": " + json.getString("message");
            }
        } catch (final JSONException ignored) {
            // Response wasn't JSON (or didn't match an expected error shape); fall through.
        }
        if (responseBody == null || responseBody.length() == 0) {
            return "HTTP " + status;
        }
        final int end = Math.min(200, responseBody.length());
        return "HTTP " + status + ": " + responseBody.substring(0, end);
    }
}
