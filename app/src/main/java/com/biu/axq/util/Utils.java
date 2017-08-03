package com.biu.axq.util;

import android.content.Context;
import android.content.pm.PackageManager;

/**
 * <p>Author: Huajian Jiang
 * <br>Date: 2017/7/31
 * <br>Email: developer.huajianjiang@gmail.com
 */
public class Utils {
    private static final String TAG = Utils.class.getSimpleName();

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

    public static byte[] hexStringToBytes(String s) {
        s = s.replaceAll(" ", "");
        Logger.e(TAG, "s==>" + s);

        for (char c : s.toCharArray()) {
            if (Character.digit(c, 16) == -1) {
                return null;
            }
        }

        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) +
                                  Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
