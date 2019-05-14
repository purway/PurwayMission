LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
    native.cpp

LOCAL_SHARED_LIBRARIES := \
    libcutils \
    libutils \
    libbinder \
    libui \
    libgui \
    libstagefright_foundation

LOCAL_MODULE:= MyShowYUV

LOCAL_MODULE_TAGS := tests

include $(BUILD_EXECUTABLE)