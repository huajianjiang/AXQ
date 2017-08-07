package com.biu.axq.ui.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import com.biu.axq.R;
import com.biu.axq.util.Views;


/**
 * <p>Author: Huajian Jiang
 * <br>Date: 2017/2/23
 * <br>Email: developer.huajianjiang@gmail.com
 */
public abstract class BaseActivity extends AppCompatActivity {
    private static final String TAG = BaseActivity.class.getSimpleName();

    private OnPreFinishListener mListener;

    private Toolbar mToolbar;

    protected abstract Fragment onCreateFragment();

    protected FrameLayout getFragmentContainer() {
        return (FrameLayout) findViewById(getFragmentContainerId());
    }

    protected int getFragmentContainerId() {
        return R.id.frag_container;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acti_base);

        mToolbar = Views.find(this, R.id.toolbar);
        setSupportActionBar(mToolbar);

        FragmentManager fm = getSupportFragmentManager();
        Fragment content = fm.findFragmentById(getFragmentContainerId());
        if (content == null) {
            content = onCreateFragment();
            if (content == null) return;
            fm.beginTransaction().replace(getFragmentContainerId(), content).commit();
        }
    }

    public Toolbar getToolbar() {
        return mToolbar;
    }

    public void setOnPreFinishListener(OnPreFinishListener listener) {
        mListener = listener;
    }

    public void showSysActionbarTitle() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) return;
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);
    }

    public void hideSysActionbarTitle() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) return;
        actionBar.setDisplayOptions(0);
    }

    public void hideSysActionbar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) return;
        actionBar.hide();
    }

    public void setBackNaviAction() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) return;
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
    }

    public void setBackNaviAction(int resId) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) return;
        actionBar.setHomeAsUpIndicator(resId);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
    }

    @SuppressWarnings("unchecked")
    public <T extends View> T getCustomViewForToolbar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) return null;
        return (T) actionBar.getCustomView();
    }

    @SuppressWarnings("unchecked")
    public <T extends View> T setCustomViewForToolbar(int layoutResId) {
        View view = getLayoutInflater().inflate(layoutResId, mToolbar, false);
        setCustomViewForToolbar(view, null);
        return (T) view;
    }

    @SuppressWarnings("unchecked")
    public <T extends View> T setCustomViewForToolbar(int layoutResId, ActionBar.LayoutParams lp) {
        View view = getLayoutInflater().inflate(layoutResId, mToolbar, false);
        setCustomViewForToolbar(view, lp);
        return (T) view;
    }

    public void setCustomViewForToolbar(View view, ActionBar.LayoutParams lp) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) return;
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        if (lp == null) {
            lp = new ActionBar.LayoutParams(view.getLayoutParams());
            lp.gravity = Gravity.CENTER;
        }
        actionBar.setCustomView(view, lp);
    }

    protected boolean shouldBackPressedFinish() {
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (shouldBackPressedFinish()) {
            boolean canFinish = true;
            if (mListener != null) {
                canFinish = mListener.onPreFinish();
            }
            if (canFinish) {
                onBackPressed();
                return true;
            }
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if ((mListener != null && mListener.onPreFinish()) || shouldBackPressedFinish()) {
            super.onBackPressed();
        }
    }

    public interface OnPreFinishListener {
        boolean onPreFinish();
    }

}
