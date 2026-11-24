# FaceLiveness demo

# The engine resolves these by name through JNI. The AAR ships the same rules, so this is only
# here to keep a release build working if the dependency is ever wired up some other way.
-keep class com.faceplugin.facerecognitionsdk.** { *; }
-keep class faceplugin.** { *; }
-keepclassmembers class * { native <methods>; }
