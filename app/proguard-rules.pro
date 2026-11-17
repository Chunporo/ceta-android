# Gson fills the network DTOs by reflection, by field name: R8 must neither rename nor strip them (and their Kotlin defaults).
-keep class com.ceta.cinema.data.remote.dto.** { *; }

# Retrofit reads generic types and annotations of the API interface at runtime.
-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep interface com.ceta.cinema.data.remote.api.** { *; }
