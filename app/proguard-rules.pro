# Keep Room database and annotated schema classes
-keep class com.lexapps.sblgntreader.data.db.SblgntDatabase_Impl { *; }
-keep @androidx.room.Database class com.lexapps.sblgntreader.** { *; }
-keep @androidx.room.Entity class com.lexapps.sblgntreader.** { *; }
-keep @androidx.room.Dao class com.lexapps.sblgntreader.** { *; }
-dontwarn androidx.room.**

# Firebase Crashlytics keeps source file + line metadata for crash reports.
-keepattributes SourceFile,LineNumberTable
-keep class com.google.firebase.crashlytics.FirebaseCrashlytics { *; }
-dontwarn com.google.firebase.crashlytics.**


