package com.biu.axq.ui.fragment;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
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
import com.biu.axq.data.BleDevice;
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
    private BleDevice mDevice;
    private BluetoothGattCharacteristic mRWNCharacteristic;

    private boolean mNotify = true;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mDevice = (BleDevice) getArguments().getSerializable(Constants.KEY_ENTITY);
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

        if (mNotify) {
            mService.setCharacteristicNotification(mRWNCharacteristic, false);
        }

        if (!mService.writeCharacteristic(mRWNCharacteristic, Utils.hexStringToBytes(command))) {
            resumeNotifyIfNeed();
        }
    }


    @Override
    protected void onFirstShow() {
        mService = new BleService(new BleHandler(), getContext());
        if (!mService.init()) {
            Msgs.shortToast(getContext(), "无法初始化蓝牙!");
            getActivity().finish();
        }
        startConnecting();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopConnecting();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mService != null) {
            mService.close();
            mService = null;
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        View notifyRoot =
                getActivity().getLayoutInflater().inflate(R.layout.menu_ble_control, null);

        final Switch notifySwitch = Views.find(notifyRoot, R.id.notifySwitch);
        notifySwitch.setChecked(mNotify);

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
            mService.connect(mDevice.address);
            showPostLoading();
        }
    }

    private void stopConnecting() {
        if (mService != null) {
            Logger.e(TAG, "stop connecting");
            mService.disconnect();
            dismissPostLoading();
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


    private void resumeNotifyIfNeed() {
        if (mNotify) {
            Logger.i(TAG, "-------------ResumeNotify-------------");
            mService.setCharacteristicNotification(mRWNCharacteristic, true);
        }
    }

    private class BleHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BleService.MSG_ON_GATT_CONNECTION_STATE_CHANGED:
                    onGattConnectionStateChanged(msg.arg1);
                    break;
                case BleService.MSG_ON_GATT_SERVICES_DISCOVERED:
                    onGattServicesDiscovered();
                    break;
                case BleService.MSG_ON_CHARACTERISTIC_RESULT_AVAILABLE:
                    onCharacteristicResultAvailable(msg.arg1, msg.arg2, msg.obj);
                    break;
                default:
                    break;
            }
        }
    }

    private void onCharacteristicResultAvailable(int type, int status, Object data) {
        final boolean successful = status == BluetoothGatt.GATT_SUCCESS;
        if (successful) {
            displayData(type, (String) data);
            if (type == BleService.WRITE) {
                mService.readCharacteristic(mRWNCharacteristic);
            } else if (type == BleService.READ) {
                resumeNotifyIfNeed();
            }
        } else {
            String typeStr = "";
            switch (type) {
                case BleService.READ:
                    typeStr = "READ";
                    resumeNotifyIfNeed();
                    break;
                case BleService.WRITE:
                    typeStr = "WRITE";
                    resumeNotifyIfNeed();
                    break;
                case BleService.NOTIFY:
                    typeStr = "NOTIFY";
                    break;
            }
            Logger.w(TAG, "Command exe failed>>>>" + typeStr);
            Msgs.shortToast(getContext(), typeStr + "失败");
        }
    }

    private void onGattServicesDiscovered() {
        displayGattServices(mService.getSupportedGattServices());
    }

    private void onGattConnectionStateChanged(int status) {
        switch (status) {
            case BleService.STATE_CONNECTING:
                break;
            case BleService.STATE_CONNECTED:
                Msgs.shortToast(getContext(), "连接成功");
                dismissPostLoading();
                break;
            case BleService.STATE_DISCONNECTING:
                break;
            case BleService.STATE_DISCONNECTED:
                Msgs.shortToast(getContext(), "已断开连接");
                dismissPostLoading();
                break;
        }
    }


}
