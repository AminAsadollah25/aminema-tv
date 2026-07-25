# Aminema
-keepattributes *Annotation*, InnerClasses
# kotlinx.serialization
-keepclassmembers class com.amin.tvos.data.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.amin.tvos.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# WebView JS interface (none used, kept for future)
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
