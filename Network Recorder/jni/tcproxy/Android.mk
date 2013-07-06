LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

TARGET_PLATFORM := android-14
TARGET_ARCH_ABI := armeabi armeabi-v7a mips x86

LOCAL_MODULE := tcproxy
LOCAL_SRC_FILES += tcproxy.cpp

include $(BUILD_EXECUTABLE)