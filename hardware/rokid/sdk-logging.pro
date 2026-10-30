# CXR-L 1.0.3 logs the authorization token in ExternalAppClient.connect.
# Remove SDK and app Android logging at compile time, including debug APKs.
-assumenosideeffects class android.util.Log {
    public static *** *(...);
}
# Preserve SDK reflection / JNI names while permitting logging removal.
-keep,allowoptimization class com.rokid.** { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-assumenosideeffects class java.lang.Throwable {
    public void printStackTrace(...);
}
