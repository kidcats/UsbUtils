package com.example.usbtext;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.util.Log;

import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by kidca on 2016/12/14
 * 1.监听USB连接插拔状态用UsbManager.ACTION_USB_DEVICE_ATTACHED在广播中动态监听
 * 2.无法解决 线程重新回调问题,即退出后重新开始没法记住usbdeviceconnect 的状态
 */
public class UsbUtils {

    private static final String TAG = "UsbUtils";
    public static final String ACTION_DEVICE_PERMISSION = "com.linc.USB_PERMISSION";
    /**
     * 获得USB的状态,与USB进行通信
     */
    private static UsbManager usbManager;
    /**
     * Usb设备的抽象，它包含一个或多个UsbInterface，而每个UsbInterface包含多个UsbEndpoint。
     * Host与其通信，先打开UsbDeviceConnection，使用UsbRequest在一个端点（endpoint）发送和接收数据。
     */
    private static UsbDevice usbDevice;

    /**
     * 定义了设备的功能集，一个UsbDevice包含多个UsbInterface，每个Interface都是独立的
     */
    private  UsbInterface usbInterface;

    /**
     * endpoint是interface的通信通道,这个是写数据节点
     */
    private static UsbEndpoint outEndpoint;//


    /**
     * 这个是读数据节点
     */
    private static UsbEndpoint inEndpoint;

    /**
     * host与device建立的连接，并在endpoint传输数据
     */
    private  UsbDeviceConnection deviceConnection;

    /**
     * usb 请求包。可以在UsbDeviceConnection上异步传输数据。
     * 注意是只在异步通信时才会用到它。
     */
    private  UsbRequest usbrequest;

    /**
     * usb常量的定义，对应Linux/usb/ch9.h
     */
    private  UsbConstants constants;


    private  Context mContext;

    /**
     * USB设备信息
     */
    private static String productId;
    private static String vendorId;

    /**
     * 所有的USB设备
     */
    private  HashMap<String, UsbDevice> devices;

    /**
     * 用于获取usb权限的PendingIntent
     */
    private static PendingIntent mPermissionIntent;

    /**
     * 记住是否已经获取权限
     */
    private static boolean isAllow=false;


    private static UsbDeviceConnection usbDeviceConnection;

    private ExecutorService executorService;

    private boolean readable;


    //单例模式创建一个Utils,用static 来保证线程同步
    private static class SingletonHolder {
        private static final UsbUtils INSTENCE = new UsbUtils();
    }

    private UsbUtils() {
    }


    public static final UsbUtils newInstence(Context context, String vendorId, String productId) {
        SingletonHolder.INSTENCE.mContext = context;
        SingletonHolder.INSTENCE.vendorId = vendorId;
        SingletonHolder.INSTENCE.productId = productId;
        SingletonHolder.INSTENCE.executorService=Executors.newFixedThreadPool(3);
        return SingletonHolder.INSTENCE;
    }

    /**
     * 初始化usbmanager
     */
    private  void initUsbManager() {
        usbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
    }

    /**
     * 获取USB所有的连接设备
     *
     * @return hashMap
     */
    public  HashMap<String, UsbDevice> getUsbDevices() {
        if (usbManager == null) {
            initUsbManager();
        }
        devices = usbManager.getDeviceList();
        return devices;
    }

    /**
     * 获取USB设备
     */
    public  void findUsbDevice() {
        if (devices == null) {
            devices = getUsbDevices();
        }
        Iterator<UsbDevice> iterator = devices.values().iterator();
        while (iterator.hasNext()) {
            UsbDevice device = iterator.next();
            if (checkDevice(device)) {
                usbDevice = device;
            }
        }
        if (checkDevice(usbDevice)) {
            Log.e(TAG, "get the usbDevice");
        } else {
            Log.e(TAG, "can not find the usbDevice,check out your usb");
        }
    }

    /**
     * 获取USB通信权限
     */
    public  void getUsbPermission() {
        findUsbDevice();
        if (usbDevice != null) {
            if (checkDevice(usbDevice)) {
                if (usbManager.hasPermission(usbDevice)) {
                    Log.e(TAG, "this usbDevice  had the Premission");
                } else {
                    Log.e(TAG, "the usbDevice  had not the Premission");
                    mPermissionIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_DEVICE_PERMISSION), 0);
                    IntentFilter filter = new IntentFilter(ACTION_DEVICE_PERMISSION);
                    mContext.registerReceiver(mUsbReceiver, filter);
                    usbManager.requestPermission(usbDevice, mPermissionIntent);
                }
            }
        }
    }


    /**
     * 开始通信
     */
    public  void startConnect() {
        //通信前的准备工作
        getUsbPermission();
        readable=true;
        if (usbDevice != null) {
            int interfaceCount=usbDevice.getInterfaceCount();
            Log.e(TAG, "interfaceCount"+interfaceCount);
            for (int interfaceIndex = 0; interfaceIndex < interfaceCount; interfaceIndex++) {
                UsbInterface uInterface = usbDevice.getInterface(interfaceIndex);
                if ((UsbConstants.USB_CLASS_CDC_DATA != uInterface.getInterfaceClass())
                        && (UsbConstants.USB_CLASS_COMM != uInterface.getInterfaceClass())) {
                    continue;
                }
                usbInterface=uInterface;
            }


            if (usbInterface!=null){
                for (int i = 0; i < usbInterface.getEndpointCount(); i++) {
                    UsbEndpoint ep = usbInterface.getEndpoint(i);
                    if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                        if (ep.getDirection() == UsbConstants.USB_DIR_OUT) {
                            outEndpoint = ep;
                            Log.e(TAG,"OUT endPoint  "+i);
                        } else {
                            inEndpoint = ep;
                            Log.e(TAG,"IN endPoint  "+i);
                        }
                    }
                }
                if ((null == inEndpoint) || (null == outEndpoint)) {
                    inEndpoint = null;
                    outEndpoint = null;
                    usbInterface = null;
                }
                usbDeviceConnection = usbManager.openDevice(usbDevice);
            }else {
                Log.e(TAG,"usbInterface is null");
            }
        }
    }

    /**
     * 开始异步获取数据
     * @param startCommand 开始命令
     * @param length 数据长度
     * @param listener 回调函数
     */
    public  void getData(byte[] startCommand, final int length, final GetDataListener listener){
        final byte[] data=new byte[length];
        //先传入开始命令
        Log.e("getdata", "startcommand");
        startConnect();
        if (usbDeviceConnection!=null){
            usbDeviceConnection.claimInterface(usbInterface, true);
            int i = usbDeviceConnection.bulkTransfer(outEndpoint, startCommand, startCommand.length, 3000);
            Log.e("getdata", "start result"  + i);
        }
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                Log.e("getdata", "start");
                while (readable){
                    if (inEndpoint != null && outEndpoint != null && usbDeviceConnection != null) {
                        int result = usbDeviceConnection.bulkTransfer(inEndpoint, data, length, 3000);
                        Log.e("getdata", "collect result"  + result);
                        if (result == length) {
                            listener.getDataSuccess(data);
                        } else {
                            listener.getDataFaile(result);
                        }
                    } else {
                        Log.e(TAG, "OUT or IN  endPoint or connect  is null");
                    }
                }
            }
        });
    }

    /**
     * 停止获取
     * @param stopCommand
     * @param listener
     */
    public  void stopData(byte[] stopCommand,StopListener listener){
        if (outEndpoint!=null&&usbDeviceConnection!=null){
            int result=usbDeviceConnection.bulkTransfer(outEndpoint,stopCommand,stopCommand.length,1500);
            if (result>0){
                listener.stop();
            }
        }
        readable=false;
    }


    /**
     * 检测是否为需要的UsbDevice
     * @param device
     * @return
     */
    private  boolean checkDevice(UsbDevice device){
        if (device==null){
            return false;
        }
        return productId.equals(String.valueOf(device.getProductId()))&&vendorId.equals(String.valueOf(device.getVendorId()));
    }



    /**
     * 用于接受是否给予权限信息
     */
    private static final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_DEVICE_PERMISSION)) {
                Log.e(TAG+"threadreceiver","receiver");
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            Log.e(TAG, "usb EXTRA_PERMISSION_GRANTED");
                            isAllow=true;
                        }
                    } else {
                        Log.e(TAG, "usb EXTRA_PERMISSION_GRANTED null!!!");
                    }
                }
            }
        }
    };


    interface  GetDataListener{
        /**
         * 读取成功
         * @param data 读取成功返回的数据
         */
        void getDataSuccess(byte[] data);

        /**
         * 读取失败
         * @param i 读取失败返回的读取长度
         */
        void getDataFaile(int i);
    }

    interface  StopListener{
        void stop();
    }
}
