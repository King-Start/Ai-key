package rkr.simplekeyboard.inputmethod.voice;

import android.content.Context;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.widget.Toast;

import rkr.simplekeyboard.inputmethod.latin.LatinIME;

public class VoiceInputManager {

    private final LatinIME mIme;
    private SpeechRecognizer mSpeechRecognizer;

    public VoiceInputManager(final LatinIME ime) {
        mIme = ime;
    }

    public void startListening() {
        if (SpeechRecognizer.isRecognitionAvailable(mIme)) {
            mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(mIme);
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID"); // atau sesuai locale

            // Listener untuk hasil voice
            // (implementasi lengkap listener bisa ditambah nanti)
            Toast.makeText(mIme, "Mulai bicara...", Toast.LENGTH_SHORT).show();

            mSpeechRecognizer.startListening(intent);
        } else {
            Toast.makeText(mIme, "Voice input tidak tersedia di perangkat ini", Toast.LENGTH_LONG).show();
        }
    }

    public void destroy() {
        if (mSpeechRecognizer != null) {
            mSpeechRecognizer.destroy();
        }
    }
}