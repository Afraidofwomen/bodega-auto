cmake_minimum_required(VERSION 3.16)
project(srm_bridge LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

find_package(JNI REQUIRED)

add_library(srm_bridge SHARED
    src/srm_bridge.cpp
    src/jni_srm_bridge.cpp
)

target_include_directories(srm_bridge PRIVATE include ${JNI_INCLUDE_DIRS})
target_link_libraries(srm_bridge PRIVATE ${JNI_LIBRARIES})
