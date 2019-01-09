package com.tangluo.dlib.demo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;

import com.tangluo.dlib.R;
import com.tangluo.dlib.natives.Constants;
import com.tangluo.dlib.natives.FaceDetector;
import com.tangluo.dlib.natives.FaceRecognition;
import com.tangluo.dlib.natives.ImageUtils;
import com.tangluo.dlib.natives.VisionDetectRect;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TrackerActivity extends AppCompatActivity {
    private static final  String TAG = "TrackerActivity";
    private String mCameraId;
    /**
     * Orientation of the camera sensor
     */
    private int mSensorOrientation;

    private static final int INPUT_SIZE = 224;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;
    private CaptureRequest.Builder mRequestBuilder;
    private Size mPreviewSize;
    private int mPreviewWidth = 0;
    private int mPreviewHeight = 0;
    private int mImageWidth = 0;
    private int mImageHeight = 0;

    private AutoFitTextureView mTextureView;
    private SurfaceHolder mSurfaceHolder;
    private ImageReader mImageReader;
    private Handler mFaceDetectHandler;
    private HandlerThread mFaceDetectThread;
    private FaceDetector mFaceDetector;
    private FaceRecognition mFaceRecognition;
    private boolean isComputing = false;
    private int isPreprocessed = 0;
    private FaceDetectRunable mDetectRunable;

    private byte[][] mYUVBytes;
    private int[] mRGBBytes = null;
    private Bitmap mRGBframeBitmap = null;
    private Bitmap mCroppedBitmap = null;
    private Paint mFaceLandmarkPaint;
    private float mScaleFactor = 1;
    private float mTranslateX;
    private float mTranslateY;
    private static final boolean SAVE_PREVIEW_BITMAP = false;

    private boolean isRecognition;

    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_view);
        mTextureView = findViewById(R.id.camera_texture);
        SurfaceView mFaceLandmarkView = findViewById(R.id.face_landmark);
        mSurfaceHolder = mFaceLandmarkView.getHolder();
        mFaceLandmarkView.setZOrderOnTop(true);
        mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);
        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        isRecognition = getIntent().getBooleanExtra("Recognizing", true);

        if(!isRecognition){
            if(mFaceDetector == null){
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mFaceDetector = new FaceDetector(Constants.getFaceShapeModel());
                        Log.i(TAG, "FaceDetector initialized");
                    }
                }).start();
            }
        }else {
            if(mFaceRecognition == null){
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mFaceRecognition = new FaceRecognition(Constants.getFaceShapeModel(), Constants.getRecognitionModel(), 0.45f);
                        isPreprocessed = mFaceRecognition.preprocess(Constants.getPreprocessImage());
                        Log.i(TAG, "FaceRecognition initialized");
                    }
                }).start();
            }
        }
        mDetectRunable = new FaceDetectRunable();

        mFaceLandmarkPaint = new Paint();
        mFaceLandmarkPaint.setColor(Color.RED);
        mFaceLandmarkPaint.setStyle(Paint.Style.STROKE);
        mFaceLandmarkPaint.setStrokeWidth(4.0f);
    }

    TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            Log.i(TAG, "SurfaceTexture width: " + width + " height: "  + height);
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader imageReader) {
            Image image = imageReader.acquireLatestImage();
            if(image == null){
                return;
            }
            if(mFaceDetector == null && mFaceRecognition == null){
                image.close();
                return;
            }
            if(mFaceRecognition != null && isPreprocessed == 0){
                image.close();
                return;
            }
            if(isComputing){
                image.close();
                return;
            }
            isComputing = true;

            Image.Plane[] planes = image.getPlanes();

            if (mImageWidth != image.getWidth() || mImageHeight != image.getHeight()){
                mImageWidth = image.getWidth();
                mImageHeight = image.getHeight();
                Log.i(TAG, String.format("Image size %dx%d", mImageWidth, mImageHeight));
                mRGBBytes = new int[mImageWidth * mImageHeight];
                mRGBframeBitmap = Bitmap.createBitmap(mImageWidth, mImageHeight, Bitmap.Config.RGB_565);
                mCroppedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE,  Bitmap.Config.RGB_565);

                mYUVBytes = new byte[planes.length][];
                for (int i = 0; i < planes.length; ++i){
                    mYUVBytes[i] = new byte[planes[i].getBuffer().capacity()];
                }
            }

            for (int i = 0; i < planes.length; ++i){
                planes[i].getBuffer().get(mYUVBytes[i]);
            }

            final int yRowStride = planes[0].getRowStride();
            final int uvRowStride = planes[1].getRowStride();
            final int uvPixelStride = planes[1].getPixelStride();

            ImageUtils.convertYUV420ToARGB8888(mYUVBytes[0], mYUVBytes[1], mYUVBytes[2],
                    mRGBBytes, mImageWidth, mImageHeight, yRowStride, uvRowStride, uvPixelStride, false);
            image.close();
            mRGBframeBitmap.setPixels(mRGBBytes, 0, mImageWidth, 0 , 0, mImageWidth, mImageHeight);
            drawResizedBitmap(mRGBframeBitmap, mCroppedBitmap);

            if(SAVE_PREVIEW_BITMAP){
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ImageUtils.saveBitmap(mCroppedBitmap);
                    }
                }).start();
            }

            mFaceDetectHandler.post(mDetectRunable);
        }
    };

    /**
     * Retrieves the image orientation from the specified screen rotation.
     *
     * @param rotation The screen rotation.
     * @return The image orientation (one of 0, 90, 270, and 360)
     */
    private int getOrientation(int rotation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }

    private void drawResizedBitmap(final Bitmap src, final Bitmap dst) {
        int mScreenRotation = 90;
        Display Orient = getWindowManager().getDefaultDisplay();
        Point point = new Point();
        Orient.getSize(point);
        int screen_width = point.x;
        int screen_height = point.y;
        int displayRotation = getWindowManager().getDefaultDisplay().getRotation();
        Log.d(TAG, String.format("screen size (%d,%d), rotation %d", screen_width, screen_height, displayRotation));
        if (screen_width < screen_height) {
            mScreenRotation = getOrientation(Surface.ROTATION_0);
        } else {
            mScreenRotation = getOrientation(Surface.ROTATION_90);
        }

        Assert.assertEquals(dst.getWidth(), dst.getHeight());
        final float minDim = Math.min(src.getWidth(), src.getHeight());

        final Matrix matrix = new Matrix();

        // We only want the center square out of the original rectangle.
        mTranslateX = -Math.max(0, (src.getWidth() - minDim) / 2);
        mTranslateY = -Math.max(0, (src.getHeight() - minDim) / 2);
        matrix.preTranslate(mTranslateX, mTranslateY);

        mScaleFactor = dst.getHeight() / minDim;
        matrix.postScale(mScaleFactor, mScaleFactor);
        Log.d(TAG, "Resize Bitmap TranslateX:" + mTranslateX + " TranslateY:" + mTranslateY
                + " ScaleFactor:" + mScaleFactor + " Image Rotation:" + mScreenRotation);

        // Rotate around the center if necessary.
        if (mScreenRotation != 0) {
            matrix.postTranslate(-dst.getWidth() / 2.0f, -dst.getHeight() / 2.0f);
            matrix.postRotate(mScreenRotation);
            matrix.postTranslate(dst.getWidth() / 2.0f, dst.getHeight() / 2.0f);
        }

        final Canvas canvas = new Canvas(dst);
        canvas.drawBitmap(src, matrix, null);
    }

    public void requestCameraPermission(){
        try {
            int mReadPermission = ActivityCompat.checkSelfPermission(this, "android.permission.CAMERA");
            if(mReadPermission != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 200);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    protected CameraDevice.StateCallback mSateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            startCameraPreivew();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            cameraDevice.close();
            mCameraDevice = null;
        }
    };

    protected void openCamera(int width, int height){
        requestCameraPermission();
        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        CameraManager mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            mCameraManager.openCamera(mCameraId, mSateCallback, null);
        }catch (CameraAccessException e){
            e.printStackTrace();
        }
    }

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private void setUpCameraOutputs(int width, int height){
        CameraManager mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : mCameraManager.getCameraIdList()){
                CameraCharacteristics mCharacteristics = mCameraManager.getCameraCharacteristics(cameraId);
                Integer facing = mCharacteristics.get(CameraCharacteristics.LENS_FACING);
                if(facing != CameraCharacteristics.LENS_FACING_FRONT){
                    continue;
                }
                StreamConfigurationMap mMap = mCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                assert mMap != null;

                // For still image captures, we use the largest available size.
                Size largest = Collections.max(
                        Arrays.asList(mMap.getOutputSizes(ImageFormat.YUV_420_888)),
                        new CompareSizesByArea());
                Log.i(TAG, "YUV_420_888 largest size: " + largest.getWidth() + "x" + largest.getHeight());
                int displayRotation = getWindowManager().getDefaultDisplay().getRotation();
                mSensorOrientation = mCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                Log.i(TAG, "Camera sensor orientation: " +  mSensorOrientation + "; displayRotation: " + displayRotation);
                boolean swappedDimensions = false;
                switch (displayRotation){
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if(mSensorOrientation == 90 || mSensorOrientation == 270){
                            swappedDimensions = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if(mSensorOrientation == 0 || mSensorOrientation == 180){
                            swappedDimensions = true;
                        }
                        break;
                    default:
                        Log.e(TAG, "Display rotation is invalid: " + displayRotation);
                }

                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;

                if(swappedDimensions){
                    rotatedPreviewWidth = height;
                    rotatedPreviewHeight = width;
                }

                mPreviewSize = chooseOptimalSize(mMap.getOutputSizes(SurfaceTexture.class),
                        rotatedPreviewWidth, rotatedPreviewHeight);
                mPreviewWidth = mPreviewSize.getWidth();
                mPreviewHeight = mPreviewSize.getHeight();

                // We fit the aspect ratio of TextureView to the size of preview we picked.
                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    mTextureView.setAspectRatio(
                            mPreviewSize.getWidth(), mPreviewSize.getHeight());
                } else {
                    mTextureView.setAspectRatio(
                            mPreviewSize.getHeight(), mPreviewSize.getWidth());
                }
                mCameraId = cameraId;
                mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.YUV_420_888, 2);
                Log.i(TAG, "Preview size: " + mPreviewSize.getWidth() + "x" + mPreviewSize.getHeight());
                mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, null);
                return;
            }
        }catch (CameraAccessException e){
            e.printStackTrace();
        }
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param width  The width of the texture view relative to sensor coordinate
     * @param height The height of the texture view relative to sensor coordinate
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int width, int height){
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        for (final Size option : choices){
            if(width > height) {
                if (option.getWidth() >= width && option.getHeight() >= height){
                    bigEnough.add(option);
                }
            }else {
                if(option.getWidth() >= height && option.getHeight() >= width){
                    bigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // first of those size.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        if (null == mTextureView || null == mPreviewSize ) {
            return;
        }
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    protected void startCameraPreivew(){
        SurfaceTexture mTexture = mTextureView.getSurfaceTexture();
        assert mTexture != null;
        mTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface mSurface = new Surface(mTexture);
        try {
            mRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mRequestBuilder.addTarget(mSurface);
            mRequestBuilder.addTarget(mImageReader.getSurface());
            mCameraDevice.createCaptureSession(Arrays.asList(mSurface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if(null == mCameraDevice){
                        return;
                    }
                    mCaptureSession = cameraCaptureSession;
                    try {
                        mRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        mRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                        mCaptureSession.setRepeatingRequest(mRequestBuilder.build(), null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            }, null);

        }catch (CameraAccessException e){
            e.printStackTrace();
        }
    }

    private void startFaceDetectThread(){
        mFaceDetectThread = new HandlerThread("FaceDetectThread");
        mFaceDetectThread.start();
        mFaceDetectHandler = new Handler(mFaceDetectThread.getLooper());

    }

    private void stopFaceDetecThread(){
        mFaceDetectThread.quitSafely();
        try {
            mFaceDetectThread.join();
            mFaceDetectThread = null;
            mFaceDetectHandler = null;
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    private class FaceDetectRunable implements Runnable {
        @Override
        public void run() {
            long startTime = System.currentTimeMillis();
            List<VisionDetectRect> rects;;
            synchronized (FaceDetectRunable.class) {
                if(!isRecognition){
                    rects = mFaceDetector.detect(mCroppedBitmap);
                }else {
                    rects = mFaceRecognition.recognize(mCroppedBitmap);
                }
            }
            long endTime = System.currentTimeMillis();
            float costTime = (endTime - startTime)/1000f;
            Log.i(TAG, "Time cost: " + String.valueOf(costTime) + " sec");

            Canvas canvas = mSurfaceHolder.lockCanvas();
            float xScale = canvas.getWidth() * 1.0f/ mPreviewHeight;
            float yScale = canvas.getHeight() *1.0f/ mPreviewWidth;
            Log.d(TAG, "Showing rectangle surface widht: " + canvas.getWidth() + " height: " + canvas.getHeight()
                       + " previewWidth: " + mPreviewWidth + " previewHeight: " + mPreviewHeight
                       + " xScale: " + xScale + " yScale: " + yScale);
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            if (rects != null && rects.size() > 0){
                for (VisionDetectRect rect : rects){
                    Rect mFaceBounds = new Rect();
                    //front facing camera
                    mFaceBounds.left = (int) ((INPUT_SIZE - rect.getRight())/ mScaleFactor);
                    mFaceBounds.top = (int)((rect.getTop() / mScaleFactor - mTranslateX) );
                    mFaceBounds.right = (int)((INPUT_SIZE - rect.getLeft()) / mScaleFactor);
                    mFaceBounds.bottom = (int)((rect.getBottom() / mScaleFactor - mTranslateX) );
                    canvas.drawRect(mFaceBounds, mFaceLandmarkPaint);
                    if(rect.getName() != null){
                        mFaceLandmarkPaint.setTextSize(40);
                        canvas.drawText(rect.getName(), mFaceBounds.left, mFaceBounds.bottom + 40, mFaceLandmarkPaint);
                    }
                }
            }
            mSurfaceHolder.unlockCanvasAndPost(canvas);
            isComputing = false;
        }
    }

    private void closeCamera(){
        Log.i(TAG, "Close camera");
        if(null != mCaptureSession){
            mCaptureSession.close();
            mCaptureSession = null;
        }

        if(null != mCameraDevice){
            mCameraDevice.close();
            mCameraDevice = null;
        }

        if(null != mImageReader){
            mImageReader.close();
            mImageReader = null;
        }
    }

    private void closeDetector(){
        Log.i(TAG, "Close detector");
        if(null != mFaceDetector){
            mFaceDetector.release();
            mFaceDetector = null;
        }

        if(null != mFaceRecognition){
            mFaceRecognition.release();
            mFaceRecognition = null;
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        startFaceDetectThread();

        if(mTextureView.isAvailable()){
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        }else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    protected void onPause() {
        closeCamera();
        closeDetector();
        stopFaceDetecThread();
        super.onPause();
    }
}
