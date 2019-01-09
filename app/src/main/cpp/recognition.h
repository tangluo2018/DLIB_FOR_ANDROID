//
// Created by tangluo on 11/13/18.
//

#ifndef DLIB_FOR_ANDROID_RECOGNITION_H
#define DLIB_FOR_ANDROID_RECOGNITION_H

#pragma once

#include <unordered_map>
#include <jni_common/jni_fileutils.h>
#include <jni_common/jni_utils.h>
#include <dlib/image_processing.h>
#include <dlib/image_processing/frontal_face_detector.h>
#include <dlib/image_loader/load_image.h>
#include <dlib/dnn.h>
#include <glog/logging.h>

#ifdef DLIB_OPENCV_SUPPORT
//#include <dlib/opencv/cv_image.h>
//#include <opencv2/core/core.hpp>
//#include <opencv2/imgproc/imgproc.hpp>
//#include <opencv2/opencv.hpp>
#endif

using namespace dlib;
using namespace std;

template <template <int,template<typename>class,int,typename> class block, int N, template<typename>class BN, typename SUBNET>
using residual = add_prev1<block<N,BN,1,tag1<SUBNET>>>;

template <template <int,template<typename>class,int,typename> class block, int N, template<typename>class BN, typename SUBNET>
using residual_down = add_prev2<avg_pool<2,2,2,2,skip1<tag2<block<N,BN,2,tag1<SUBNET>>>>>>;

template <int N, template <typename> class BN, int stride, typename SUBNET>
using block  = BN<con<N,3,3,1,1,relu<BN<con<N,3,3,stride,stride,SUBNET>>>>>;

template <int N, typename SUBNET> using ares      = relu<residual<block,N,affine,SUBNET>>;
template <int N, typename SUBNET> using ares_down = relu<residual_down<block,N,affine,SUBNET>>;

template <typename SUBNET> using alevel0 = ares_down<256,SUBNET>;
template <typename SUBNET> using alevel1 = ares<256,ares<256,ares_down<256,SUBNET>>>;
template <typename SUBNET> using alevel2 = ares<128,ares<128,ares_down<128,SUBNET>>>;
template <typename SUBNET> using alevel3 = ares<64,ares<64,ares<64,ares_down<64,SUBNET>>>>;
template <typename SUBNET> using alevel4 = ares<32,ares<32,ares<32,SUBNET>>>;

using anet_type = loss_metric<fc_no_bias<128,avg_pool_everything<
                      alevel0<
                      alevel1<
                      alevel2<
                      alevel3<
                      alevel4<
                      max_pool<3,3,2,2,relu<affine<con<32,7,7,2,2,
                      input_rgb_image_sized<150>
                      >>>>>>>>>>>>;

class DLibFaceRecognition {

private:
    dlib::frontal_face_detector mFaceDetector;
    dlib::shape_predictor msp;
    anet_type net;
    std::string mLandmarkModel;
    std::string mRecognitionModel;
    std::string mPreprocessImage;
    float mTolerance = 0.45;

#ifdef DLIB_OPENCV_SUPPORT
    inline int detect(const cv::Mat& image){
        if(image.empty())
            return 0;

        if(image.channels() == 1){
            cv::cvtColor(image, image, CV_GRAY2BGR);
        }
        CHECK(image.channels() == 3);
        dlib::cv_image<dlib::bgr_pixel> img(image);
        LOG(INFO) << "DLIB Face detecting ...";
        std::vector<dlib::rectangle> rects = mFaceDetector(img);
        LOG(INFO) << "DLIB HOG Faces detected: " << rects.size();

        if(rects.size() != 0 && !mLandmarkModel.empty()){
            for (unsigned long i = 0; i < rects.size(); ++i) {
                dlib::full_object_detection shape = msp(img, rects[i]);
                LOG(INFO) << "face index:" << i
                          << "number of parts: " << shape.num_parts();
                matrix<rgb_pixel> face_chip;
                dlib::extract_image_chip(img, get_face_chip_details(shape, 150, 0.25), face_chip);
                mFaces.push_back(face_chip);
            }
        }
        return rects.size();
    }
#endif

    inline int detect(const dlib::array2d<dlib::rgb_pixel>& pixel_img){
        if (pixel_img.size() == 0){
            return 0;
        }
        LOG(INFO) << "DLIB Face detecting ...";
        std::vector<dlib::rectangle> rects = mFaceDetector(pixel_img);
        LOG(INFO) << "DLIB HOG Faces detected: " << rects.size();

        if(rects.size() != 0 && !mLandmarkModel.empty()){
            for (unsigned long i = 0; i < rects.size(); ++i) {
                dlib::full_object_detection shape = msp(pixel_img, rects[i]);
                LOG(INFO) << "face index: " << i
                          << " number of parts: " << shape.num_parts();
                matrix<rgb_pixel> face_chip;
                dlib::extract_image_chip(pixel_img, get_face_chip_details(shape, 150, 0.25), face_chip);
                mPrepFaces.push_back(face_chip);
            }
        }
        return rects.size();
    }

public:
    DLibFaceRecognition(const std::string& landmarkModel, const std::string& recognitonModle,
            const float tolerance)
            :mLandmarkModel(landmarkModel),
             mRecognitionModel(recognitonModle),
             mTolerance(tolerance){
        mFaceDetector = get_frontal_face_detector();
        init();
    }

    inline int init(){
        LOG(INFO) << "Init DLibFaceRecognition";
        if(jniutils::fileExists(mLandmarkModel) && jniutils::fileExists(mRecognitionModel)){
            dlib::deserialize(mLandmarkModel) >> msp;
            dlib::deserialize(mRecognitionModel) >> net;
            return 1;
        } else{
            LOG(INFO) << "Not exist modle:" << mLandmarkModel << " " << mRecognitionModel;
            return 0;
        }
    }

    virtual inline int preprocess(const std::string& preprocessImage){
        LOG(INFO) << "Preprocess DLibFaceRecognition";
        if(jniutils::dirExists(preprocessImage)){
            std::vector<file> files = dlib::get_files_in_directory_tree(preprocessImage, match_endings(".jpg .jpeg .png"));
            if(files.size() == 0){
                LOG(INFO) << "Not exist original image";
                return 0;
            } else {
                for (int i = 0; i < files.size(); ++i) {
                    array2d<rgb_pixel> src_img;
//                    cv::Mat src_img = cv::imread(files[i], CV_LOAD_IMAGE_COLOR);
                    LOG(INFO) << "Load preprocess image: " << files[i];
                    load_image(src_img, files[i]);
                    detect(src_img);
                    if(mPrepFaces.size() == 0){
                        continue;
                    }
                    std::vector<matrix<float,0,1>> face_descriptors = net(mPrepFaces);
                    mDescriptors.push_back(face_descriptors[0]);
                    mNames.push_back(files[i].name());
                }
                return 1;
            }
        } else {
            LOG(INFO) << "Not exist original image";
            return 0;
        }
    }

    virtual inline int recognize(const dlib::array2d<dlib::rgb_pixel>& image){
        if (image.size() == 0){
            return 0;
        }
        LOG(INFO) << "Image width: " << image.nc()
                  << " height: " << image.nr();
        mRects.clear();
        mRects = mFaceDetector(image);
        std::vector<matrix<rgb_pixel>> face_chips;
        LOG(INFO) << mRects.size() <<" face(s) detected!";
        if(mRects.size() == 0){
            return 0;
        }
        for (int i = 0; i < mRects.size(); ++i) {
            auto shape = msp(image, mRects[i]);
            matrix<rgb_pixel> face_chip;
            extract_image_chip(image, get_face_chip_details(shape, 150, 0.25), face_chip);
            face_chips.push_back(face_chip);
        }
        LOG(INFO) << "Caculate the face feature descriptor";
        std::vector<matrix<float,0,1>> face_descriptors = net(face_chips);

        mRecognizedName.clear();
//        mFaceNameMap.clear();
        float len;
        for (int i = 0; i < face_descriptors.size(); ++i) {
            for (int j = 0; j < mDescriptors.size(); ++j) {
                len = length(mDescriptors[j] - face_descriptors[i]);
                name = "Unknown";
                if(len < mTolerance){
                    name = mNames[j];
                }
                LOG(INFO) << "Lenght: " << len;
                mRecognizedName.push_back(name);
//                mFaceNameMap.insert(pair<std::string, std::vector<dlib::rectangle>>(name, mRects));
            }
        }
        return  mRects.size();

    }

#ifdef DLIB_OPENCV_SUPPORT
    virtual inline int recognize2(const cv::Mat& image){
        if(image.empty())
            return 0;
        if(image.channels() == 1){
            cv::cvtColor(image, image, CV_GRAY2BGR);
        }
        CHECK(image.channels() == 3);
        dlib::cv_image<dlib::bgr_pixel> img(image);
        mRects = mFaceDetector(img);
        std::vector<matrix<rgb_pixel>> face_chips;

        for (int i = 0; i < mRects.size(); ++i) {
            auto shape = msp(img, mRects[i]);
            matrix<rgb_pixel> face_chip;
            extract_image_chip(img, get_face_chip_details(shape, 150, 0.25), face_chip);
            face_chips.push_back(face_chip);
        }
        std::vector<matrix<float,0,1>> face_descriptors = net(face_chips);

        mRecognizedName.clear();
        mFaceNameMap.clear();
        float len;
        for (int i = 0; i < face_descriptors.size(); ++i) {
            for (int j = 0; j < mDescriptors.size(); ++j) {
                len = length(mDescriptors[j] - face_descriptors[i]);
                if(len < mTolerance){
                    name = mNames[j];
                }
                LOG(INFO) << "Lenght: " << len;
                mRecognizedName.push_back(name);
                mFaceNameMap.insert(pair<std::string, std::vector<dlib::rectangle>>(name, mRects));
            }
        }
        return  mRects.size();
    }
#endif

    inline std::vector<dlib::rectangle> getRects() {
        return mRects;
    }

    inline std::vector<std::string> getNames(){
        return mRecognizedName;
    }

    inline std::unordered_map<std::string, std::vector<dlib::rectangle>>& getFaceNameMap(){
        return mFaceNameMap;
    };

protected:
    std::vector<dlib::rectangle> mRects;
    std::vector<matrix<rgb_pixel>> mPrepFaces;
    std::vector<matrix<float,0,1>> mDescriptors;
    std::vector<std::string> mNames;
    std::vector<std::string> mRecognizedName;
    std::unordered_map<std::string, std::vector<dlib::rectangle>> mFaceNameMap;
    std::string name = "Unknown";
};

#endif //DLIB_FOR_ANDROID_RECOGNITION_H
