package rkr.simplekeyboard.inputmethod.emoji;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import rkr.simplekeyboard.inputmethod.latin.LatinIME;

public class EmojiPanelController {

    private final LatinIME mIme;
    private final TextCommitter mCommitter;
    private View mPanel;

    public interface TextCommitter {
        void commitText(CharSequence text);
    }

    public EmojiPanelController(final LatinIME ime, final TextCommitter committer) {
        mIme = ime;
        mCommitter = committer;
    }

    public void attach(final View rootView) {
        mPanel = rootView.findViewById(R.id.emoji_panel);
        if (mPanel == null) return;

        // Contoh tombol emoji sederhana
        Button emojiBtn = mPanel.findViewById(R.id.emoji_button_sample);
        if (emojiBtn != null) {
            emojiBtn.setOnClickListener(v -> mCommitter.commitText("😊"));
        }

        // Bisa ditambah RecyclerView untuk full emoji grid nanti
    }

    public void toggle() {
        if (mPanel != null) {
            mPanel.setVisibility(mPanel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        }
    }
}