# 桌面端 ProGuard 的补充规则。
# Android 那边由 AGP 自动合并各库 AAR 里的 consumer 规则；桌面这条链路只吃
# 插件自带的 default-compose-desktop-rules.pro（kotlin/skia/skiko/coroutines），
# 库自己的规则一个都不认，所以反射入口要在这里点名。

# okio 的扩展函数带 @JvmName 后缀（buffer$5ae116e 这种），ProGuard 的内联/特化改写
# 会把返回类型收窄成实现类，字节码校验当场 VerifyError。整包留着最省事，也就几百 K。
-keep class okio.** { *; }

# filekit-dialogs-compose 是对着旧版 skiko 编的，
# Image.encodeToData(EncodedImageFormat, int) 这个签名在 0.150 里已经没了。
# 不调那条图片编码路径就没影响，但 ProGuard 把未解析引用当错误拦下来。
-dontwarn io.github.vinceglb.filekit.**

# Room 用 Class.forName("<Database>_Impl") 找生成实现，静态图里没人引用它。
-if @androidx.room3.Database class ** { *; }
-keep class <1>_Impl {
    <init>(...);
}

# ProGuard 的静态图不认 JNI 绑定：native 方法在 Java 侧没人显式调用就会被删，
# 运行时按符号名去找就 UnsatisfiedLinkError（实测踩过 BundledSQLiteDriverKt.nativeThreadSafeMode）。
-keepclasseswithmembers class * {
    native <methods>;
}

# DriverManager 按 META-INF/services 里的名字反射实例化驱动。
-keep class * implements java.sql.Driver {
    <init>();
}

# ProGuard 不读 META-INF/services，ServiceLoader 找的实现类在静态图里没有引用，会被整类删掉。
# 这几条对应实测丢过的：Coil 的 ktor 网络取图器、Ktor 的 kotlinx-serialization 扩展、Ktor 引擎容器。
-keep class * implements coil3.util.FetcherServiceLoaderTarget {
    <init>();
}
-keep class * implements io.ktor.serialization.kotlinx.KotlinxSerializationExtensionProvider {
    <init>();
}
-keep class * implements io.ktor.client.HttpClientEngineContainer {
    <init>();
}

# 无参 HttpClient {} 靠按名字试探引擎，Java 引擎只在 jvm 侧被反射加载。
-keep class io.ktor.client.engine.java.** {
    <init>();
    <fields>;
}

# reified 的 encodeToString/decodeFromString 走 Class.getDeclaredField("Companion")
# + serializer()，序列化器和这两个入口不能被裁。
-keepclassmembers class **$$serializer { *; }
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
}
