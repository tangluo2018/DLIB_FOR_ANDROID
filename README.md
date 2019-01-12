# DLIB_FOR_ANDROID
A Android app demo with dlib face recongnition

## Build & Install
* Git pull the source code and import into AndroidStudio as a Android project, then you can build it in the AndroidStudio IDE.

* Edit file local.propertise to set your own ndk.dir and sdk.dir

* Connect your Android device to the PC, build the project and the app demo will be installed into your device automatically.

## Usage
Connect your device to PC

1. **Enter your device**
```
adb shell
```

2. **Create dlib directory**
```
mkdir -p /sdcard/dlib/faces
```

3. **Exit the device then push necessary files**
 ```
 adb push shape_predictor_5_face_landmarks.dat /sdcard/dlib
 adb push dlib_face_recognition_resnet_model_v1.dat /sdcard/dlib
 ```
 
4. **Push the face photo that you want it to be tracking/recongnized**
 ```
 adb push xxx /sdcard/faces
 ```

You can find the *.dat files in dat directory.

## Features
This project is based on [dlib-android](https://github.com/tzutalin/dlib-android), and dlib-android project depends on a third party library, the OpenCV-android-sdk. OpenCV was removed from this project and png/jpeg are supported default.

* Shared library was prebuilt default, find it app/libs/armeabi-v7a/libdlib.so.

* PNG and JEPG are supported default in dlib Shared library.

## Thanks

* [dlib-android](https://github.com/tzutalin)
* [face_recongnition](https://github.com/ageitgey/face_recognition)
