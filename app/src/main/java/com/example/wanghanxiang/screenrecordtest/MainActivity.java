package com.example.wanghanxiang.screenrecordtest;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import com.example.wanghanxiang.screenrecordtest.utils.SettingUtil;

public class MainActivity extends AppCompatActivity {

    public ScreenCaptureService screenService;
    private ServiceConnection conn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(!Settings.canDrawOverlays(getApplicationContext())) {
                //启动Activity让用户授权
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent,100);
            }
        }

        setContentView(R.layout.activity_main);
        startScreenCaptureService();

    }

    //点击方法
    public void ontouch(View v) {
        //把服务对象传到工具类中
        SettingUtil.setScreenService(screenService);
        SettingUtil.startScreenRecord(MainActivity.this,200);
    }


    //启动服务
    public void startScreenCaptureService() {
        conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                ScreenCaptureService.RecordBinder ScreenSeriviceRecord = (ScreenCaptureService.RecordBinder) iBinder;
                screenService = ScreenSeriviceRecord.getRecordService();

            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {

            }
        };
        Intent intent=new Intent(MainActivity.this,ScreenCaptureService.class);
        bindService(intent,conn,BIND_AUTO_CREATE);

    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(true);
            return true;
        }else if(keyCode == KeyEvent.KEYCODE_HOME){
            moveTaskToBack(true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {

                } else {
                    Toast.makeText(this, "ACTION_MANAGE_OVERLAY_PERMISSION权限已被拒绝", Toast.LENGTH_SHORT).show();

                }
            }

        }else if(requestCode == 200){
            if(resultCode != 0){
                //初始化MediaProjection
                if (screenService != null) {
                    SettingUtil.SetUpMediaProjection(resultCode, data);
                }
                SettingUtil.setUpData();
            }

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (conn != null) {
            unbindService(conn);
            conn = null;
        }
    }
}
