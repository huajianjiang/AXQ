package com.biu.axq.adapter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.TextView;

import com.biu.axq.R;
import com.github.huajianjiang.baserecyclerview.widget.ArrayAdapter;
import com.github.huajianjiang.baserecyclerview.widget.BaseAdapter;
import com.github.huajianjiang.baserecyclerview.widget.BaseViewHolder;

/**
 * <p>Author: Huajian Jiang
 * <br>Date: 2017/8/1
 * <br>Email: developer.huajianjiang@gmail.com
 */
public class BleAdapter extends ArrayAdapter<BaseViewHolder, BluetoothDevice> {

    public BleAdapter(Context ctxt) {
        super(ctxt);
    }

    @Override
    public BaseViewHolder onBuildViewHolder(ViewGroup parent, int viewType) {
        return new BaseViewHolder(getLayoutInflater().inflate(R.layout.item_ble, parent, false)) {
        };
    }

    @Override
    public void onPopulateViewHolder(BaseViewHolder holder, int position) {
        BluetoothDevice device = getItem(position);
        ((TextView)holder.getView(R.id.name)).setText(device.getName());
        ((TextView)holder.getView(R.id.mac)).setText(device.getAddress());
    }

    @Override
    public void insert(BluetoothDevice item) {
        if (!getItems().contains(item)) {
            super.insert(item);
        }
    }
}
