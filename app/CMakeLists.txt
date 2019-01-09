# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

message("Checking CMAKE_SYSTEM_NAME = '${CMAKE_SYSTEM_NAME}'")
if (${CMAKE_SYSTEM_NAME} MATCHES "Darwin")
    add_definitions(-DOS_OSX)
elseif (${CMAKE_SYSTEM_NAME} MATCHES "Linux")
    add_definitions(-DOS_LINUX)
elseif (${CMAKE_SYSTEM_NAME} MATCHES "Windows")
    add_definitions(-DOS_WIN)
elseif (${CMAKE_SYSTEM_NAME} MATCHES "Android")
    add_definitions(-DOS_ANDROID)
    message("Checking CMAKE_ABI_NAME = '${CMAKE_ANDROID_ARCH_ABI}'")
else()
    message("OS not detected.")
endif()

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

set(DLIB_ANDROID_SHARED_NAME dlib_jni)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -O2 -O3 ")
set(DLIB_JNI_DIR src/main/cpp)
set(CPP_DIR src/main/cpp)
set(DLIB_DIR ${CPP_DIR}/dlib)
set(JNI_COMMON_DIR ${CPP_DIR}/jni_common)
set(THIRD_PARTY_DIR third_party)
set(OPENCV_DIR ${PROJECT_SOURCE_DIR}/${THIRD_PARTY_DIR}/OpenCV-android-sdk)
set(OPENCV_PREBUILT_DIR ${OPENCV_DIR}/sdk/native/jni)
set(OPENCV_CONFIG_DIR "${OPENCV_PREBUILT_DIR}/abi-${CMAKE_ANDROID_ARCH_ABI}")
set(MINIGLOG_DIR ${PROJECT_SOURCE_DIR}/${THIRD_PARTY_DIR}/miniglog/glog)
set(LIBS_DIR ${PROJECT_SOURCE_DIR}/libs)


set(CMAKE_ANDROID_ARM_NEON ON)

#add_definitions(-D__ARM_NEON__)
#add_definitions(-D__ARM_NEON)
set(ARM_NEON_IS_AVAILABLE 1)

set(OPENCV_INCLUDE_DIR ${OPENCV_PREBUILT_DIR}/include)

include_directories(${CPP_DIR} ${DLIB_DIR} ${DLIB_DIR}/dlib/external/libpng  ${DLIB_DIR}/dlib/external/libjpeg ${THIRD_PARTY_DIR}/miniglog
        ${OPENCV_INCLUDE_DIR})


add_definitions(-DCMAKE_BUILD_TYPE=Release)

#add_subdirectory(${DLIB_DIR})
#include(${DLIB_DIR})

add_definitions(-DDLIB_PNG_SUPPORT)
add_definitions(-DDLIB_JPEG_SUPPORT)
add_definitions(-DDLIB_NO_GUI_SUPPORT)

#if (DLIB_PNG_SUPPORT)
#    include_directories(${DLIB_DIR}/dlib/external/libpng ${DLIB_DIR}/dlib/external/zlib)
#    set(source_files ${source_files}
#            ${DLIB_DIR}/dlib/external/libpng/png.c
#            ${DLIB_DIR}/dlib/external/libpng/pngerror.c
#            ${DLIB_DIR}/dlib/external/libpng/pngget.c
#            ${DLIB_DIR}/dlib/external/libpng/pngmem.c
#            ${DLIB_DIR}/dlib/external/libpng/pngpread.c
#            ${DLIB_DIR}/dlib/external/libpng/pngread.c
#            ${DLIB_DIR}/dlib/external/libpng/pngrio.c
#            ${DLIB_DIR}/dlib/external/libpng/pngrtran.c
#            ${DLIB_DIR}/dlib/external/libpng/pngrutil.c
#            ${DLIB_DIR}/dlib/external/libpng/pngset.c
#            ${DLIB_DIR}/dlib/external/libpng/pngtrans.c
#            ${DLIB_DIR}/dlib/external/libpng/pngwio.c
#            ${DLIB_DIR}/dlib/external/libpng/pngwrite.c
#            ${DLIB_DIR}/dlib/external/libpng/pngwtran.c
#            ${DLIB_DIR}/dlib/external/libpng/pngwutil.c
#            ${DLIB_DIR}/dlib/external/zlib/adler32.c
#            ${DLIB_DIR}/dlib/external/zlib/compress.c
#            ${DLIB_DIR}/dlib/external/zlib/crc32.c
#            ${DLIB_DIR}/dlib/external/zlib/deflate.c
#            ${DLIB_DIR}/dlib/external/zlib/gzclose.c
#            ${DLIB_DIR}/dlib/external/zlib/gzlib.c
#            ${DLIB_DIR}/dlib/external/zlib/gzread.c
#            ${DLIB_DIR}/dlib/external/zlib/gzwrite.c
#            ${DLIB_DIR}/dlib/external/zlib/infback.c
#            ${DLIB_DIR}/dlib/external/zlib/inffast.c
#            ${DLIB_DIR}/dlib/external/zlib/inflate.c
#            ${DLIB_DIR}/dlib/external/zlib/inftrees.c
#            ${DLIB_DIR}/dlib/external/zlib/trees.c
#            ${DLIB_DIR}/dlib/external/zlib/uncompr.c
#            ${DLIB_DIR}/dlib/external/zlib/zutil.c
#            )

#        include(${DLIB_DIR}/dlib/cmake_utils/check_if_neon_available.cmake)
#        if (ARM_NEON_IS_AVAILABLE)
#            message (STATUS "NEON instructions will be used for libpng.")
#            enable_language(ASM)
#            set(source_files ${source_files}
#                    ${DLIB_DIR}/dlib/external/libpng/arm/arm_init.c
#                    ${DLIB_DIR}/dlib/external/libpng/arm/filter_neon_intrinsics.c
#                    ${DLIB_DIR}/dlib/external/libpng/arm/filter_neon.S
#                    )
#            set_source_files_properties(${DLIB_DIR}/dlib/external/libpng/arm/filter_neon.S PROPERTIES COMPILE_FLAGS "${CMAKE_ASM_FLAGS} ${CMAKE_CXX_FLAGS} -x assembler-with-cpp")
#        endif()
#        set(REQUIRES_LIBS "")
#    set(source_files ${source_files}
#            image_loader/png_loader.cpp
#            image_saver/save_png.cpp
#            )
#endif()

#add_library(
#        dlib
#        SHARED
#        ${source_files}
#        src/main/cpp/dlib/dlib/all/source.cpp
#)
#
#target_link_libraries(
#        dlib
#)

include(${OPENCV_CONFIG_DIR}/OpenCVConfig.cmake)
#include(${DLIB_DIR}/dlib/cmake_utils/release_build_by_default)

message("OpenCV_CONFIG_PATH = '${OpenCV_CONFIG_PATH}'")

add_library(
        dlib
        SHARED
        IMPORTED
)

set_target_properties(
        dlib
        PROPERTIES IMPORTED_LOCATION
        ${LIBS_DIR}/${CMAKE_ANDROID_ARCH_ABI}/libdlib.so
)

add_library( # Sets the name of the library.
             ${DLIB_ANDROID_SHARED_NAME}

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
#             ${DLIB_DIR}/dlib/all/source.cpp
             ${DLIB_JNI_DIR}/jni_face_detect.cpp
             ${DLIB_JNI_DIR}/jni_face_recognize.cpp
             ${JNI_COMMON_DIR}/jni_utils.cpp
             ${JNI_COMMON_DIR}/jni_fileutils.cpp
             ${JNI_COMMON_DIR}/jni_bitmap2array2d.cpp
#             ${JNI_COMMON_DIR}/jni_bitmap2mat.cpp
             ${JNI_COMMON_DIR}/rgb2yuv.cpp
             ${JNI_COMMON_DIR}/yuv2rgb.cpp
             ${JNI_COMMON_DIR}/jni_imageutils.cpp
#             ${DLIB_DIR}/dlib/threads/threads_kernel_shared.cpp
#             ${DLIB_DIR}/dlib/entropy_decoder/entropy_decoder_kernel_2.cpp
#             ${DLIB_DIR}/dlib/base64/base64_kernel_1.cpp
#             ${DLIB_DIR}/dlib/threads/threads_kernel_1.cpp
#             ${DLIB_DIR}/dlib/threads/threads_kernel_2.cpp
#             ${DLIB_DIR}/dlib/cuda/cpu_dlib.cpp
#             ${DLIB_DIR}/dlib/cuda/tensor_tools.cpp
#             ${DLIB_DIR}/dlib/threads/thread_pool_extension.cpp
#             ${DLIB_DIR}/dlib/threads/async.cpp
#             ${DLIB_DIR}/dlib/dir_nav/dir_nav_extensions.cpp
#             #${DLIB_DIR}/dlib/dir_nav/dir_nav_kernel_1.cpp
#             ${DLIB_DIR}/dlib/dir_nav/dir_nav_kernel_2.cpp
             ${MINIGLOG_DIR}/logging.cc
        )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log
             )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                       ${DLIB_ANDROID_SHARED_NAME}

                       # Links the target library to the log library
                       # included in the NDK.
                       android
                       dlib
                       z
                       m
                       dl
                       jnigraphics
                       atomic
#                       opencv_core
#                       opencv_imgproc
#                       opencv_imgcodecs
#                       opencv_highgui
                       ${log-lib}
        )

add_executable(
        dlibtest
        src/main/cpp/dlibtest.cpp
)

target_link_libraries(
        dlibtest
        dlib
)