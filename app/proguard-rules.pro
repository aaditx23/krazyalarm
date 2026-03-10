# ================================================================================================
# KrazyAlarm ProGuard Rules
# ================================================================================================

# Preserve line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*,Signature,Exception

# ================================================================================================
# Kotlin
# ================================================================================================

-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings { <fields>; }

# ================================================================================================
# Kotlin Coroutines
# ================================================================================================

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**

# ================================================================================================
# Room Database
# ================================================================================================

-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# ================================================================================================
# Koin Dependency Injection
# ================================================================================================

-keep class org.koin.** { *; }
-dontwarn org.koin.**

# Keep our DI modules so Koin can resolve them at runtime
-keep class com.aaditx23.krazyalarm.di.** { *; }

# Keep KoinComponent inject delegates on receivers/services
-keepclassmembers class * implements org.koin.core.component.KoinComponent { *; }

# ================================================================================================
# App-specific classes R8 cannot infer
# ================================================================================================

# Data models (data classes used with Room / DataStore)
-keep class com.aaditx23.krazyalarm.domain.models.** { *; }

# Enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Repository interfaces (referenced by string in Koin modules)
-keep interface com.aaditx23.krazyalarm.domain.repository.** { *; }
-keep class com.aaditx23.krazyalarm.data.repository.** { *; }

# ViewModels — constructors must survive for Koin parametersOf
-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }

# ================================================================================================
# Alarm Infrastructure — the entire scheduler package must be kept intact
# ================================================================================================

# Everything in the scheduler package: receivers, service, scheduler, queue manager
-keep class com.aaditx23.krazyalarm.data.scheduler.** { *; }

# AlarmRingingActivity + companion (intent extras, static fields read at runtime)
-keep class com.aaditx23.krazyalarm.presentation.screen.alarm_ringing.AlarmRingingActivity { *; }

# AlarmRingingViewModel constructor (Koin parametersOf passes alarmId)
-keep class com.aaditx23.krazyalarm.presentation.screen.alarm_ringing.AlarmRingingViewModel { <init>(...); *; }

# ================================================================================================
# Reflection attributes for DI and Room
# ================================================================================================

-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# ================================================================================================
# Misc
# ================================================================================================

-keepclasseswithmembernames class * { native <methods>; }
-keepclassmembers class * implements android.os.Parcelable { public static final ** CREATOR; }

-dontwarn com.google.android.gms.**
-dontwarn org.slf4j.**
-dontwarn org.jetbrains.annotations.**
-dontwarn javax.annotation.**

# ================================================================================================
# End of ProGuard Rules
# ================================================================================================
