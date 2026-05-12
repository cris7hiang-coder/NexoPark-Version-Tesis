# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
#####################################
## Reglas básicas para Android y Kotlin
#####################################
#####################################
## Reglas básicas para Android y Kotlin
#####################################
-keepattributes *Annotation*
-keepclassmembers class ** {
    @androidx.annotation.Keep *;
}
-keep @androidx.annotation.Keep class * { *; }
-dontwarn kotlinx.coroutines.**
-dontwarn kotlin.reflect.jvm.internal.**
-dontwarn kotlin.jvm.internal.**

# Gson
-keepclassmembers class ** {
    @com.google.gson.annotations.SerializedName <fields>;
}

#####################################
## Jetpack Compose
#####################################
-keep class androidx.compose.** { *; }
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.compose.**
-dontwarn androidx.activity.**
-dontwarn androidx.lifecycle.**

#####################################
## Firebase
#####################################
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-keep class com.google.firebase.** { *; }
-keepclassmembers class com.google.firebase.** { *; }
-keepclassmembers class com.google.android.gms.internal.** { *; }

#####################################
## Google Play Services / Maps / Places
#####################################
-keep class com.google.android.gms.maps.** { *; }
-keep class com.google.android.gms.maps.model.** { *; }
-keep class com.google.android.libraries.places.** { *; }
-keep interface com.google.android.gms.** { *; }

#####################################
## ML Kit (Barcode scanning)
#####################################
-dontwarn com.google.mlkit.**
-keep class com.google.mlkit.** { *; }
-keep interface com.google.mlkit.** { *; }

#####################################
## CameraX
#####################################
-keep class androidx.camera.** { *; }
-keep interface androidx.camera.** { *; }
-dontwarn androidx.camera.**

#####################################
## ZXing
#####################################
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

#####################################
## Lottie
#####################################
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

#####################################
## Retrofit / OkHttp / Gson
#####################################
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keep class com.squareup.okhttp3.** { *; }
-keepclassmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
