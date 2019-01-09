/*
 * jni_pedestrian_det.cpp using google-style
 *
 *  Created on: Oct 20, 2015
 *      Author: Tzutalin
 *
 *  Copyright (c) 2015 Tzutalin. All rights reserved.
 *
 *  Modified by tangluo on Oct 30, 2018
 */

#include <jni.h>
#include <detector.h>
#include <jni_common/jni_common.h>
#include <jni_common/jni_bitmap2array2d.h>
//#include <jni_common/jni_bitmap2mat.h>

extern JNI_VisionDetectRect* g_pJNI_VisionDetectRect;

namespace {

    #define JAVA_NULL 0
    using DetectorPtr = DLibHOGFaceDetector*;

    class JNI_FaceDetector {
    public:
        JNI_FaceDetector(JNIEnv* env){
            jclass clazz = env->FindClass(CLASSNAME_FACE_DETECTOR);
            mNativeContext = env->GetFieldID(clazz, NATIVE_FACE_DETECTOR_CONTEXT, "J");
            env->DeleteLocalRef(clazz);
        }

        DetectorPtr getDetectorPtrFromJava(JNIEnv* env, jobject thiz){
            DetectorPtr const p = (DetectorPtr)env->GetLongField(thiz, mNativeContext);
            return p;
        }

        void setDetectorPtrToJava(JNIEnv* env, jobject thiz, jlong ptr){
            env->SetLongField(thiz, mNativeContext, ptr);
        }

        jfieldID mNativeContext;
    };

    std::mutex gLock;

    std::shared_ptr<JNI_FaceDetector> getJNI_FaceDetector(JNIEnv* env){
        static std::once_flag mOnceInitFlag;
        static std::shared_ptr<JNI_FaceDetector> mJNI_FaceDetector;
        std::call_once(mOnceInitFlag, [env](){
            mJNI_FaceDetector = std::make_shared<JNI_FaceDetector>(env);
        });
        return mJNI_FaceDetector;
    }

    DetectorPtr const getDetectorPtr(JNIEnv* env, jobject thiz){
        std::lock_guard<std::mutex> lock(gLock);
        return getJNI_FaceDetector(env)->getDetectorPtrFromJava(env, thiz);
    }

    void setDetectorPtr(JNIEnv* env, jobject thiz, DetectorPtr newPtr){
        std::lock_guard<std::mutex> lock(gLock);
        DetectorPtr oldPtr = getJNI_FaceDetector(env)->getDetectorPtrFromJava(env, thiz);
        if(oldPtr != JAVA_NULL){
            DLOG(INFO) << "Delete old detector ptr : " << oldPtr;
            delete oldPtr;
        }
        if(newPtr != JAVA_NULL){
            DLOG(INFO) << "Set new detector ptr : " << newPtr;
        }
        getJNI_FaceDetector(env)->setDetectorPtrToJava(env, thiz, (jlong)newPtr);
    }
}

#ifdef __cplusplus
extern "C" {
#endif

#define DLIB_JNI(NAME) Java_com_tangluo_dlib_natives_FaceDetector_##NAME

    jobjectArray getDetectResult(JNIEnv* env, DetectorPtr detectorPtr, const int& size){
        jobjectArray  jDetectRectArray = JNI_VisionDetectRect::createDetectRectObjectArray(env, size);
        for (int i = 0; i < size; ++i) {
            jobject jDetectRect = JNI_VisionDetectRect::createDetectRectObject(env);
            env->SetObjectArrayElement(jDetectRectArray, i, jDetectRect);
            dlib::rectangle rect = detectorPtr->getResult()[i];
            g_pJNI_VisionDetectRect->setRect(env, jDetectRect, rect.left(), rect.top(), rect.right(), rect.bottom());
            g_pJNI_VisionDetectRect->setLable(env, jDetectRect, "face");
            std::unordered_map<int, dlib::full_object_detection>& faceShapeMap = detectorPtr->getFaceShapeMap();
            if(faceShapeMap.find(i) != faceShapeMap.end()){
                dlib::full_object_detection shape = faceShapeMap[i];
                for (unsigned long j = 0; j < shape.num_parts(); ++j) {
                    int x = shape.part(j).x();
                    int y = shape.part(j).y();
                    g_pJNI_VisionDetectRect->addLandmark(env, jDetectRect, x, y);
                }
            }
        }
        return jDetectRectArray;
    }

JNIEXPORT jobjectArray JNICALL
DLIB_JNI(jniBitmapDetect)(JNIEnv* env, jobject thiz, jobject bitmap){
        LOG(INFO) << "JNI Bitmap Face Detect called";
//        cv::Mat rgbaMat;
//        cv::Mat bgrMat;
//        jniutils::ConvertBitmapToRGBAMat(env, bitmap, rgbaMat, true);
//        cv::cvtColor(rgbaMat, bgrMat, cv::COLOR_RGBA2BGR);
        dlib::array2d<dlib::rgb_pixel> pixels;
        jniutils::ConvertBitmapToArray2d(env, bitmap, pixels);
        DetectorPtr detectorPtr = getDetectorPtr(env, thiz);
        int size = detectorPtr->detect(pixels);
        return getDetectResult(env, detectorPtr, size);
    }

JNIEXPORT jobjectArray JNICALL
DLIB_JNI(jniDetect)(JNIEnv* env, jobject thiz, jstring imgPath){
    const char* img_path = env->GetStringUTFChars(imgPath, 0);
    DetectorPtr detectorPtr = getDetectorPtr(env, thiz);
    LOG(INFO) << "JNI Face detect called";
    int size = detectorPtr->detect(std::string(img_path));
    env->ReleaseStringUTFChars(imgPath, img_path);
    return getDetectResult(env, detectorPtr, size);

}

JNIEXPORT jint JNICALL
DLIB_JNI(jniInit)(JNIEnv* env, jobject thiz, jstring jLandMarkModel){
    std::string landMarkModel = jniutils::convertJStrToString(env, jLandMarkModel);
    LOG(INFO) << "Create DLIB HOG face detector";
    DetectorPtr detectorPtr = new DLibHOGFaceDetector(landMarkModel);
    setDetectorPtr(env, thiz, detectorPtr);
    LOG(INFO) << "JNI Face detect init called";
    return JNI_OK;
}

JNIEXPORT jint JNICALL
DLIB_JNI(jniRelease)(JNIEnv* env, jobject thiz) {
    setDetectorPtr(env, thiz, JAVA_NULL);
    return JNI_OK;
}

#ifdef __cplusplus
}
#endif