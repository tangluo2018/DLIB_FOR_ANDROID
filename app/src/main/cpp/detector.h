//
// Created by tangluo on 10/29/18.
//

#ifndef DLIB_FOR_ANDROID_DETECTOR_H
#define DLIB_FOR_ANDROID_DETECTOR_H

#pragma once

#include <dutil.h>
#include <unordered_map>
#include <jni_common/jni_fileutils.h>
#include <jni_common/jni_utils.h>
#include <dlib/image_processing.h>
#include <dlib/image_processing/frontal_face_detector.h>
//#include <dlib/image_processing/render_face_detections.h>
#include <dlib/image_loader/load_image.h>
#include <dlib/image_transforms/interpolation.h>
//#include <dlib/opencv/cv_image.h>
//#include <opencv2/core/core.hpp>
//#include <opencv2/imgproc/imgproc.hpp>
//#include <opencv2/opencv.hpp>
#include <glog/logging.h>

class DLibHOGDetector {
private:
    typedef dlib::scan_fhog_pyramid<dlib::pyramid_down<6>> image_scanner_type;
    dlib::object_detector<image_scanner_type> mObjectDetector;

    inline void init(){
        LOG(INFO) <<"Model path: " << mModelPath;
        if(jniutils::fileExists(mModelPath)){
            dlib::deserialize(mModelPath) >> mObjectDetector;
        }else {
            LOG(INFO) << "Not exist " << mModelPath;
        }
    }

public:
    DLibHOGDetector() {}

    DLibHOGDetector(const std::string& modelPath)
            : mModelPath(modelPath) {
        init();
    }

//    virtual inline int detect(const std::string& path){
//        using namespace jniutils;
//        if(!fileExists(mModelPath) || !fileExists(path)){
//            std::cout << "No model or input file" << std::endl;
//            return 0;
//        }
//        cv::Mat src_img = cv::imread(path, CV_LOAD_IMAGE_COLOR);
//        if(src_img.empty()){
//            return 0;
//        }
//        int img_width = src_img.cols;
//        int img_height = src_img.rows;
//        int img_size_min = MIN(img_width, img_height);
//        int img_size_max = MAX(img_width, img_height);
//
//        float  scale = float(INPUT_IMG_MIN_SIZE) / float(img_size_min);
//        if(scale * img_size_max > INPUT_IMG_MAX_SIZE){
//            scale = (float)INPUT_IMG_MAX_SIZE / (float)img_size_max;
//        }
//
//        if(scale != 1.0){
//            cv::Mat outputMat;
//            cv::resize(src_img, outputMat, cv::Size(img_width * scale, img_height * scale));
//            src_img = outputMat;
//        }
//
//        dlib::cv_image<dlib::bgr_pixel> cimg(src_img);
//
//        double thresh = 0.45;
//        mRets = mObjectDetector(cimg, thresh);
//        return mRets.size();
//    }

    inline std::vector<dlib::rectangle> getResult() {return mRets;}

    virtual ~DLibHOGDetector() {}

protected:
    std::vector<dlib::rectangle> mRets;
    std::string mModelPath;
    const int INPUT_IMG_MAX_SIZE = 800;
    const int INPUT_IMG_MIN_SIZE = 600;
};

/*
 * DLib face detect and face feature extractor
 */
class DLibHOGFaceDetector : public DLibHOGDetector {
private:
    std::string mLandMarkModel;
    dlib::shape_predictor msp;
    std::unordered_map<int, dlib::full_object_detection> mFaceShapeMap;
    dlib::frontal_face_detector mFaceDetector;

    inline void init() {
        LOG(INFO) << "Init mFaceDetector";
        mFaceDetector = dlib::get_frontal_face_detector();
    }

public:
    DLibHOGFaceDetector() {init();}

    DLibHOGFaceDetector(const std::string& landmarkmodel)
            : mLandMarkModel(landmarkmodel){
        init();
        if(!mLandMarkModel.empty() && jniutils::fileExists(mLandMarkModel)){
            dlib::deserialize(mLandMarkModel) >> msp;
            LOG(INFO) << "Load landmark model data: " << mLandMarkModel;
        } else {
            LOG(INFO) << "Can't not find model data: " << mLandMarkModel;
        }
    }

    virtual inline  int detect(const std::string& image){
        LOG(INFO) << "Load_image: " << image;
        try {
            dlib::array2d<dlib::rgb_pixel> img;
            dlib::load_image(img, image);
            LOG(INFO) << "Input image width: " << img.nc() << ", height: " << img.nr();
//        dlib::pyramid_up(img);
            mRets = mFaceDetector(img);
            LOG(INFO) << "DLIB HOG Face detected size : " << mRets.size();
            return mRets.size();
        }catch (std::exception& e){
            LOG(INFO) << e.what();
        }
        return 0;
    }

    virtual inline int detect(const dlib::array2d<dlib::rgb_pixel>& image){
        if (image.size() == 0){
            return 0;
        }
        mRets.clear();
        mFaceShapeMap.clear();
        mRets = mFaceDetector(image);
        LOG(INFO) << mRets.size() <<" face(s) detected!";
        if (mRets.size() != 0 && !mLandMarkModel.empty()){
            for (int i = 0; i < mRets.size(); ++i) {
                dlib::full_object_detection shape = msp(image, mRets[i]);
                mFaceShapeMap[i] = shape;
            }
        }
        return mRets.size();
    }

//    virtual inline int detect(const cv::Mat& image){
//        if(image.empty())
//            return 0;
//
//        if(image.channels() == 1){
//            cv::cvtColor(image, image, CV_GRAY2BGR);
//        }
//        CHECK(image.channels() == 3);
//        //if(image.channels() > 1){
//        //    cv::cvtColor(image, image, CV_BGR2GRAY);
//        //}
//        cv::Mat resized_image;
//        double  scale = 80.0/MIN_FACE_SIZE;
//        cv::resize(image,resized_image, cv::Size(), scale, scale);
//
//        LOG(INFO) << "DLIB Face detecting ...";
//        dlib::cv_image<dlib::bgr_pixel> img(resized_image);
//        //dlib::array2d<unsigned char> image_gray;
//        //dlib::assign_image(image_gray, img);
//
//        //dlib::cv_image<unsigned char> img(resized_image);
//
//        mRets = mFaceDetector(img);
//        LOG(INFO) << "DLIB HOG Faces detected: " << mRets.size();
//        mFaceShapeMap.clear();
//
//        if(mRets.size() != 0 && !mLandMarkModel.empty()){
//            for (unsigned long i = 0; i < mRets.size(); ++i) {
//                dlib::full_object_detection shape = msp(img, mRets[i]);
//                LOG(INFO) << "face index:" << i
//                          << "number of parts: " << shape.num_parts();
//                mFaceShapeMap[i] = shape;
//            }
//        }
//        return mRets.size();
//    }

    std::unordered_map<int, dlib::full_object_detection>& getFaceShapeMap(){
        return mFaceShapeMap;
    }

protected:
    const int MIN_FACE_SIZE = 80;
};

#endif //DLIB_FOR_ANDROID_DETECTOR_H
