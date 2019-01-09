//
// Created by huangmh on 10/30/18.
//

#ifndef DLIB_FOR_ANDROID_JNI_COMMON_H
#define DLIB_FOR_ANDROID_JNI_COMMON_H

#include <jni.h>

#define CLASSNAME_FACE_DETECTOR "com/tangluo/dlib/natives/FaceDetector"
#define NATIVE_FACE_DETECTOR_CONTEXT "mNativeFaceDetectorContext"
#define CLASSNAME_FACE_RECOGNITION "com/tangluo/dlib/natives/FaceRecognition"
#define NATIVE_FACE_RECOGNITION_CONTEXT "mNativeFaceRecognitionContext"

#define CLASSNAME_VISION_DETECT_RECT "com/tangluo/dlib/natives/VisionDetectRect"


class JNI_VisionDetectRect {
public:
    JNI_VisionDetectRect(JNIEnv* env){
        jclass rectClass = env->FindClass(CLASSNAME_VISION_DETECT_RECT);

        jLabel = env->GetFieldID(rectClass, "mLabel", "Ljava/lang/String;");
        jName = env->GetFieldID(rectClass, "mName", "Ljava/lang/String;");
        jConfidence = env->GetFieldID(rectClass, "mConfidence", "F");
        jLeft = env->GetFieldID(rectClass, "mLeft", "I");
        jRight = env->GetFieldID(rectClass, "mRight", "I");
        jTop = env->GetFieldID(rectClass, "mTop", "I");
        jBottom = env->GetFieldID(rectClass, "mBottom", "I");
        jAddLandmark = env->GetMethodID(rectClass, "addLandmark", "(II)Z");
    }

    void setLable(JNIEnv* env, jobject& jDetectRect, const std::string& label){
        jstring jstr = (jstring)env->NewStringUTF(label.c_str());
        env->SetObjectField(jDetectRect, jLabel, (jobject)jstr);
    }

    void setName(JNIEnv* env, jobject& jDetectRect, const std::string& name){
        jstring jstr = (jstring)env->NewStringUTF(name.c_str());
        env->SetObjectField(jDetectRect, jName, (jobject)jstr);
    }

    void setRect(JNIEnv* env, jobject& jDetectRect, const int& left, const int& top, const int& right, const int& bottom){
        env->SetIntField(jDetectRect, jLeft, left);
        env->SetIntField(jDetectRect, jTop, top);
        env->SetIntField(jDetectRect, jRight, right);
        env->SetIntField(jDetectRect, jBottom, bottom);
    }

    void addLandmark(JNIEnv* env, jobject& jDetectRect, const int& x, const int& y){
        env->CallBooleanMethod(jDetectRect, jAddLandmark, x, y);
    }

    static jobject createDetectRectObject(JNIEnv* env){
        jclass rectClass = env->FindClass(CLASSNAME_VISION_DETECT_RECT);
        jmethodID jInit = env->GetMethodID(rectClass, "<init>", "()V");
        return env->NewObject(rectClass, jInit);
    }

    static jobjectArray createDetectRectObjectArray(JNIEnv* env, const int& size){
        jclass rectClass = env->FindClass(CLASSNAME_VISION_DETECT_RECT);
        return (jobjectArray)env->NewObjectArray(size, rectClass, NULL);
    }

private:
    jfieldID jLabel;
    jfieldID jName;
    jfieldID jConfidence;
    jfieldID jLeft;
    jfieldID jRight;
    jfieldID jTop;
    jfieldID jBottom;
    jmethodID jAddLandmark;
};

#endif //DLIB_FOR_ANDROID_JNI_COMMON_H
