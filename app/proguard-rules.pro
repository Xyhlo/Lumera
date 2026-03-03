# ─── Lumera ProGuard / R8 Rules ───

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ─── Retrofit + Gson ───
-keepattributes Signature
-keepattributes *Annotation*

# Gson serialized classes (keep fields used by Gson)
-keep class com.lumera.app.data.remote.** { *; }
-keep class com.lumera.app.data.update.AppUpdateManager$* { *; }
-keep class com.lumera.app.data.model.** { *; }
-keep class com.lumera.app.data.model.stremio.** { *; }

# Retrofit interfaces
-keep,allowobfuscation interface com.lumera.app.data.remote.StremioApiService
-keep,allowobfuscation interface com.lumera.app.data.remote.IntroDbService

# Prevent R8 from stripping Gson TypeToken
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# ─── Room ───
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# ─── Hilt / Dagger ───
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# ─── libtorrent4j (JNI) ───
-keep class org.libtorrent4j.** { *; }
-keepclassmembers class org.libtorrent4j.** { *; }

# ─── NanoHTTPD ───
-keep class fi.iki.elonen.** { *; }

# ─── ZXing ───
-keep class com.google.zxing.** { *; }

# ─── AndroidX Security (EncryptedSharedPreferences) ───
-keep class androidx.security.crypto.** { *; }

# ─── Compose (defaults handle most, but keep stability) ───
-dontwarn androidx.compose.**

# ─── Media3 / ExoPlayer ───
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ─── OkHttp ───
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# ─── General ───
-dontwarn javax.annotation.**
-dontwarn kotlin.reflect.jvm.internal.**
