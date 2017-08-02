package com.biu.axq.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.biu.axq.ui.fragment.AppFragment;
import com.biu.axq.ui.fragment.BleScanFragment;


/**
 * <p>Author: Huajian Jiang
 * <br>Date: 2017/5/11
 * <br>Email: developer.huajianjiang@gmail.com
 */
public class MainPagerAdapter extends FragmentPagerAdapter {

    public static final int PAGE_COUNT = 1;
    private static final String TAG = MainPagerAdapter.class.getSimpleName();

    public MainPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Fragment getItem(int position) {
        return new BleScanFragment();
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }
}
