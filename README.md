# UsbUtils
this is a usb host utils for Android


1. 首先官方文档来一发
   https://developer.android.com/guide/topics/connectivity/usb/host.html
   
2.其中的坑有 usbDeviceConnection这个貌似只有在获取到权限后才能获得,否则为null

3.未完成的部分为手机无法记住权限,在退出重登后需要重新插拔以获取权限