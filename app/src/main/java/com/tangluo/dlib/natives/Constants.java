package com.tangluo.dlib.natives;

import android.os.Environment;

import java.io.File;

public final class Constants {
    private static String sdcard = Environment.getExternalStorageDirectory().getAbsolutePath();

    private Constants() {

    }

    public static String getFaceShapeModel(){

        return sdcard + File.separator + "dlib/shape_predictor_5_face_landmarks.dat";
    }

    public static String getRecognitionModel(){
        return sdcard + File.separator + "dlib/dlib_face_recognition_resnet_model_v1.dat";
    }

    public static String getDetectorImage(){
        return sdcard + File.separator + "dlib/lena.jpg";
    }

    public static String get68FaceShapeModel(){
        return sdcard + File.separator + "dlib/shape_predictor_68_face_landmarks.dat";
    }

    public static String getPreprocessImage(){
        return sdcard + File.separator + "dlib/faces";
    }

}
