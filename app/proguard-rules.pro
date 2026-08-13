# --- Sabri Usta TV R8 kurallari ---

# Release surumunde tum debug/verbose loglari kaldirilir.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.sabriusta.tv.data.catalog.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.sabriusta.tv.data.catalog.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Media3 / ExoPlayer
-dontwarn androidx.media3.**
-keep class androidx.media3.exoplayer.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Room
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**

# Hilt tarafindan uretilen bilesenler
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
