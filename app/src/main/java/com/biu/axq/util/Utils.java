package com.biu.axq.util;

import android.content.Context;
import android.content.pm.PackageManager;

/**
 * <p>Author: Huajian Jiang
 * <br>Date: 2017/7/31
 * <br>Email: developer.huajianjiang@gmail.com
 */
public class Utils {
    public static boolean checkSupportBT(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
    }

    public static boolean checkSupportBLE(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    public static String bytesToHexString(byte[] data) {
        if (data == null || data.length == 0) return "";
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte byteChar : data) {
            sb.append(String.format("%02x", byteChar));
        }
        return sb.toString();
    }
}
