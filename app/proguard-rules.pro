# DakotaGroupStaff ProGuard Configuration
# Add project specific ProGuard rules here.

# ==========================================
# PERFORMANCE & SECURITY RULES
# ==========================================

# Keep line numbers for better debugging in production
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep annotations
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

# ==========================================
# RETROFIT & OKHTTP
# ==========================================

# Retrofit does reflection on generic parameters. InnerClasses is required to use Signature and
# EnclosingMethod is required to use InnerClasses.
-keepattributes Signature, InnerClasses, EnclosingMethod

# Retrofit does reflection on method and parameter annotations.
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Keep annotation default values (e.g., retrofit2.http.Field.encoded).
-keepattributes AnnotationDefault

# Retain service method parameters when optimizing.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Ignore annotation used for build tooling.
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Ignore JSR 305 annotations for embedding nullability information.
-dontwarn javax.annotation.**

# Guarded by a NoClassDefFoundError try/catch and only used when on the classpath.
-dontwarn kotlin.Unit

# Top-level functions that can only be used by Kotlin.
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# With R8 full mode, it sees no subtypes of Retrofit interfaces since they are created with a Proxy
# and replaces all potential values with null. Explicitly keeping the interfaces prevents this.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# Keep inherited services.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface * extends <1>

# With R8 full mode generic signatures are stripped for classes that are not
# kept. Suspend functions are wrapped in continuations where the type argument
# is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# R8 full mode strips generic signatures from return types if not kept.
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>

# With R8 full mode generic signatures are stripped for classes that are not kept.
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Keep Certificate Pinning class (CRITICAL FOR SECURITY)
-keep class okhttp3.CertificatePinner { *; }
-keep class okhttp3.CertificatePinner$Builder { *; }

# ==========================================
# GSON
# ==========================================

# Gson uses generic type information stored in a class file when working with fields.
-keepattributes Signature

# Gson specific classes
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }

# Keep generic signature of Call, Response (R8 full mode strips signatures from non-kept items)
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Application classes that will be serialized/deserialized over Gson
# CRITICAL: Keep all API request and response classes to prevent field obfuscation
-keep class com.dakotagroupstaff.data.remote.response.** { <fields>; }
-keep class com.dakotagroupstaff.data.local.entity.** { <fields>; }
-keep class com.dakotagroupstaff.data.remote.retrofit.**Request { <fields>; }

# Keep all data classes used in API requests (prevent R8 from removing fields like 'nip')
-keep class com.dakotagroupstaff.data.remote.retrofit.LoginRequest { *; }
-keep class com.dakotagroupstaff.data.remote.retrofit.AttendanceRequest { *; }
-keep class com.dakotagroupstaff.data.remote.retrofit.LeaveRequest { *; }
-keep class com.dakotagroupstaff.data.remote.retrofit.LeaveSubmissionRequest { *; }
-keep class com.dakotagroupstaff.data.remote.retrofit.ApprovalRequest { *; }
-keep class com.dakotagroupstaff.data.remote.retrofit.RejectionRequest { *; }

# Prevent proguard from stripping interface information from TypeAdapter, TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Retain generic signatures of TypeToken and its subclasses with R8 version 3.0 and higher.
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# ==========================================
# KOTLIN COROUTINES
# ==========================================

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Most of volatile fields are updated with AFU and should not be mangled
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Same story for the standard library's SafeContinuation that also uses AtomicReferenceFieldUpdater
-keepclassmembers class kotlin.coroutines.SafeContinuation {
    volatile <fields>;
}

# These classes are only required by kotlinx.coroutines.debug.AgentPremain, which is only loaded when
# kotlinx-coroutines-core is used as a Java agent, so these are not needed in contexts where ProGuard is used.
-dontwarn java.lang.instrument.ClassFileTransformer
-dontwarn sun.misc.SignalHandler
-dontwarn java.lang.instrument.Instrumentation
-dontwarn sun.misc.Signal

# ==========================================
# KOIN - DEPENDENCY INJECTION
# ==========================================

-keep class org.koin.** { *; }
-keep class org.koin.core.** { *; }
-keep class org.koin.android.** { *; }

# Keep Koin modules
-keep class com.dakotagroupstaff.di.** { *; }

# ==========================================
# ROOM DATABASE
# ==========================================

-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ==========================================
# DATASTORE
# ==========================================

-keep class androidx.datastore.*.** { *; }

# ==========================================
# GLIDE
# ==========================================

-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
  *** rewind();
}

# ==========================================
# SECURITY - ANDROIDX SECURITY CRYPTO
# ==========================================

-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }

# ==========================================
# GOOGLE PLAY SERVICES - LOCATION
# ==========================================

-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ==========================================
# VIEWBINDING
# ==========================================

-keep class * implements androidx.viewbinding.ViewBinding {
    public static *** bind(android.view.View);
    public static *** inflate(android.view.LayoutInflater);
}

# ==========================================
# PARCELIZE
# ==========================================

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}
-keepnames class * implements android.os.Parcelable
-keepclassmembers class * implements android.os.Parcelable {
  public static final ** CREATOR;
}

# ==========================================
# CUSTOM APPLICATION CLASSES
# ==========================================

# Keep custom Application class
-keep class com.dakotagroupstaff.DakotaGroupStaffApp { *; }

# Keep all Activities, Fragments, Services
-keep public class * extends android.app.Activity
-keep public class * extends androidx.fragment.app.Fragment
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Keep ViewModel classes
-keep class * extends androidx.lifecycle.ViewModel {
    <init>();
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}

# ==========================================
# LEAKCANARY (DEBUG ONLY)
# ==========================================

# LeakCanary is only used in debug builds, but add rules just in case
-dontwarn com.squareup.leakcanary.**
-keep class com.squareup.leakcanary.** { *; }

# ==========================================
# GENERAL ANDROID
# ==========================================

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep native methods
-keepclassmembers class * {
    native <methods>;
}

# Keep custom views
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# ==================================================================================================
# Google Tink (from Security Crypto)
# ==================================================================================================
-dontwarn com.google.api.client.http.**
-dontwarn org.joda.time.**
-dontwarn com.google.crypto.tink.util.**