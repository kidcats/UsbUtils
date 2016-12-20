package com.example.usbtext;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.widget.Toast;

public class MyReceiver extends BroadcastReceiver {
    private final static String TAG = "USB_STATE";
    public MyReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        UsbDevice usbDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        String deviceName = usbDevice.getDeviceName();
        Log.e(TAG,"--- 接收到广播， action: " + action);
        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
            Log.e(TAG, "USB device is Attached: " + deviceName);
            Toast.makeText(context,"USB device is Attached"+ deviceName,Toast.LENGTH_SHORT).show();
            UsbUtils.newInstence(context.getApplicationContext(),"4292","40963").getUsbPermission();
        } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
            Log.e(TAG, "USB device is Detached: " + deviceName);
            Toast.makeText(context,"USB device is Detached: "+ deviceName,Toast.LENGTH_SHORT).show();
        }
    }
}
