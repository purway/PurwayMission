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

# dji
-dontwarn dji.**
-keep class dji.midware.** {*;}
-keepclassmembers class * extends android.app.Service
-keepclassmembers public class * extends android.view.View {
    void set*(***);
    *** get*();
}
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}
-keep,allowshrinking class android.support.** {
    <fields>;
    <methods>;
}
-keep,allowshrinking class android.media.** {
    <fields>;
    <methods>;
}
-keep,allowshrinking class * extends dji.publics.DJIUI.** {
    public <methods>;
}
-keepclassmembers enum * {
    public static <methods>;
}

-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.* { *; }
-keepattributes *Annotation*
-keepclassmembers class ** {
    @dji.thirdparty.v3.eventbus.Subscribe <methods>;
}
-keep enum dji.thirdparty.v3.eventbus.ThreadMode { *; }
-keepclassmembers class * extends dji.thirdparty.v3.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
-keep class * extends android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

-keepclassmembers class dji.** {
    <fields>;
    <methods>;
}

-keepclassmembers class com.dji.** {
    <fields>;
    <methods>;
}

-keep,allowshrinking class com.handmark.** {
    <fields>;
    <methods>;
}
-keep,allowshrinking class com.google.** {
    <fields>;
    <methods>;
}

-keep,allowshrinking class com.hp.hpl.** {
    <fields>;
    <methods>;
}

-keep,allowshrinking class net.sourceforge.** {
    <fields>;
    <methods>;
}

-keep,allowshrinking class org.bouncycastle.** {
    <fields>;
    <methods>;
}
-keep,allowshrinking class org.** {
    <fields>;
    <methods>;
}

-keep,allowshrinking class com.trilead.** {
    <fields>;
    <methods>;
}

-keep,allowshrinking class com.jcraft.** {
    <fields>;
    <methods>;
}

-keep,allowshrinking class de.mud.** {
    <fields>;
    <methods>;
}

-keep,allowshrinking class net.sourceforge.** {
    <fields>;
    <methods>;
}

-keepclasseswithmembers,allowshrinking class * {
    native <methods>;
}

# 高德相关依赖
# 集合包:3D地图3.3.2 导航1.8.0 定位2.5.0
-dontwarn com.amap.api.**
-dontwarn com.autonavi.**
-keep class com.amap.api.**{*;}
-keep class com.autonavi.**{*;}
# 地图服务
-dontwarn com.amap.api.services.**
-keep class com.map.api.services.** {*;}
# 3D地图
-dontwarn com.amap.api.mapcore.**
-dontwarn com.amap.api.maps.**
-dontwarn com.autonavi.amap.mapcore.**
-keep class com.amap.api.mapcore.**{*;}
-keep class com.amap.api.maps.**{*;}
-keep class com.autonavi.amap.mapcore.**{*;}
# 定位
-dontwarn com.amap.api.location.**
-dontwarn com.aps.**
-keep class com.amap.api.location.**{*;}
-keep class com.aps.**{*;}

#lite
-keep class com.tencent.** { *; }

#bugly
-dontwarn com.tencent.bugly.**
-keep public class com.tencent.bugly.**{*;}

#im
-keep class com.tencent.**{*;}
-dontwarn com.tencent.**

-keep class tencent.**{*;}
-dontwarn tencent.**

-keep class qalsdk.**{*;}
-dontwarn qalsdk.**