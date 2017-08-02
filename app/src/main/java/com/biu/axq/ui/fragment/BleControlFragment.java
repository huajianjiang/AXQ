package com.biu.axq.ui.fragment;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.widget.TextViewCompat;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.biu.axq.BuildConfig;
import com.biu.axq.R;
import com.biu.axq.service.BleService;
import com.biu.axq.util.Constants;
import com.biu.axq.util.Logger;
import com.biu.axq.util.Msgs;
import com.biu.axq.util.Views;

import java.util.List;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * <p>Author: Huajian Jiang
 * <br>Date: 2017/8/2
 * <br>Email: developer.huajianjiang@gmail.com
 */
public class BleControlFragment extends AppFragment {
    private static final String TAG = BleControlFragment.class.getSimpleName();

    private TextView mOutput;
    private TextView mInput;
    private TextView mWrite;

    private BleService mService;
    private BluetoothDevice mDevice;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mDevice = getArguments().getParcelable(Constants.KEY_ENTITY);

        Intent gattServiceIntent = new Intent(getContext(), BleService.class);
        getContext().bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    public View onGenerateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.ble_control_frag, container, false);
    }

    @Override
    public void onInitView(View root) {
        mInput = Views.find(root, R.id.et_input);
        mOutput = Views.find(root, R.id.tv_output);
        mWrite = Views.find(root, R.id.tv_write);

    }

    @Override
    public void onResume() {
        super.onResume();
        getContext().registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mService != null) {
            Logger.e(TAG, "connect");
            mService.connect(mDevice.getAddress());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        getContext().unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getContext().unbindService(mServiceConnection);
        mService = null;
    }

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        String uuid;
        BluetoothGattCharacteristic notifyCharacteristic = null;

        for (BluetoothGattService service : gattServices) {
            uuid = service.getUuid().toString();
            if (uuid.equalsIgnoreCase(BleService.UUID_SERVER)) {
                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                for (BluetoothGattCharacteristic characteristic : characteristics) {
                    uuid = characteristic.getUuid().toString();
                    if (uuid.equalsIgnoreCase(BleService.UUID_NOTIFY)) {
                        notifyCharacteristic = characteristic;
                    }
                }
                break;
            }
        }

        if (notifyCharacteristic != null) {
            Logger.e(TAG, "setNotifyCharacteristic==>" + notifyCharacteristic.getUuid().toString());
            mService.setCharacteristicNotification(notifyCharacteristic, true);
        }
    }

    private void displayData(String data) {
        Logger.e(TAG, data + "\n");
        mOutput.getEditableText().insert(0, data + "\n");
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BleService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BleService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BleService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BleService.ACTION_GATT_CONNECTED.equals(action)) {
                Msgs.shortToast(getContext(), "连接成功");
            } else if (BleService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Msgs.shortToast(getContext(), "已断开连接");
            } else if (BleService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mService.getSupportedGattServices());
            } else if (BleService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BleService.EXTRA_DATA));
            }
        }
    };

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Logger.e(TAG, "onServiceConnected");
            mService = ((BleService.BleBinder) service).getService();
            if (!mService.init()) {
                Msgs.shortToast(getContext(), "无法初始化蓝牙!");
                getActivity().finish();
            }
            mService.connect(mDevice.getAddress());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };


}
