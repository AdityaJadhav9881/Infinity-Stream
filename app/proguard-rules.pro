# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep Hilt classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep Room entities
-keep class com.musicflow.app.data.local.entity.** { *; }

# Keep Ktor serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Coil
-keep class coil.** { *; }
-dontwarn coil.**

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses, EnclosingMethod
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.musicflow.app.**$$serializer { *; }
-keepclassmembers class com.musicflow.app.** { *** Companion; }
-keepclasseswithmembers class com.musicflow.app.** { kotlinx.serialization.KSerializer serializer(...); }

# yt-dlp Android
-keep class com.yausername.youtubedl_android.** { *; }
-dontwarn com.yausername.youtubedl_android.**

# ExoPlayer / Media3
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.datasource.** { *; }
-keep class androidx.media3.session.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Prevent R8 from stripping interface information
-keep,allowobfuscation,allowshrinking interface kotlin.coroutines.Continuation
