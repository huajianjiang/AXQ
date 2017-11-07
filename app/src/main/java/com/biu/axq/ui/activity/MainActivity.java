package com.biu.axq.ui.activity;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.biu.axq.adapter.MainPagerAdapter;
import com.biu.axq.ui.fragment.BleScanFragment;
import com.biu.axq.util.Constants;
import com.biu.axq.R;
import com.biu.axq.util.Logger;
import com.biu.axq.util.Res;
import com.biu.axq.util.Views;

import java.util.List;
import java.util.Random;

import static com.biu.axq.util.Constants.RQ_PERM_LOCATION;
import static com.biu.axq.util.Utils.checkSupportBLE;
import static com.biu.axq.util.Utils.checkSupportBT;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
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
        test();

        Views.find(this, R.id.coorLayout)
             .setPadding(0, Res.getStatusBarHeight(this), 0, Res.getNavigationBarHeight(this));
    }

    private WifiManager mWifiManager;

    private void test() {
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        List<String> providers = lm.getAllProviders();
        for (String name : providers) {
            Logger.e(TAG, "providerName = " + name);
            try {
                if (lm.isProviderEnabled(name)) {
                    Logger.e(TAG, "providerName = " + name + " enabled");
                }
            } catch (Exception e) {
                Logger.e(TAG, "EXP");
            }
        }

        mWifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        IntentFilter intentFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(new WifiReceiver(), intentFilter);
    }

    private class WifiReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                List<ScanResult> results = mWifiManager.getScanResults();
                for (ScanResult result : results) {
                    Logger.e(TAG, "wifi = " + result.SSID);
                }
            }
        }
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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.scan) {
            if (mWifiManager.startScan()) {
                Logger.e(TAG,"starScan");
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
