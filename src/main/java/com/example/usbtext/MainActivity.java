package com.example.usbtext;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private UsbUtils utils;
    // 开始采集心电命令
    public static final byte[] START_ECG_COLLECT = {(byte)0x55, (byte)0xaa, 0x0f, 0x22,0x00,0x00,
            0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x30};
    // 停止采集命令
    public static final byte[] STOP_COLLECT = {(byte) 0x55,(byte) 0xaa, 0x0f, 0x33,0x00,0x00,
            0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x41};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        utils=UsbUtils.newInstence(this.getApplicationContext(),"4292","40963");

        Button btn= (Button) findViewById(R.id.btn);
        Button stop= (Button) findViewById(R.id.btn_stop);
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                utils.stopData(STOP_COLLECT, new UsbUtils.StopListener() {
                    @Override
                    public void stop() {
                        Log.e("taf","stop");
                    }
                });
            }
        });
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                utils.getUsbPermission();
                utils.getData(START_ECG_COLLECT, 37, new UsbUtils.GetDataListener() {
                    @Override
                    public void getDataSuccess(byte[] data) {
                        Log.e("tag","get data succ"+data.length);
                    }

                    @Override
                    public void getDataFaile(int i) {
                        Log.e("tag","get data faile"+i);
                    }
                });
            }
        });

    }
}
