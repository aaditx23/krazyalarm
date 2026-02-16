# ================================================================================================
# KrazyAlarm ProGuard Rules
# ================================================================================================

# Preserve line numbers for debugging crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep annotations
-keepattributes *Annotation*,Signature,Exception

# ================================================================================================
# Kotlin
# ================================================================================================

-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}

# ================================================================================================
# Kotlin Coroutines
# ================================================================================================

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ================================================================================================
# Jetpack Compose
# ================================================================================================

-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.animation.** { *; }

# Keep Composable functions
-keep @androidx.compose.runtime.Composable class * { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# ================================================================================================
# Room Database
# ================================================================================================

-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Room entities and DAOs
-keep class com.aaditx23.krazyalarm.data.local.database.** { *; }
-keep interface com.aaditx23.krazyalarm.data.local.database.** { *; }

# ================================================================================================
# Koin Dependency Injection
# ================================================================================================

-keep class org.koin.** { *; }
-keep class org.koin.core.** { *; }
-keep class org.koin.dsl.** { *; }
-keepnames class * extends org.koin.core.module.Module
-keepclassmembers class * {
    public <init>(...);
}

# Keep Koin modules
-keep class com.aaditx23.krazyalarm.di.** { *; }

# ================================================================================================
# App Data Models
# ================================================================================================

# Keep all data models (data classes, enums, sealed classes)
-keep class com.aaditx23.krazyalarm.domain.models.** { *; }
-keepclassmembers class com.aaditx23.krazyalarm.domain.models.** { *; }

# Keep sealed class hierarchy
-keep class * extends com.aaditx23.krazyalarm.domain.models.FlashPattern { *; }
-keep class * extends com.aaditx23.krazyalarm.domain.models.VibrationPattern { *; }

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ================================================================================================
# ViewModels
# ================================================================================================

-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(...);
}
-keep class com.aaditx23.krazyalarm.presentation.**.** extends androidx.lifecycle.ViewModel { *; }

# ================================================================================================
# Repositories and Data Sources
# ================================================================================================

-keep interface com.aaditx23.krazyalarm.domain.repository.** { *; }
-keep class com.aaditx23.krazyalarm.data.repository.** { *; }
-keep class com.aaditx23.krazyalarm.data.local.** { *; }

# ================================================================================================
# Services and Receivers
# ================================================================================================

-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver
-keep class com.aaditx23.krazyalarm.data.scheduler.** { *; }

# Keep alarm scheduler implementations
-keep class com.aaditx23.krazyalarm.data.scheduler.AlarmSchedulerImpl { *; }
-keep class com.aaditx23.krazyalarm.data.scheduler.AlarmRingingService { *; }
-keep class com.aaditx23.krazyalarm.data.scheduler.AlarmReceiver { *; }

# ================================================================================================
# Activities
# ================================================================================================

-keep class * extends androidx.activity.ComponentActivity {
    <init>(...);
}
-keep class com.aaditx23.krazyalarm.MainActivity { *; }
-keep class com.aaditx23.krazyalarm.presentation.screen.alarm_ringing.AlarmRingingActivity { *; }

# ================================================================================================
# Audio and Media
# ================================================================================================

-keep class com.aaditx23.krazyalarm.util.AudioAmplifier { *; }
-keep class android.media.audiofx.** { *; }
-dontwarn android.media.audiofx.**

# ================================================================================================
# Serialization (for DataStore, SharedPreferences)
# ================================================================================================

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ================================================================================================
# Parcelable
# ================================================================================================

-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ================================================================================================
# Navigation
# ================================================================================================

-keep class androidx.navigation.** { *; }
-keepnames class androidx.navigation.fragment.NavHostFragment

# ================================================================================================
# Material Components
# ================================================================================================

-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# ================================================================================================
# Reflection (for dependency injection and Room)
# ================================================================================================

-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# ================================================================================================
# R8 Full Mode Compatibility
# ================================================================================================

# Disable obfuscation for debugging (remove in production if needed)
# -dontobfuscate

# Optimization flags
-optimizationpasses 5
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# ================================================================================================
# Remove Logging in Release
# ================================================================================================

-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# ================================================================================================
# Keep Native Methods
# ================================================================================================

-keepclasseswithmembernames class * {
    native <methods>;
}

# ================================================================================================
# AndroidX and Google
# ================================================================================================

-dontwarn com.google.android.gms.**
-dontwarn androidx.**

# Keep AndroidX
-keep class androidx.** { *; }
-keep interface androidx.** { *; }

# ================================================================================================
# Warnings to Ignore
# ================================================================================================

-dontwarn org.slf4j.**
-dontwarn org.jetbrains.annotations.**
-dontwarn javax.annotation.**
-dontwarn edu.umd.cs.findbugs.annotations.**

# ================================================================================================
# End of ProGuard Rules
# ================================================================================================
