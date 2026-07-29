# Aminema
-keepattributes *Annotation*, InnerClasses
# kotlinx.serialization
-keepclassmembers class com.amin.tvos.data.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.amin.tvos.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# WebView JS interfaces power keyboard, playback, poster and background-sync bridges.
# Keep annotated methods and their containing classes when the debug update channel
# is minified; protected media URLs are never exposed by these bridges.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepclasseswithmembers,allowoptimization class * {
    @android.webkit.JavascriptInterface <methods>;
}
