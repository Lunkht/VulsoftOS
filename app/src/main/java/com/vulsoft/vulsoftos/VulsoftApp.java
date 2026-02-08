package com.vulsoft.vulsoftos;

import android.app.Application;
import android.content.Context;

public class VulsoftApp extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }
}
