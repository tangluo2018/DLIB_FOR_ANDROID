//
// Created by tanglou on 11/15/18.
//
#include <iostream>
#include <dlib/image_processing/frontal_face_detector.h>
#include <dlib/image_io.h>

using namespace dlib;
using namespace std;

int main(int argc, char** argv){
    try {
        if(argc == 1){
            cout << "Give  image files as argument" << endl;
            return 0;
        }
        frontal_face_detector detector = get_frontal_face_detector();
        for (int i = 1; i < argc; ++i) {
            cout << "processing image" << endl;
            array2d<unsigned char> img;
            load_image(img, argv[i]);
            std::vector<rectangle> rect = detector(img);
            if(rect.size() == 0) {
                cout << "No face detected!" << endl;
            }
            for (int j = 0; j < rect.size(); ++j) {
                cout << "Face " << j+1 << " left: " << rect[i].left()
                                       << " right: "<< rect[i].right()
                                       << " top: "  << rect[i].top()
                                       << " bottom: " << rect[i].bottom()
                                       << endl;
            }
        }
        return 1;
    }catch (exception& e) {
        cout << "exception thrown!" << endl;
        cout << e.what() << endl;
    }
}
