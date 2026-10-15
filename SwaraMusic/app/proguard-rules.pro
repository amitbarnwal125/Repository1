# Keep line numbers for readable crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
# OkHttp platform warnings
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
# Media session service is referenced from the manifest
-keep class com.swara.music.playback.PlaybackService { *; }
