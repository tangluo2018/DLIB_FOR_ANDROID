//
// Created by tangluo on 11/13/18.
//

#include <jni.h>
#include <recognition.h>
#include <jni_common/jni_common.h>
#include <jni_common/jni_utils.h>
//#include <jni_common/jni_bitmap2mat.h>
#include <jni_common/jni_bitmap2array2d.h>

extern JNI_VisionDetectRect* g_pJNI_VisionDetectRect;

namespace {
#define JAVA_NULL 0
    using RecognitionPtr = DLibFaceRecognition*;

    class JNI_FaceRecognition {
    public:
        JNI_FaceRecognition(JNIEnv* env){
            jclass clazz = env->FindClass(CLASSNAME_FACE_RECOGNITION);
            mNativeContext = env->GetFieldID(clazz, NATIVE_FACE_RECOGNITION_CONTEXT, "J");
            env->DeleteLocalRef(clazz);
        }

        RecognitionPtr getRecognitionPtrFromJava(JNIEnv* env, jobject thiz){
            RecognitionPtr const p = (RecognitionPtr)env->GetLongField(thiz, mNativeContext);
            return p;
        }

        void setRecognitionPtrToJava(JNIEnv* env, jobject thiz, jlong ptr){
            env->SetLongField(thiz, mNativeContext, ptr);
        }

        jfieldID mNativeContext;
    };

    std::mutex gLock;

    std::shared_ptr<JNI_FaceRecognition> getJNI_FaceRecognition(JNIEnv* env){
        static std::once_flag mOnceInitFlag;
        static std::shared_ptr<JNI_FaceRecognition> mJNI_FaceRecognition;
        std::call_once(mOnceInitFlag, [env](){
            mJNI_FaceRecognition = std::make_shared<JNI_FaceRecognition>(env);
        });
        return mJNI_FaceRecognition;
    }

    RecognitionPtr const getRecognitionPtr(JNIEnv* env, jobject thiz){
        std::lock_guard<std::mutex> lock(gLock);
        return getJNI_FaceRecognition(env)->getRecognitionPtrFromJava(env, thiz);
    }

    void setRecognitionPtr(JNIEnv* env, jobject thiz, RecognitionPtr newPtr){
        std::lock_guard<std::mutex> lock(gLock);
        RecognitionPtr oldPtr = getJNI_FaceRecognition(env)->getRecognitionPtrFromJava(env, thiz);
        if(oldPtr != JAVA_NULL){
            DLOG(INFO) << "Delete old recogniton ptr : " << oldPtr;
            delete oldPtr;
        }
        if(newPtr != JAVA_NULL){
            DLOG(INFO) << "Set new recogniton ptr : " << newPtr;
        }
        getJNI_FaceRecognition(env)->setRecognitionPtrToJava(env, thiz, (jlong)newPtr);
    }
}

#ifdef __cplusplus
extern "C" {
#endif

jobjectArray getRecognitionResult(JNIEnv* env, RecognitionPtr recognitionPtr, const int& size){
    jobjectArray  jDetectRectArray = JNI_VisionDetectRect::createDetectRectObjectArray(env, size);
    for (int i = 0; i < size; ++i) {
        jobject jDetectRect = JNI_VisionDetectRect::createDetectRectObject(env);
        env->SetObjectArrayElement(jDetectRectArray, i, jDetectRect);
        dlib::rectangle rect = recognitionPtr->getRects()[i];
        std::string name = recognitionPtr->getNames()[i];
        g_pJNI_VisionDetectRect->setRect(env, jDetectRect, rect.left(), rect.top(), rect.right(), rect.bottom());
        g_pJNI_VisionDetectRect->setLable(env, jDetectRect, "face");
        g_pJNI_VisionDetectRect->setName(env, jDetectRect, name);
    }
    return jDetectRectArray;
}

#define DLIB_JNI(NAME) Java_com_tangluo_dlib_natives_FaceRecognition_##NAME

#ifdef DLIB_OPENCV_SUPPORT
    JNIEXPORT jobjectArray JNICALL
    DLIB_JNI(jniBitmapRecognize2)(JNIEnv* env, jobject thiz, jobject bitmap){
        cv::Mat rgbaMat;
        cv::Mat bgrMat;
        jniutils::ConvertBitmapToRGBAMat(env, bitmap, rgbaMat, true);
        cv::cvtColor(rgbaMat, bgrMat, cv::COLOR_RGBA2BGR);
        RecognitionPtr recognitionPtr = getRecognitionPtr(env, thiz);
        jint size = recognitionPtr->recognize(bgrMat);
        return getRecognitionResult(env, recognitionPtr, size);
    }
#endif

    JNIEXPORT jobjectArray JNICALL
    DLIB_JNI(jniBitmapRecognize)(JNIEnv* env, jobject thiz, jobject bitmap){
        dlib::array2d<dlib::rgb_pixel> pixels;
        jniutils::ConvertBitmapToArray2d(env, bitmap, pixels);
        RecognitionPtr recognitionPtr = getRecognitionPtr(env, thiz);
        jint size = recognitionPtr->recognize(pixels);
        return getRecognitionResult(env, recognitionPtr, size);
    }

    JNIEXPORT jint JNICALL
    DLIB_JNI(jniInit)(JNIEnv* env, jobject thiz, jstring jLandmarkModel, jstring jRecognitionModle, jfloat tolerance){
        std::string landMarkModel = jniutils::convertJStrToString(env, jLandmarkModel);
        std::string recognitionModel = jniutils::convertJStrToString(env, jRecognitionModle);
        RecognitionPtr recognitionPtr = new DLibFaceRecognition(landMarkModel, recognitionModel, tolerance);
        setRecognitionPtr(env, thiz, recognitionPtr);
        return JNI_OK;
    }

    JNIEXPORT jint JNICALL
    DLIB_JNI(jnipreprocess)(JNIEnv* env, jobject thiz, jstring jpreprocessImage){
    std::string preprocessImage = jniutils::convertJStrToString(env, jpreprocessImage);
    RecognitionPtr recognitionPtr = getRecognitionPtr(env, thiz);
    jint result = recognitionPtr->preprocess(preprocessImage);
    return result;
}

    JNIEXPORT jint JNICALL
    DLIB_JNI(jniRelease)(JNIEnv* env, jobject thiz) {
        setRecognitionPtr(env, thiz, JAVA_NULL);
        return JNI_OK;
    }


#ifdef __cplusplus
};
#endif