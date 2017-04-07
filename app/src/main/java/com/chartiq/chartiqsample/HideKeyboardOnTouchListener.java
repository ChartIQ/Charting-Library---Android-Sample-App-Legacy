package com.chartiq.chartiqsample;

import android.view.MotionEvent;
import android.view.View;

public class HideKeyboardOnTouchListener implements View.OnTouchListener {
    @Override
    public boolean onTouch(final View view, MotionEvent event) {
        Util.hideKeyboard(view);
        return false;
    }
}
