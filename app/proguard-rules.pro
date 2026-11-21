# Keep Room schemas and entities
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Retrofit & Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.mtrx.store.data.remote.dto.** { *; }

# Coil
-keep class coil.** { *; }
-dontwarn coil.**
