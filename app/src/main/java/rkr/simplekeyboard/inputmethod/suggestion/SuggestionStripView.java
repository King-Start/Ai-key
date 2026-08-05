package rkr.simplekeyboard.inputmethod.suggestion;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import rkr.simplekeyboard.inputmethod.latin.LatinIME;

public class SuggestionStripView extends LinearLayout {

    private LatinIME mIme;

    public SuggestionStripView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        setBackgroundColor(0xFF37474F);
    }

    public void setIme(LatinIME ime) {
        mIme = ime;
    }

    public void showSuggestions(String[] suggestions) {
        removeAllViews();

        for (String suggestion : suggestions) {
            TextView tv = new TextView(getContext());
            tv.setText(suggestion);
            tv.setPadding(16, 8, 16, 8);
            tv.setTextColor(0xFFFFFFFF);
            tv.setTextSize(16);
            tv.setOnClickListener(v -> {
                if (mIme != null) mIme.commitText(suggestion + " ");
            });
            addView(tv);
        }
    }
}