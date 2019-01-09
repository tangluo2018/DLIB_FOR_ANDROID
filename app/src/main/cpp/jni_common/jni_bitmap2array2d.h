//
// Created by huangmh on 11/16/18.
//

#ifndef DLIB_FOR_ANDROID_JNI_BITMAP2ARRAY2D_H
#define DLIB_FOR_ANDROID_JNI_BITMAP2ARRAY2D_H

#include <android/bitmap.h>
#include <dlib/array2d.h>

namespace jniutils {

    void ConvertBitmapToArray2d(JNIEnv* env, jobject& bitmap, dlib::array2d<dlib::rgb_pixel>& dst);

}
#endif //DLIB_FOR_ANDROID_JNI_BITMAP2ARRAY2D_H
