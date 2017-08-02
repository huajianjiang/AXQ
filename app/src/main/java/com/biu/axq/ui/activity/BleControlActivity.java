package com.biu.axq.ui.activity;

import android.support.v4.app.Fragment;

import com.biu.axq.ui.fragment.BleControlFragment;

/**
 * <p>Author: Huajian Jiang
 * <br>Date: 2017/8/2
 * <br>Email: developer.huajianjiang@gmail.com
 */
public class BleControlActivity extends AppActivity {

    @Override
    protected Fragment onCreateFragment() {
        BleControlFragment controlFrag = new BleControlFragment();
        controlFrag.setArguments(getIntent().getExtras());
        return controlFrag;
    }

}
