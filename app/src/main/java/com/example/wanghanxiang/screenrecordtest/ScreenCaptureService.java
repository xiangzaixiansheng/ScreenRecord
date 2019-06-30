package com.example.wanghanxiang.screenrecordtest;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.wanghanxiang.screenrecordtest.utils.FileUtil;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by wanghanxiang on 2019/6/30.
 */

public class ScreenCaptureService  extends Service {
    public WindowManager.LayoutParams wmParams;
    //创建浮动窗口设置布局参数的对象
    WindowManager mWindowManager;
    public LinearLayout mFloatLayout;
    public TextView mFloatTextView,mFloatTimeTextView;
    private final String RecordScreenPath = "/RecordScreen/";
    private String mRecordFilePath;
    private long baseTimer;
    private MediaRecorder mMediaRecorder;
    //计时标识位
    private boolean opemTimer=true;
    private boolean closeTimer=false;
    java.util.Timer  mTimer=new Timer();
    //视频的参数
    private int mRecordWidth;
    private int mRecordHeight;
    private int mScreenDpi;

    //录制运行标志位
    private boolean mIsRunning=false;

    private MediaProjectionManager mProjectionManager;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;

    private int mResultCode;
    private Intent mResultData ;
    //录制时间
    private int mRecordSeconds=0;



    private static final String TAG = "ScreenCaptureService";

    @Override
    public void onCreate() {
        super.onCreate();
        mMediaRecorder = new MediaRecorder();
        //createFloatView();
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        //return null;
        return  new RecordBinder();
    }

    //通信使用
    public class RecordBinder extends Binder {
        public ScreenCaptureService getRecordService() {
            return ScreenCaptureService.this;
        }
    }

    private Handler handler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            mFloatTimeTextView.setText((String)msg.obj);

        }
    };

    public void createFloatView() {

        initWindows();
        LayoutInflater inflater = LayoutInflater.from(getApplication());
        //获取浮动窗口视图所在布局
        mFloatLayout = (LinearLayout) inflater.inflate(R.layout.floatlayout, null);
        //添加mFloatLayout
        mWindowManager.addView(mFloatLayout, wmParams);

        mFloatTextView = (TextView) mFloatLayout.findViewById(R.id.record_textview);
        mFloatTimeTextView = (TextView) mFloatLayout.findViewById(R.id.time_textview);


        mFloatLayout.measure(View.MeasureSpec.makeMeasureSpec(0,
                View.MeasureSpec.UNSPECIFIED), View.MeasureSpec
                .makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        //计时器
        changeTimer(opemTimer);
        //获得屏幕信息
        getScreen();

        setUpMediaRecorder();
        createVirtualDisplay();
        //开始录制屏幕
        mMediaRecorder.start();
        mIsRunning=true;

        mFloatLayout.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // TODO Auto-generated method stub
                //getRawX是触摸位置相对于屏幕的坐标，getX是相对于按钮的坐标
                wmParams.x = (int) event.getRawX() - mFloatTextView.getMeasuredWidth() / 2;
                //减25为状态栏的高度
                wmParams.y = (int) event.getRawY() - mFloatTextView.getMeasuredHeight() / 2 - 25;
                //刷新
                mWindowManager.updateViewLayout(mFloatLayout, wmParams);
                return false;  //此处必须返回false，否则OnClickListener获取不到监听
            }
        });

        mFloatTextView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                changeTimer(closeTimer);
                Toast.makeText(ScreenCaptureService.this,getResources().getText(R.string.stop_record), Toast.LENGTH_SHORT).show();
                //停止录像
                stopRecord();
            }
        });

    }


    //获取屏幕高度
    public void getScreen(){
        DisplayMetrics metric = new DisplayMetrics();
        WindowManager mWindowManager = (WindowManager) this.getApplication().getSystemService(Context.WINDOW_SERVICE);
        mWindowManager.getDefaultDisplay().getMetrics(metric);
        mRecordWidth = metric.widthPixels;
        //获取像素
        mRecordHeight = metric.heightPixels;
        //获取dpi
        mScreenDpi= metric.densityDpi;

    }

    //初始化window对象
    public void initWindows(){
        wmParams = new WindowManager.LayoutParams();
        //获取的是WindowManagerImpl.CompatModeWrapper
        mWindowManager = (WindowManager) getApplication().getSystemService(getApplication().WINDOW_SERVICE);
        //设置window type
        // wmParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        wmParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        //设置图片格式，效果为背景透明
        wmParams.format = PixelFormat.RGBA_8888;
        //设置浮动窗口不可聚焦
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        //调整悬浮窗显示的停靠位置为左侧置顶
        wmParams.gravity = Gravity.LEFT | Gravity.TOP;
        // 以屏幕左上角为原点，设置x、y初始值，相对于gravity
        wmParams.x = 0;
        wmParams.y = 0;

        //设置悬浮窗口长宽数据
        wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
    }

    public void changeTimer(boolean status){
        //计时
        baseTimer = SystemClock.elapsedRealtime();
        if(status == true){
            mTimer.scheduleAtFixedRate(new TimerTask() {
                public void run() {
                    int time = (int)((SystemClock.elapsedRealtime() - baseTimer) / 1000);
                    String hh = new DecimalFormat("00").format(time / 3600);
                    String mm = new DecimalFormat("00").format(time % 3600 / 60);
                    String ss = new DecimalFormat("00").format(time % 60);
                    String timeFormat = new String(hh + ":" + mm + ":" + ss);
                    Message msg = new Message();
                    Log.e(TAG,timeFormat);
                    msg.obj = timeFormat;
                    handler.sendMessage(msg);
                    mRecordSeconds++;
                }

            }, 0, 1000L);
        }else{
            mTimer.cancel();
        }
    }

    private void setUpMediaRecorder() {
        //设置文件名字
        mRecordFilePath = getSaveDirectory() + File.separator + System.currentTimeMillis() + ".mp4";
        if (mMediaRecorder == null) {
            mMediaRecorder = new MediaRecorder();
        }

        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setOutputFile(mRecordFilePath);
        mMediaRecorder.setVideoSize(mRecordWidth, mRecordHeight);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mMediaRecorder.setVideoEncodingBitRate((int) (mRecordWidth * mRecordHeight * 3.6));
        mMediaRecorder.setVideoFrameRate(20);

        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //外部调用初始化
    public void setResultData(int resultCode, Intent resultData) {

        mResultCode = resultCode;
        mResultData = resultData;

        Log.e(TAG,"mResultData"+mResultData);

        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (mProjectionManager != null) {
            mMediaProjection = mProjectionManager.getMediaProjection(mResultCode, mResultData);
            Log.e(TAG,"mMediaProjection" + mMediaProjection);
        }
    }


    private void createVirtualDisplay() {

        mVirtualDisplay = mMediaProjection.createVirtualDisplay("MainScreen", mRecordWidth, mRecordHeight, mScreenDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder.getSurface(), null, null);
    }

    //判断是否运行
    public boolean ismIsRunning() {
        return mIsRunning;
    }


    public String getSaveDirectory() {

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            String path = Environment.getExternalStorageDirectory().getAbsolutePath() + RecordScreenPath;
            FileUtil.createSDFile(path);
            return path;
        } else {
            return null;
        }
    }

    //停止方法
    public void stopRecord(){
        if (mMediaRecorder != null) {
            try {

                mMediaRecorder.stop();

            } catch (Exception e) {

                mMediaRecorder = null;
                mMediaRecorder = new MediaRecorder();

            } finally {

                //通知系统更新
                FileUtil.fileScanVideo(this, mRecordFilePath, mRecordWidth, mRecordHeight, mRecordSeconds);
                mVirtualDisplay.release();
                mMediaProjection.stop();
                mMediaProjection = null;
                //还原计时时间
                mRecordSeconds = 0;
                //移除消息
                handler.removeCallbacksAndMessages(null);
            }
            mMediaRecorder.reset();
            mMediaRecorder.release();
        }
        if (mFloatLayout != null) {
            //移除悬浮窗口
            mWindowManager.removeView(mFloatLayout);
        }
        //还原标识位
        mIsRunning = false;
        //调用销毁方法
        onDestroy();

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG,"onDestroy()");
    }
}
