package com.biu.axq.util;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

/**
 * <p>Author: Huajian Jiang
 * <br>Date: 2017/4/6
 * <br>Email: developer.huajianjiang@gmail.com
 */
@SuppressWarnings("unused")
public class App extends Application {
    public static final String TAG = App.class.getSimpleName();
    private static App INSTANCE;

    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;
    }

    public static App get() {
        return INSTANCE;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

}
