# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in <sdk>/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-keepattributes LineNumberTable,SourceFile
-renamesourcefileattribute SourceFile
-printconfiguration "/tmp/Android Studio/build/full-r8-config.txt"
-printusage "/tmp/Android Studio/build/usage.txt"
-printseeds "/tmp/Android Studio/build/seeds.txt"

# Keep generic signature of TypeToken, TypeToken$TypeImpl for Gson serialization
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# Keep the generic signatures needed for Gson type adapters
-keepattributes Signature

# Keep LevelCompletionData and Map/HashMap for serialization
-keep class roboyard.eclabs.ui.LevelCompletionData { *; }
-keepclassmembers class roboyard.eclabs.ui.LevelCompletionData { *; }

# Keep serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
