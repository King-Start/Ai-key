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
 * The AI backends the keyboard can send prompts to.
 * Default models updated to commonly available ones (2026).
 */
public enum AiProvider {
    OPENAI(
            "openai",
            "ChatGPT (OpenAI)",
            "https://api.openai.com/v1/chat/completions",
            "gpt-4o-mini",
            RequestFormat.OPENAI),
    GEMINI(
            "gemini",
            "Gemini (Google)",
            "https://generativelanguage.googleapis.com/v1beta/models/",
            "gemini-2.0-flash",
            RequestFormat.GEMINI),
    DEEPSEEK(
            "deepseek",
            "DeepSeek",
            "https://api.deepseek.com/chat/completions",
            "deepseek-chat",
            RequestFormat.OPENAI),
    ANTHROPIC(
            "anthropic",
            "Claude (Anthropic)",
            "https://api.anthropic.com/v1/messages",
            "claude-3-5-sonnet-latest",
            RequestFormat.ANTHROPIC),
    CUSTOM(
            "custom",
            "Custom (OpenAI-compatible)",
            "",
            "",
            RequestFormat.OPENAI);

    public enum RequestFormat {
        OPENAI, GEMINI, ANTHROPIC
    }

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
