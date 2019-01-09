package com.tangluo.dlib.demo;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.tangluo.dlib.R;
import com.tangluo.dlib.natives.Constants;
import com.tangluo.dlib.natives.FaceDetector;
import com.tangluo.dlib.natives.VisionDetectRect;
import java.util.List;

public class DetectorActivity extends AppCompatActivity {
    private final static String TAG = "DetectorActivity";
    private List<VisionDetectRect> mRects;
    private ImageView mImageView;
    private Bitmap mBitmap;
    private TextView mInitialview;
    private PopupWindow popupWindow;
    private Canvas canvas;
    private FaceDetector mFaceDetector;
    private boolean isInit = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detector);
        mImageView = findViewById(R.id.detect_img);
        Button mRectBtn = findViewById(R.id.rect_btn);
        Button mLandmarkBtn = findViewById(R.id.landmark_btn);

        new Thread(){
            @Override
            public void run() {
                mFaceDetector  = new FaceDetector(Constants.get68FaceShapeModel());
                isInit = false;
                handler.sendEmptyMessage(2);
            }
        }.start();

        mBitmap = BitmapFactory.decodeFile(Constants.getDetectorImage()).copy(Bitmap.Config.RGB_565, true);;
        canvas = new Canvas(mBitmap);

        mRectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isInit){
                    popupWaitWindow(new String("Initializing"));
                    return;
                }
                new Thread(){
                    @Override
                    public void run() {
                        mRects = mFaceDetector.detect(mBitmap);
                        Log.i(TAG, mRects.size() + " faces was found.");
                        for (VisionDetectRect rect : mRects){
                            drawFaceRect(canvas, rect);
                        }
                        handler.sendEmptyMessage(1);
                    }
                }.start();
                popupWaitWindow("Detecting");
            }
        });

        mLandmarkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isInit){
                    popupWaitWindow("Initializing");
                    return;
                }
                new Thread(){
                    @Override
                    public void run() {
                        mRects = mFaceDetector.detect(mBitmap);
                        for (VisionDetectRect rect : mRects){
                            drawFaceLandmark(canvas, rect);
                        }
                        handler.sendEmptyMessage(1);
                    }
                }.start();
                popupWaitWindow("Detecting");
            }
        });

    }

    private void popupWaitWindow(String text){
        popupWindow = new PopupWindow();
        View view = LayoutInflater.from(this).inflate(R.layout.popupwindow, null);
        mInitialview = view.findViewById(R.id.initial_txt);
        mInitialview.setText(text);
        popupWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setContentView(view);
        popupWindow.showAtLocation(getWindow().getDecorView(), Gravity.CENTER, 0, 0);
    }

    public void drawFaceLandmark(Canvas canvas, VisionDetectRect rect){
        List<Point> mLandmarks = rect.getFaceLandmarks();
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(4.0f);
        for (int i = 0; i < mLandmarks.size(); ++i){
            canvas.drawPoint(mLandmarks.get(i).x, mLandmarks.get(i).y, paint);
        }
    }

    public void drawFaceRect(Canvas canvas, VisionDetectRect rect){
        Rect mRect = new Rect(rect.getLeft(), rect.getTop(), rect.getRight(), rect.getBottom());
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(4.0f);
        canvas.drawRect(mRect, paint);
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 1:
                    mImageView.setImageBitmap(mBitmap);
                    popupWindow.dismiss();
                    break;
                case 2:
                    popupWindow.dismiss();
                default:
                    break;
            }
        }
    };

//    @Override
//    public void onResume(){
//        super.onResume();
//    }

    @Override
    protected void onPause() {
        if(null != mFaceDetector) {
            mFaceDetector.release();
        }
        super.onPause();
    }
}
