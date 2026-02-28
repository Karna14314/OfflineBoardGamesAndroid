# Preserve coroutines
-keepnames class kotlinx.coroutines.** { *; }

# Preserve DataStore
-keep class androidx.datastore.** { *; }

# Preserve game classes referenced by name
-keep class com.offlinegames.games.** { *; }
-keep class com.offlinegames.core.** { *; }

# Keep serialisable data classes
-keepclassmembers class * implements java.io.Serializable {
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
