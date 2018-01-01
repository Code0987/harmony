# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in D:\Data\All\ADK/tools/proguard/proguard-android.txt
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

-keepattributes *Annotation*
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
-dontskipnonpubliclibraryclasses
-forceprocessing
-optimizationpasses 5

-keep class * extends android.app.Activity
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}

-keep public class * extends java.lang.Exception

-dontwarn javax.annotation.**

-keep class org.jaudiotagger.** { *; }

-keep class com.h6ah4i.** { *; }

-keep class com.wang.avi.** { *; }
-keep class com.wang.avi.indicators.** { *; }
-keep class org.jsoup.**

-dontwarn org.apache.commons.**
-keep class org.apache.http.** { *; }
-dontwarn org.apache.http.**
-dontnote android.net.http.*
-dontnote org.apache.commons.codec.**
-dontnote org.apache.http.**

-keep class org.acoustid.chromaprint.**
-keep class org.acoustid.chromaprint.** { *; }

-keep class com.google.firebase.**
-keep class io.realm.**

-keep class com.github.mikephil.**

-keep class co.mobiwise.materialintro.**

-keep class com.codetroopers.betterpickers.**

-keep class com.tozny.crypto.**

-keep class com.airbnb.lottie.**

-keep class at.huber.youtubeExtractor.**

# Facebook

-keep class com.facebook.** { *; }
-keepattributes Signature
-dontwarn com.facebook.**


## Google Analytics 3.0 specific rules ##

-keep class com.google.analytics.** { *; }


## Google Play Services rules ##
-keep class * extends java.util.ListResourceBundle {
    protected java.lang.Object[][] getContents();
}
-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}
-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final *** NULL;
}
-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
-keep class com.google.android.gms.internal.** { *; }
-dontwarn com.google.android.gms.internal.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# com.google.android.gms.auth.api.signin.SignInApiOptions$Builder
# references these classes but no implementation is provided.
-dontnote com.facebook.Session
-dontnote com.facebook.FacebookSdk
-keepnames class com.facebook.Session {}
-keepnames class com.facebook.FacebookSdk {}

# android.app.Notification.setLatestEventInfo() was removed in
# Marsmallow, but is still referenced (safely)
-dontwarn com.google.android.gms.common.GooglePlayServicesUtil


## GSON 2.2.4 specific rules ##

# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# For using GSON @Expose annotation
-keepattributes *Annotation*

-keepattributes EnclosingMethod

# Gson specific classes
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }
-dontnote libcore.icu.ICU
-dontnote sun.misc.Unsafe

# OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.squareup.okhttp.** { *; }
-keep interface com.squareup.okhttp.** { *; }
-dontwarn com.squareup.okhttp.**


# Okio
-keep class sun.misc.Unsafe { *; }
-dontwarn java.nio.file.*
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn okio.**
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.ParametersAreNonnullByDefault

## Square Otto specific rules ##
## https://square.github.io/otto/ ##

-keepclassmembers class ** {
    @com.squareup.otto.Subscribe public *;
    @com.squareup.otto.Produce public *;
}
-dontnote com.squareup.otto.*


## Square Picasso specific rules ##
## https://square.github.io/picasso/ ##

-dontwarn com.squareup.okhttp.**


## Android support ##

-dontwarn android.support.design.**
-keep class android.support.design.** { *; }
-keep interface android.support.design.** { *; }
-keep public class android.support.design.R$* { *; }
-keep public class android.support.v7.widget.** { *; }
-keep public class android.support.v7.internal.widget.** { *; }
-keep public class android.support.v7.internal.view.menu.** { *; }
-keep public class * extends android.support.v4.view.ActionProvider {
    public <init>(android.content.Context);
}
# http://stackoverflow.com/questions/29679177/cardview-shadow-not-appearing-in-lollipop-after-obfuscate-with-proguard/29698051
-keep class android.support.v7.widget.RoundRectDrawable { *; }

## Crashlytics ##

-keep class com.crashlytics.** { *; }
-dontwarn com.crashlytics.**
-keep public class * extends java.lang.Exception
