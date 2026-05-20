-keep class com.disparasms.app.data.local.entity.** { *; }
-keep class com.disparasms.app.data.model.** { *; }
-keep class com.disparasms.app.domain.model.** { *; }

-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

-dontwarn org.apache.poi.**
-dontwarn com.fasterxml.jackson.**
-dontwarn org.osgi.**
-dontwarn org.apache.logging.log4j.**
