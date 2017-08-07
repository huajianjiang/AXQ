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
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;


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
public class BleService {
    private static final String TAG = BleService.class.getSimpleName();

    public final static String UUID_SERVER = "0000fff0-0000-1000-8000-00805f9b34fb";
    public final static String UUID_NOTIFY = "0000fff6-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    public static final int STATE_DISCONNECTED = -1;
    public static final int STATE_DISCONNECTING = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;

    public static final int READ = 0;
    public static final int WRITE = 1;
    public static final int NOTIFY = 2;

    private Handler mBleHandler;
    public static final int MSG_ON_GATT_CONNECTION_STATE_CHANGED = 0;
    public static final int MSG_ON_CHARACTERISTIC_RESULT_AVAILABLE = 1;
    public static final int MSG_ON_GATT_SERVICES_DISCOVERED = 2;

    private Context mContext;
    private BluetoothManager mBleManager;
    private BluetoothAdapter mBleAdapter;
    private BluetoothGatt mBleGatt;
    private int mConnState = STATE_DISCONNECTED;
    private String mBleAddr;

    public BleService(Handler bleHandler, Context context) {
        mBleHandler = bleHandler;
        mContext = context;
    }

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            int gattConnState = STATE_DISCONNECTED;
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTING:
                    gattConnState = STATE_CONNECTING;
                    break;
                case BluetoothProfile.STATE_CONNECTED:
                    gattConnState = STATE_CONNECTED;
                    // Attempts to discover services after successful connection.
                    Logger.i(TAG, "Attempting to start service discovery:" +
                                  mBleGatt.discoverServices());
                    break;
                case BluetoothProfile.STATE_DISCONNECTING:
                    gattConnState = STATE_DISCONNECTING;
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    gattConnState = STATE_DISCONNECTED;
                    break;
            }
            notifyUpdate(MSG_ON_GATT_CONNECTION_STATE_CHANGED, gattConnState);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Logger.e(TAG, "onServicesDiscovered");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                notifyUpdate(MSG_ON_GATT_SERVICES_DISCOVERED, 0);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic, int status)
        {
            Logger.e(TAG, "onCharacteristicRead");
            notifyUpdate(MSG_ON_CHARACTERISTIC_RESULT_AVAILABLE, READ, status, characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic, int status)
        {
            Logger.e(TAG, "onCharacteristicWrite");
            notifyUpdate(MSG_ON_CHARACTERISTIC_RESULT_AVAILABLE, WRITE, status, characteristic);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                int status)
        {
            Logger.e(TAG,"**************onDescriptorWrite****************");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Logger.i(TAG, "onDescriptorWrite Successful");
            } else {
                Logger.w(TAG, "onDescriptorWrite received: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic)
        {
            Logger.e(TAG, "onCharacteristicChanged");
            notifyUpdate(MSG_ON_CHARACTERISTIC_RESULT_AVAILABLE, NOTIFY, BluetoothGatt.GATT_SUCCESS,
                         characteristic);
        }

    };


    private void notifyUpdate(final int what,final int status) {
        mBleHandler.obtainMessage(what, status, -1).sendToTarget();
    }

    private void notifyUpdate(final int what, int type, int status,
            final BluetoothGattCharacteristic characteristic)
    {
        String extra = Utils.bytesToHexString(characteristic.getValue());
        mBleHandler.obtainMessage(what, type, status, extra).sendToTarget();
    }

    public boolean init() {
        if (mBleManager == null) {
            mBleManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
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
        mBleGatt = device.connectGatt(mContext, false, mGattCallback);

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

    public boolean readCharacteristic(BluetoothGattCharacteristic characteristic)
    {
        if (mBleAdapter == null || mBleGatt == null) {
            Logger.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        //内部异步处理
        return mBleGatt.readCharacteristic(characteristic);
    }

    /**
     * 修改某个服务中的某个特型属性
     * @param characteristic 特型属性
     * @param value 新值
     */
    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic,
            byte[] value)
    {
        if (mBleAdapter == null || mBleGatt == null) {
            Logger.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }

        boolean successful = characteristic.setValue(value);
        if (successful) {
            Logger.e(TAG, "writeCharacteristic>>>> loacal successful ");
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            //内部异步处理
            successful = mBleGatt.writeCharacteristic(characteristic);
        }
        Logger.e(TAG, "writeCharacteristic>>>>" + successful);
        return successful;
    }

    public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
            boolean enabled)
    {
        if (mBleAdapter == null || mBleGatt == null) {
            Logger.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }

        boolean successful = mBleGatt.setCharacteristicNotification(characteristic, enabled);

        List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
        for (BluetoothGattDescriptor descriptor : descriptors) {
            Logger.e(TAG, "descriptor==>" + descriptor.getUuid() + "\n" +
                          Utils.bytesToHexString(descriptor.getValue()));
        }

        if (enabled) {
            BluetoothGattDescriptor descriptor =
                    characteristic.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
            if (descriptor != null) {
                successful = descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                if (successful) {
                    //内部异步处理
                    successful = mBleGatt.writeDescriptor(descriptor);
                }
            }
        }

        Logger.e(TAG, "setCharacteristicNotification>>>>" + successful);

        return successful;
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
        mBleHandler = null;
    }

}
