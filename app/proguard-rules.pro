# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\<user>\AppData\Local\Android\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
#-dontwarn
-dontskipnonpubliclibraryclasses
#-dontobfuscate
-forceprocessing
-optimizationpasses 10

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Jackson
-keepattributes *Annotation*,EnclosingMethod,Signature
-keepnames class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.databind.**
-keep class org.codehaus.** { *; }
-keepclassmembers public final enum org.codehaus.jackson.annotate.JsonAutoDetect$Visibility {
    public static final org.codehaus.jackson.annotate.JsonAutoDetect$Visibility *; }


# Belvedere
-keep enum fr.marin.cyril.belvedere
-keep class fr.marin.cyril.belvedere
-keep interface fr.marin.cyril.belvedere

# Logs
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}