package com.example.wanghanxiang.screenrecordtest.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;

import java.io.File;

/**
 * Created by wanghanxiang on 2019/6/30.
 */

public class FileUtil {

    public static boolean createSDFile(String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }

        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
            return true;
        }
        return true;
    }

    //刷新数据库
    public static Uri fileScanVideo(Context context, String videoPath, int videoWidth, int videoHeight,
                                    int videoTime) {

        File file = new File(videoPath);
        if (file.exists()) {

            Uri uri = null;

            long size = file.length();
            String fileName = file.getName();
            long dateTaken = System.currentTimeMillis();

            ContentValues values = new ContentValues(11);
            values.put(MediaStore.Video.Media.DATA, videoPath); // path;
            values.put(MediaStore.Video.Media.TITLE, fileName); // title;
            values.put(MediaStore.Video.Media.DURATION, videoTime * 1000); // time
            values.put(MediaStore.Video.Media.WIDTH, videoWidth); // width
            values.put(MediaStore.Video.Media.HEIGHT, videoHeight); // height
            values.put(MediaStore.Video.Media.SIZE, size); // size;
            values.put(MediaStore.Video.Media.DATE_TAKEN, dateTaken); // date;
            values.put(MediaStore.Video.Media.DISPLAY_NAME, fileName);// filename;
            values.put(MediaStore.Video.Media.DATE_MODIFIED, dateTaken / 1000);// modify time;
            values.put(MediaStore.Video.Media.DATE_ADDED, dateTaken / 1000); // add time;
            values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");

            ContentResolver resolver = context.getContentResolver();

            if (resolver != null) {
                try {
                    uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
                } catch (Exception e) {
                    e.printStackTrace();
                    uri = null;
                }
            }

            if (uri == null) {
                MediaScannerConnection.scanFile(context, new String[]{videoPath}, new String[]{"video/*"}, new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri) {

                    }
                });
            }

            return uri;
        }

        return null;
    }

}
