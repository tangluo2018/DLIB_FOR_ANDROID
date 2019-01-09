//
// Created by tangluo on 11/16/18.
//

#include <android/bitmap.h>
#include <glog/logging.h>
#include <dlib/array2d.h>
#include <jni_common/types.h>

using namespace jnicommon;

namespace jniutils {

    void ConvertBitmapToArray2d(JNIEnv* env, jobject& bitmap, dlib::array2d<dlib::rgb_pixel>& dst){
        AndroidBitmapInfo info;
        void* pixels = 0;

        if(AndroidBitmap_getInfo(env, bitmap, &info) < 0){
            LOG(INFO) << "Get Bitmap info failed";
            return;
        }
        if(info.format != ANDROID_BITMAP_FORMAT_RGBA_8888 &&
                  info.format != ANDROID_BITMAP_FORMAT_RGB_565){
            LOG(INFO) << "Wrong Bitmap format, only support RGBA_8888 and RGB_565";
            return;
        }
        if(AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0){
            LOG(INFO) << "Lock Bitmap pixels failed";
            return;
        }
        if(pixels == NULL){
            LOG(INFO) << "Bitmap pixels is NULL";
            return;
        }
        int width = info.width;
        int height = info.height;
        dst.set_size((long)height, (long)width);
        uint8* data = (uint8*)pixels;
        LOG(INFO) << "Bitmap width: " << width
                  << " height: " << height
                  << " format: " << info.format
                  << " stride: " << info.stride;
        if (info.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
            for (int i = 0; i < height; ++i) {
                for (int j = 0; j < width; ++j) {
                    uint32 * pixel = (uint32 *)(data + 4 * j);
                    float alpha = ((*pixel) & 0xFF)/255;
                    dst[i][j].red = (uint8)((((*pixel) >> 24) & 0xFF) * alpha + (1 - alpha) * 255);
                    dst[i][j].green = (uint8)((((*pixel) >> 16) & 0xFF) * alpha + (1 - alpha) * 255);
                    dst[i][j].blue = (uint8)((((*pixel) >> 8) & 0xFF) * alpha + (1 - alpha) * 255);
                }
                data = data + info.stride;
            }

        } else if(info.format == ANDROID_BITMAP_FORMAT_RGB_565) {
            for (int i = 0; i < height; ++i) {
                for (int j = 0; j < width; ++j) {
                    uint16* pixel = (uint16 *)(data + 2 * j);
                    dst[i][j].red = (uint8)((((*pixel) & 0xF800)>> 11) << 3);
                    dst[i][j].green = (uint8)((((*pixel) & 0x07E0) >> 5) << 2);
                    dst[i][j].blue = (uint8)((((*pixel) & 0x001F)) << 3);
                }
                data = data + info.stride;
            }
        }
        AndroidBitmap_unlockPixels(env, bitmap);
    }
}