# Add project specific ProGuard rules here.
-keep class com.nousresearch.hermes.data.model.** { *; }
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
