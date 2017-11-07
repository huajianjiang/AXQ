package com.biu.axq.ui.fragment;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.biu.axq.R;
import com.biu.axq.adapter.BleAdapter;
import com.biu.axq.data.BleDevice;
import com.biu.axq.ui.activity.BleControlActivity;
import com.biu.axq.util.Constants;
import com.biu.axq.util.Logger;
import com.biu.axq.util.Msgs;
import com.biu.axq.util.Utils;
import com.biu.axq.util.Views;
import com.github.huajianjiang.baserecyclerview.widget.BaseAdapter;
import com.github.huajianjiang.expandablerecyclerview.widget.PatchedRecyclerView;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import static com.biu.axq.util.Constants.RQ_ENABLE_BT;

/**
 * <p>Author: Huajian Jiang
 * <br>Date: 2017/7/31
 * <br>Email: developer.huajianjiang@gmail.com
 */
public class BleScanFragment extends AppFragment implements BaseAdapter.OnItemClickListener {
    private static final String TAG = BleScanFragment.class.getSimpleName();
    private SwipeRefreshLayout mRefreshLayout;
    private PatchedRecyclerView mRv;
    private BleAdapter mAdapter;
    private BluetoothAdapter mBtAdapter;

    private Disposable mDelayTask;

    private boolean mScanning;

    private View mLoadingView;
    private View mEmptyView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        checkHardware();
    }

    @Override
    public View onGenerateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.ble_scan_frag, container, false);
    }

    @Override
    public void onInitView(View root) {
        mRefreshLayout = Views.find(root, R.id.refreshLayout);
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                toggleScan(true);
            }
        });
        mRv = Views.find(root, R.id.rv);
        mAdapter = new BleAdapter(getContext().getApplicationContext());
        mRv.setAdapter(mAdapter);
        mRv.addItemDecoration(
                new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));

        mLoadingView = Views.find(root, R.id.loadingBar);
        mEmptyView = Views.find(root, R.id.empty);

        mRv.setEmptyView(mLoadingView);

        mAdapter.clickTargets(R.id.itemView).listenClickEvent(this);
    }


    @Override
    public void onResume() {
        super.onResume();
        toggleScan(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        toggleScan(false);
    }

    @Override
    public void onItemClick(RecyclerView parent, View view) {

        toggleScan(false);

        int pos = parent.getChildAdapterPosition(parent.findContainingItemView(view));
        BluetoothDevice device = mAdapter.getItem(pos);
        BleDevice bleDevice = new BleDevice();
        bleDevice.address = device.getAddress();
        Intent controlIntent = new Intent(getContext(), BleControlActivity.class);
        controlIntent.putExtra(Constants.KEY_ENTITY, bleDevice);
        startActivity(controlIntent);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.RQ_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                toggleScan(true);
            } else {
                Msgs.shortToast(getContext(), "蓝牙开启请求被拒，请手动打开蓝牙");
            }
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem progress = menu.findItem(R.id.progress);
        if (mScanning) {
            progress.setActionView(R.layout.menu_progress);
        }else {
            progress.setActionView(null);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.ble_sacn, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    private void checkHardware() {
        if (!Utils.checkSupportBT(getContext()) || !Utils.checkSupportBLE(getContext())) {
            Msgs.shortToast(getContext(), "该设备不支持 蓝牙/BLE 功能,无法扫描蓝牙");
            getActivity().finish();
        }

        mBtAdapter = ((BluetoothManager) getContext().getSystemService(
                Context.BLUETOOTH_SERVICE)).getAdapter();

        if (!mBtAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, RQ_ENABLE_BT);
        }
    }

    private void toggleScan(boolean scan) {
        if (scan) {
            startScanning();
        } else {
            stopScanning();
        }
    }

    private void startScanning() {
        stopScanning();

        mRv.setEmptyView(mLoadingView);
        mAdapter.invalidate(null);

        mScanning = true;
        mBtAdapter.startLeScan(mScanCallback);
        mDelayTask = Observable.timer(10, TimeUnit.SECONDS)
                               .subscribeOn(AndroidSchedulers.mainThread())
                               .observeOn(AndroidSchedulers.mainThread())
                               .subscribe(new Consumer<Long>() {
                                   @Override
                                   public void accept(@NonNull Long aLong) throws Exception {
                                       mBtAdapter.stopLeScan(mScanCallback);
                                       mScanning = false;
                                       if (mAdapter.isEmpty()) {
                                           Logger.e(TAG, "scan result nothing");
                                           mRv.setEmptyView(mEmptyView);
                                       }
                                       getActivity().invalidateOptionsMenu();
                                       Logger.e(TAG, "stop scanning");
                                   }
                               });

        getActivity().invalidateOptionsMenu();
    }

    private void stopScanning() {
        mRefreshLayout.setRefreshing(false);
        mBtAdapter.stopLeScan(mScanCallback);
        if (mDelayTask != null) {
            mDelayTask.dispose();
            mDelayTask = null;
        }
        mScanning = false;
        getActivity().invalidateOptionsMenu();
    }

    private BluetoothAdapter.LeScanCallback mScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            Logger.e(TAG, "onLeScan>>>" + device.getName());
            mAdapter.insert(device);
        }
    };

}
