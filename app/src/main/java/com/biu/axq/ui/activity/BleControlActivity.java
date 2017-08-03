package com.biu.axq.ui.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.biu.axq.R;
import com.biu.axq.ui.fragment.BleControlFragment;

/**
 * <p>Author: Huajian Jiang
 * <br>Date: 2017/8/2
 * <br>Email: developer.huajianjiang@gmail.com
 */
public class BleControlActivity extends AppActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.ble_console);
        setBackNaviAction();
    }

    @Override
    protected Fragment onCreateFragment() {
        BleControlFragment controlFrag = new BleControlFragment();
        controlFrag.setArguments(getIntent().getExtras());
        return controlFrag;
    }

}
