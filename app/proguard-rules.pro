# Правила R8 для ЭХО.
# kotlinx.serialization работает через сгенерированные сериализаторы.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class ru.echo.radar.data.remote.** {
    *** Companion;
}
-keepclasseswithmembers class ru.echo.radar.data.remote.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Coil
-dontwarn coil.**
