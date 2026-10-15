# MediaPipe usa reflexão e JNI.
-keep class com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.**
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**
# OpenCV carrega classes via JNI.
-keep class org.opencv.** { *; }
