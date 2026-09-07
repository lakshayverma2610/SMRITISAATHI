# Add project specific ProGuard rules here.

# Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * extends androidx.room.RoomDatabase {
    abstract *;
}

# Firebase
-keep class com.google.firebase.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Kotlin Serialization
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# Domain models (need to survive minification for Firestore serialization)
-keep class com.nercare.cogcare.domain.model.** { *; }
-keep class com.nercare.cogcare.data.local.entities.** { *; }

# Moshi
-keep class com.squareup.moshi.** { *; }

# WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(...);
}

# Generative AI
-keep class com.google.ai.** { *; }

# General Android
-keepattributes SourceFile,LineNumberTable
