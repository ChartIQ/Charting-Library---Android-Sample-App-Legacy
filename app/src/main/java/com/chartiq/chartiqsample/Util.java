package com.chartiq.chartiqsample;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

/**
 * Created by mist on 15.03.17.
 */

public class Util {
    public static void hideKeyboard(final View view) {
        view.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager in = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                in.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }, 100);
    }
}
