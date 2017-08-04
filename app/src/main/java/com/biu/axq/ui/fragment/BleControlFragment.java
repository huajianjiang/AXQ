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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.biu.axq.R;
import com.biu.axq.service.BleService;
import com.biu.axq.util.Constants;
import com.biu.axq.util.Logger;
import com.biu.axq.util.Msgs;
import com.biu.axq.util.Utils;
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

    private BleService mService;
    private BluetoothDevice mDevice;
    private BluetoothGattCharacteristic mRWNCharacteristic;

    private boolean mNotify = true;

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
        Views.find(root, R.id.tv_write).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        switch (v.getId()) {
            case R.id.tv_write:
                writeCharacteristic();
                break;

            default:
                break;
        }
    }

    private void writeCharacteristic() {
        String command = mInput.getText().toString().trim();
        if (TextUtils.isEmpty(command)) {
            Msgs.shortToast(getContext(), "请先输入 HEX 指令");
            return;
        }
        if (mService != null && mRWNCharacteristic != null) {
            //发送 HEX 指令，异步的写入成功回调后立刻读取
            byte[] value = Utils.hexStringToBytes(command);
            if (value != null) {
                //先禁用通知，防止返回的数据混乱写入命令后的返回的结果
                mService.setCharacteristicNotification(mRWNCharacteristic, false);
                mService.writeCharacteristic(mRWNCharacteristic, value);
            } else {
                Msgs.shortToast(getContext(), "输入格式错误，请输入 HEX 命令格式");
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getContext().registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
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


    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        View notifyRoot =
                getActivity().getLayoutInflater().inflate(R.layout.menu_ble_control, null);
        final Switch notifySwitch = Views.find(notifyRoot, R.id.notifySwitch);
        notifySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mNotify = isChecked;
                if (mService != null && mRWNCharacteristic != null) {
                    Logger.e(TAG, "notify>>>>" + notifySwitch.isChecked());
                    mService.setCharacteristicNotification(mRWNCharacteristic,
                                                           notifySwitch.isChecked());
                }
            }
        });
        menu.findItem(R.id.action_notify).setActionView(notifyRoot);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.ble_control, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_clear:
                mOutput.setText("");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void startConnecting() {
        if (mService != null) {
            Logger.e(TAG, "start connecting");
            mService.connect(mDevice.getAddress());
            showPostLoading();
        }
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
            mRWNCharacteristic = notifyCharacteristic;
            mService.setCharacteristicNotification(notifyCharacteristic, true);
        } else {
            Msgs.shortToast(getContext(), "无服务");
        }
    }

    private void displayData(int type, String data) {
        String prefix = "";
        if (type != -1) {
            switch (type) {
                case BleService.READ:
                    prefix = "Read: ";
                    break;
                case BleService.WRITE:
                    prefix = "Write: ";
                    break;
                case BleService.NOTIFY:
                    prefix = "Notify: ";
                    break;
            }
        }
        mOutput.getEditableText().insert(0, prefix + data + "\n\n");
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BleService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BleService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BleService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BleService.ACTION_COMMAND_STATE);
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
                dismissPostLoading();
            } else if (BleService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Msgs.shortToast(getContext(), "已断开连接");
                dismissPostLoading();
            } else if (BleService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mService.getSupportedGattServices());
            } else if (BleService.ACTION_DATA_AVAILABLE.equals(action)) {
                int eventType = intent.getIntExtra(BleService.EXTRA_EVENT_TYPE, -1);
                displayData(eventType, intent.getStringExtra(BleService.EXTRA_DATA));

            } else if (BleService.ACTION_COMMAND_STATE.equals(action)) {
                int eventType = intent.getIntExtra(BleService.EXTRA_EVENT_TYPE, -1);
                boolean successful = intent.getBooleanExtra(BleService.EXTRA_COMMAND_STATE, false);
                if (eventType == BleService.WRITE) {
                    if (successful) {
                        mService.readCharacteristic(mRWNCharacteristic);
                    } else {
                        Msgs.shortToast(getContext(), "写入失败");
                        if (mNotify) {
                            Logger.i(TAG, "-------------ResumeNotify-------------");
                            mService.setCharacteristicNotification(mRWNCharacteristic, true);
                        }
                    }
                } else if (eventType == BleService.READ) {
                    if (!successful) {
                        Msgs.shortToast(getContext(), "读取失败");
                    }
                    if (mNotify) {
                        Logger.i(TAG, "-------------ResumeNotify-------------");
                        mService.setCharacteristicNotification(mRWNCharacteristic, true);
                    }
                } else if (eventType == BleService.NOTIFY) {
                    if (!successful) {
                        Msgs.shortToast(getContext(), "通知失败");
                    }
                }
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
            startConnecting();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };


}
