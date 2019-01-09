/**
 * Modified by tangluo on 18-11-13
 */

package com.tangluo.dlib.natives;

import android.graphics.Bitmap;
import android.support.annotation.Keep;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

public class FaceRecognition {
    private static final String TAG = "native-dlib";
    private long mNativeFaceRecognitionContext;
    private String mLandmarkModel = "";
    private String mRecognitionModel = "";
    private float mTolerance;

    static {
        try {
            System.loadLibrary("dlib_jni");
            Log.d(TAG, "android_dlib jni library loaded");
        }catch (UnsatisfiedLinkError e){
            Log.e(TAG, "android_dlib jni library not found");
        }
    }

    public FaceRecognition(String landmarkModel, String recognitionModel, float tolerance){
        mLandmarkModel = landmarkModel;
        mRecognitionModel = recognitionModel;
        mTolerance = tolerance;
        init();
    }

    public int init(){
        return  jniInit(mLandmarkModel, mRecognitionModel, mTolerance);
    }

    public int preprocess(String preprocessImage){
        return jnipreprocess(preprocessImage);
    }

    public List<VisionDetectRect> recognize(Bitmap bitmap){
        VisionDetectRect[] mRects = jniBitmapRecognize(bitmap);
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
    private synchronized native int jniInit(String landMarkModel, String recognitonModel, float tolerance);

    @Keep
    private synchronized native int jnipreprocess(String preprocessImage);

    @Keep
    private synchronized native VisionDetectRect[] jniBitmapRecognize(Bitmap bitmap);

    @Keep
    private synchronized native int jniRelease();
}
