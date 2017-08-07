package com.biu.axq.ui.widget;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by jhj_Plus on 2016/10/25.
 */
public class MyViewPager extends ViewPager {
    private static final String TAG = "MyViewPager";

    private boolean mScrollable = false;

    public MyViewPager(Context context) {
        super(context);
    }

    public MyViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public boolean isScrollable() {
        return mScrollable;
    }

    public void setScrollable(boolean scrollable) {
        mScrollable = scrollable;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mScrollable && super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return mScrollable && super.onTouchEvent(ev);
    }
}
