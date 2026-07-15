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

/**
 * The AI backends the keyboard can send prompts to. Each provider has a stable id (used as a
 * SharedPreferences key suffix, so it must never change once shipped), a default endpoint, a
 * default model, and a request/response shape used by {@link AiClient}.
 *
 * Default model ids are best-effort: providers change these fairly often, so every model id is
 * also exposed as an editable setting. If generation suddenly starts failing, the model name is
 * the first thing to check against the provider's docs.
 */
public enum AiProvider {
    OPENAI(
            "openai",
            "ChatGPT (OpenAI)",
            "https://api.openai.com/v1/chat/completions",
            "gpt-5.6",
            RequestFormat.OPENAI),
    GEMINI(
            "gemini",
            "Gemini (Google)",
            "https://generativelanguage.googleapis.com/v1beta/models/",
            "gemini-flash-latest",
            RequestFormat.GEMINI),
    DEEPSEEK(
            "deepseek",
            "DeepSeek",
            "https://api.deepseek.com/chat/completions",
            "deepseek-v4-flash",
            RequestFormat.OPENAI),
    ANTHROPIC(
            "anthropic",
            "Claude (Anthropic)",
            "https://api.anthropic.com/v1/messages",
            "claude-sonnet-5",
            RequestFormat.ANTHROPIC),
    CUSTOM(
            "custom",
            "Custom (OpenAI-compatible)",
            "",
            "",
            RequestFormat.OPENAI);

    /** Shape of the request/response body, since providers don't all speak the same dialect. */
    public enum RequestFormat {
        OPENAI, GEMINI, ANTHROPIC
    }

    /** Stable identifier persisted in SharedPreferences. Never change existing values. */
    public final String id;
    public final String displayName;
    public final String defaultEndpoint;
    public final String defaultModel;
    public final RequestFormat format;

    AiProvider(final String id, final String displayName, final String defaultEndpoint,
               final String defaultModel, final RequestFormat format) {
        this.id = id;
        this.displayName = displayName;
        this.defaultEndpoint = defaultEndpoint;
        this.defaultModel = defaultModel;
        this.format = format;
    }

    public static AiProvider fromId(final String id) {
        for (final AiProvider provider : values()) {
            if (provider.id.equals(id)) {
                return provider;
            }
        }
        return OPENAI;
    }
}
