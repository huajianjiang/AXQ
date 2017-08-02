package com.biu.axq.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;


import com.biu.axq.BuildConfig;
import com.biu.axq.util.Logger;
import com.biu.axq.util.Utils;

import java.util.List;
import java.util.UUID;

/**
 * <p>Author: Huajian Jiang
 * <br>Date: 2017/8/2
 * <br>Email: developer.huajianjiang@gmail.com
 */
public class BleService extends Service {
    private static final String TAG = BleService.class.getSimpleName();

    public final static String UUID_SERVER = "0000fff0-0000-1000-8000-00805f9b34fb";
    public final static String UUID_NOTIFY = "0000fff6-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            BuildConfig.APPLICATION_ID + ".ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            BuildConfig.APPLICATION_ID + ".ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            BuildConfig.APPLICATION_ID + ".ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            BuildConfig.APPLICATION_ID + ".ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = BuildConfig.APPLICATION_ID + ".EXTRA_DATA";

    private BluetoothManager mBleManager;
    private BluetoothAdapter mBleAdapter;
    private BluetoothGatt mBleGatt;
    private int mConnState = STATE_DISCONNECTED;
    private String mBleAddr;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBleBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    public Binder mBleBinder = new BleBinder();

    public class BleBinder extends Binder {
        public BleService getService() {
            return BleService.this;
        }
    }

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Logger.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Logger.i(TAG, "Attempting to start service discovery:" + mBleGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnState = STATE_DISCONNECTED;
                Logger.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Logger.e(TAG, "onServicesDiscovered");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Logger.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic, int status)
        {
            Logger.e(TAG, "onCharacteristicRead");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic, int status)
        {
            Logger.e(TAG, "onCharacteristicWrite");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Logger.i(TAG, "onCharacteristicWrite Successful");
            } else {
                Logger.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic)
        {
            Logger.e(TAG, "onCharacteristicChanged");
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };


    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
            final BluetoothGattCharacteristic characteristic)
    {
        final Intent intent = new Intent(action);
        String extra = Utils.bytesToHexString(characteristic.getValue());
        intent.putExtra(EXTRA_DATA, extra);
        sendBroadcast(intent);
    }

    public boolean init() {
        if (mBleManager == null) {
            mBleManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBleManager == null) {
                Logger.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBleAdapter = mBleManager.getAdapter();
        if (mBleAdapter == null) {
            Logger.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    public boolean connect(final String address) {
        if (mBleAdapter == null || TextUtils.isEmpty(address)) {
            Logger.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBleAddr != null && address.equals(mBleAddr)
            && mBleGatt != null) {
            Logger.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBleGatt.connect()) {
                mConnState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBleAdapter.getRemoteDevice(address);
        if (device == null) {
            Logger.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBleGatt = device.connectGatt(this, false, mGattCallback);

        Logger.d(TAG, "Trying to create a new connection.");
        mBleAddr = address;
        mConnState = STATE_CONNECTING;
        return true;
    }

    public void disconnect() {
        if (mBleAdapter == null || mBleGatt == null) {
            Logger.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBleGatt.disconnect();
    }

    public boolean readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBleAdapter == null || mBleGatt == null) {
            Logger.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        return mBleGatt.readCharacteristic(characteristic);
    }

    /**
     * 修改某个服务中的某个特型属性
     * @param characteristic 特型属性
     * @param value 新值
     */
    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] value) {
        if (mBleAdapter == null || mBleGatt == null) {
            Logger.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }

        boolean successful = characteristic.setValue(value);
        if (successful) {
            successful = mBleGatt.writeCharacteristic(characteristic);
        }

        Logger.e(TAG, "writeCharacteristic>>>>" + successful);

        return successful;
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
            boolean enabled)
    {
        if (mBleAdapter == null || mBleGatt == null) {
            Logger.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        mBleGatt.setCharacteristicNotification(characteristic, enabled);

        List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
        for (BluetoothGattDescriptor descriptor : descriptors) {
            Logger.e(TAG, "descriptor==>" + descriptor.getUuid() + "\n" +
                          Utils.bytesToHexString(descriptor.getValue()));
        }

        BluetoothGattDescriptor descriptor =
                characteristic.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBleGatt.writeDescriptor(descriptor);
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBleGatt == null) return null;
        return mBleGatt.getServices();
    }

    public void close() {
        if (mBleGatt == null) {
            return;
        }
        mBleGatt.close();
        mBleGatt = null;
    }

}
