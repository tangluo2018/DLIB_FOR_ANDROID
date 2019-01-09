package com.tangluo.dlib;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.tangluo.dlib.demo.DetectorActivity;
import com.tangluo.dlib.demo.TrackerActivity;


public class MainActivity extends AppCompatActivity {
    private static final int EXTERNAL_STORAGE = 1;
    private static String[] EXTERNAL_STORAGE_PERMISSIONS = { "android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE" };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView mDetectBtn = findViewById(R.id.detect_btn);
        TextView mTrackBtn = findViewById(R.id.track_btn);
        TextView mRecognizeBtn = findViewById(R.id.recognize_btn);
        getExternalStoragePermission();

        mDetectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, DetectorActivity.class);
                startActivity(intent);
            }
        });


        mTrackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, TrackerActivity.class);
                intent.putExtra("Recognizing", false);
                startActivity(intent);
            }
        });

        mRecognizeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, TrackerActivity.class);
                intent.putExtra("Recognizing", true);
                startActivity(intent);
            }
        });
    }

    public void getExternalStoragePermission(){
        try {
            int mReadPermission = ActivityCompat.checkSelfPermission(this, "android.permission.READ_EXTERNAL_STORAGE");
            if(mReadPermission != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, EXTERNAL_STORAGE_PERMISSIONS, EXTERNAL_STORAGE);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
