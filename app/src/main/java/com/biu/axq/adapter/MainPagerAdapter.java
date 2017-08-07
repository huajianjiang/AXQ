package com.biu.axq.adapter;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.biu.axq.data.BleDevice;
import com.biu.axq.ui.fragment.AppFragment;
import com.biu.axq.ui.fragment.BleControlFragment;
import com.biu.axq.ui.fragment.BleScanFragment;
import com.biu.axq.util.Constants;
import com.biu.axq.util.Logger;

import java.util.ArrayList;
import java.util.List;


/**
 * <p>Author: Huajian Jiang
 * <br>Date: 2017/5/11
 * <br>Email: developer.huajianjiang@gmail.com
 */
public class MainPagerAdapter extends FragmentPagerAdapter {

    public static final int PAGE_COUNT = 3;
    private static final String TAG = MainPagerAdapter.class.getSimpleName();

    public MainPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Fragment getItem(int position) {
        Fragment content;
        switch (position) {
            case 0:
                content = new BleScanFragment();
                break;
            case 1:
            case 2:
                Logger.e(TAG, "getItem>" + position);
                content = new BleControlFragment();
                BleDevice device = new BleDevice();
                device.address = position == 1 ? "A0:E6:F8:35:87:26" : "A0:E6:F8:35:86:E7";
                Bundle args = new Bundle();
                args.putSerializable(Constants.KEY_ENTITY, device);
                content.setArguments(args);
                break;
            default:
                throw new IllegalStateException("FragmentPagerAdapter exp");
        }

        return content;
    }


    @Override
    public int getCount() {
        return PAGE_COUNT;
    }


}
