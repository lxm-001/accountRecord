# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ===== Tinker hotfix =====
# Tinker's loader / runtime classes must never be obfuscated; the loader looks them up by name.
-dontwarn com.tencent.tinker.**
-keep class com.tencent.tinker.** { *; }
-keep class com.tencent.tinker.entry.** { *; }
-keep class com.tencent.tinker.loader.** { *; }
-keep class com.tencent.tinker.lib.** { *; }
# Anything Tinker-annotated must be preserved.
-keep @com.tencent.tinker.anno.DefaultLifeCycle public class *
-keepclasseswithmembernames class * {
    native <methods>;
}
# Our own ApplicationLike + host Application are listed as Tinker loaders — keep them too.
-keep class com.mian.accountrecord.AccountRecordApp { *; }
-keep class com.mian.accountrecord.tinker.SampleApplicationLike { *; }
-keep class com.mian.accountrecord.tinker.TinkerManager { *; }
