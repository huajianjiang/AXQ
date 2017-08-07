package com.biu.axq.ui.activity;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Toast;

import com.biu.axq.adapter.MainPagerAdapter;
import com.biu.axq.ui.fragment.BleScanFragment;
import com.biu.axq.util.Constants;
import com.biu.axq.R;
import com.biu.axq.util.Views;

import java.util.Random;

import static com.biu.axq.util.Constants.RQ_PERM_LOCATION;
import static com.biu.axq.util.Utils.checkSupportBLE;
import static com.biu.axq.util.Utils.checkSupportBT;

public class MainActivity extends AppCompatActivity {
    private Toolbar mToolbar;
    private ViewPager mVp;
    private MainPagerAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acti_main);

        mToolbar = Views.find(this, R.id.toolbar);
        setSupportActionBar(mToolbar);

        BottomNavigationView naviView = Views.find(this,R.id.naviView);
        naviView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        mVp = Views.find(this, R.id.vp);
        mAdapter = new MainPagerAdapter(getSupportFragmentManager());
        mVp.setAdapter(mAdapter);
        mVp.setOffscreenPageLimit(mAdapter.getCount() - 1);

        //TODO
        ActivityCompat.requestPermissions(this,
                                          new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                          RQ_PERM_LOCATION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RQ_PERM_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "yes，perms granted", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private BottomNavigationView.OnNavigationItemSelectedListener
            mOnNavigationItemSelectedListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {

                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.navigation_home:
                        case R.id.navigation_dashboard:
                        case R.id.navigation_notifications:
                            mVp.setCurrentItem(item.getOrder());
                            return true;
                    }
                    return false;
                }
            };


}
