package com.example.wanghanxiang.screenrecordtest.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.util.Log;

import com.example.wanghanxiang.screenrecordtest.ScreenCaptureService;

/**
 * Created by wanghanxiang on 2019/6/30.
 */

public class SettingUtil {
    private final static String TAG = "SettingUtil";

    private static ScreenCaptureService mScreenCaptureService;


    public static void setScreenService(ScreenCaptureService screenService){
        mScreenCaptureService = screenService;
    }


    public static void startScreenRecord(Activity activity, int requestCode) {
        if (mScreenCaptureService != null && !mScreenCaptureService.ismIsRunning()) {
            MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) activity.
                    getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            if (mediaProjectionManager != null) {
                Intent intent = mediaProjectionManager.createScreenCaptureIntent();
                PackageManager packageManager = activity.getPackageManager();
                if (packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                    Log.d(TAG, "startActivityForResult start");
                    activity.startActivityForResult(intent, requestCode);
                }
            }
        }
    }


    public static void setUpData() {
        if(mScreenCaptureService != null && !mScreenCaptureService.ismIsRunning()){
            mScreenCaptureService.createFloatView();
        }
    }

    //给服务中的MediaProjection传入授权值
    public  static void SetUpMediaProjection(int resultCode, Intent resultData){
        if(mScreenCaptureService != null) {
            Log.e(TAG,"SetUpMediaProjection()");
            mScreenCaptureService.setResultData(resultCode, resultData);
        }
    }

}
