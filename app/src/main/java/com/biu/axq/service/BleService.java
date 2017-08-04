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
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;


import com.biu.axq.BuildConfig;
import com.biu.axq.util.Logger;
import com.biu.axq.util.Utils;
import com.github.huajianjiang.baserecyclerview.util.Predications;

import java.util.List;
import java.util.UUID;

import retrofit2.http.PUT;

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

    public static final int READ = 0;
    public static final int WRITE = 1;
    public static final int NOTIFY = 2;

    public final static String ACTION_GATT_CONNECTED =
            BuildConfig.APPLICATION_ID + ".ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            BuildConfig.APPLICATION_ID + ".ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            BuildConfig.APPLICATION_ID + ".ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            BuildConfig.APPLICATION_ID + ".ACTION_DATA_AVAILABLE";

    public final static String ACTION_COMMAND_STATE =
            BuildConfig.APPLICATION_ID + ".ACTION_COMMAND_STATE";

    public final static String EXTRA_DATA = BuildConfig.APPLICATION_ID + ".EXTRA_DATA";
    public final static String EXTRA_EVENT_TYPE = BuildConfig.APPLICATION_ID + ".EXTRA_EVENT_TYPE";
    public final static String EXTRA_COMMAND_STATE = BuildConfig.APPLICATION_ID + ".EXTRA_COMMAND_STATE";

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

    private Handler mCommandHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case READ:
                    if (!readCharacteristicInternal((BluetoothGattCharacteristic) msg.obj)) {
                        Logger.e(TAG,"readCharacteristicInternal>>>>>failed");
                        broadcastUpdate(ACTION_COMMAND_STATE, READ, false);
                    }
                    break;
                case WRITE:
                    byte[] value = msg.getData().getByteArray(EXTRA_DATA);
                    if (!writeCharacteristicInternal((BluetoothGattCharacteristic) msg.obj,
                                                     value))
                    {
                        Logger.e(TAG, "writeCharacteristicInternal>>>>>failed");
                        broadcastUpdate(ACTION_COMMAND_STATE, WRITE, false);
                    }
                    break;
                case NOTIFY:
                    boolean enable = msg.getData().getBoolean(EXTRA_DATA, false);
                    if (!setCharacteristicNotificationInternal(
                            (BluetoothGattCharacteristic) msg.obj, enable))
                    {
                        broadcastUpdate(ACTION_COMMAND_STATE, NOTIFY, false);
                    }
                    break;
            }
        }
    };

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
            boolean successful = status == BluetoothGatt.GATT_SUCCESS;
            Logger.e(TAG, "onCharacteristicRead");
            if (successful) {
                Logger.i(TAG, "onCharacteristicRead Successful");
                broadcastUpdate(ACTION_DATA_AVAILABLE, READ, characteristic);
            }
            broadcastUpdate(ACTION_COMMAND_STATE, READ, successful);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic, int status)
        {
            boolean successful = status == BluetoothGatt.GATT_SUCCESS;
            Logger.e(TAG, "onCharacteristicWrite");
            if (successful) {
                Logger.i(TAG, "onCharacteristicWrite Successful");
                broadcastUpdate(ACTION_DATA_AVAILABLE, WRITE, characteristic);
            } else {
                Logger.w(TAG, "onCharacteristicWrite received: " + status);
            }
            broadcastUpdate(ACTION_COMMAND_STATE, WRITE, successful);
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
            broadcastUpdate(ACTION_DATA_AVAILABLE, NOTIFY, characteristic);
        }

    };


    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,int type, boolean state) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_COMMAND_STATE, state);
        intent.putExtra(EXTRA_EVENT_TYPE, type);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, int type,
            final BluetoothGattCharacteristic characteristic)
    {
        final Intent intent = new Intent(action);
        String extra = Utils.bytesToHexString(characteristic.getValue());
        intent.putExtra(EXTRA_DATA, extra);
        intent.putExtra(EXTRA_EVENT_TYPE, type);
        Logger.e(TAG, "response = >" + extra);
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

    public void readCharacteristic(BluetoothGattCharacteristic characteristic)
    {
        mCommandHandler.obtainMessage(READ, characteristic).sendToTarget();
    }

    private boolean readCharacteristicInternal(BluetoothGattCharacteristic characteristic)
    {
        if (mBleAdapter == null || mBleGatt == null) {
            Logger.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        //内部异步处理
        return mBleGatt.readCharacteristic(characteristic);
    }

    public void writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] value) {
        Message msg = mCommandHandler.obtainMessage(WRITE, characteristic);
        Bundle args = new Bundle();
        args.putByteArray(EXTRA_DATA, value);
        msg.setData(args);
        msg.sendToTarget();
    }

    /**
     * 修改某个服务中的某个特型属性
     * @param characteristic 特型属性
     * @param value 新值
     */
    private boolean writeCharacteristicInternal(BluetoothGattCharacteristic characteristic,
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

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
            boolean enabled)
    {
        Message msg = mCommandHandler.obtainMessage(NOTIFY, characteristic);
        Bundle args = new Bundle();
        args.putBoolean(EXTRA_DATA, enabled);
        msg.setData(args);
        msg.sendToTarget();
    }

    public boolean setCharacteristicNotificationInternal(BluetoothGattCharacteristic characteristic,
            boolean enabled)
    {
        if (mBleAdapter == null || mBleGatt == null) {
            Logger.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }

        boolean successful;

        successful = mBleGatt.setCharacteristicNotification(characteristic, enabled);

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
    }

}
