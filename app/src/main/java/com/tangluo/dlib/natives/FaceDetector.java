package com.tangluo.dlib.natives;


import android.graphics.Bitmap;
import android.support.annotation.Keep;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

/**
 * Created by houzhi on 16-10-20.
 * Modified by tzutalin on 16-11-15
 * Modified by tangluo on 18-10-30
 */

public class FaceDetector {
    private static final String TAG = "native-dlib";
    private long mNativeFaceDetectorContext;
    private String mLandMarkModel = "";

    static {
        try {
            System.loadLibrary("dlib_jni");
            Log.d(TAG, "android_dlib jni library loaded");
        }catch (UnsatisfiedLinkError e){
            Log.e(TAG, "android_dlib jni library not found");
        }
    }

    @SuppressWarnings("unused")
    public FaceDetector(){
        jniInit(mLandMarkModel);
    }

    public  FaceDetector(String landMarkModel) {
        mLandMarkModel = landMarkModel;
        jniInit(mLandMarkModel);
    }

    public List<VisionDetectRect> detect(String image){
        VisionDetectRect[] mRects = jniDetect(image);
        return Arrays.asList(mRects);
    }

    public List<VisionDetectRect> detect(Bitmap bitmap){
        VisionDetectRect[] mRects = jniBitmapDetect(bitmap);
        return Arrays.asList(mRects);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        release();
    }

    public void release(){
        jniRelease();
    }


    @Keep
    private synchronized native int jniInit(String landMarkModel);

    @Keep
    private synchronized native VisionDetectRect[] jniDetect(String image);

    @Keep
    private synchronized native VisionDetectRect[] jniBitmapDetect(Bitmap bitmap);

    @Keep
    private synchronized native int jniRelease();
}