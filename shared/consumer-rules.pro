# shared 的 consumer keep rules：由 build.gradle.kts 里 android.optimization.consumerKeepRules
# 发布，随 AAR 合并进消费方（androidApp）的 R8 配置。
# 只声明 shared 自身「运行时按名字查找」的东西；Room3/Ktor/Coil/OkHttp/sqlite 等库自带规则，不重复。
# qizheng-*/liuyao-*/crypto-core 与 ComposeIztro 都是纯计算代码，无反射，无需各自维护规则文件。

# ---- kotlinx.serialization ----
# Json.encodeToString(value) / decodeFromString<T>() 走 reified 重载，
# 运行时用 Class.getDeclaredField("Companion") + serializer() 取生成的序列化器，
# 所以 @Serializable 类的 Companion 字段与 serializer() 方法名不能被改名或裁掉。
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,Signature,InnerClasses,EnclosingMethod

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
    *** Companion;
}

-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# AGP 自带那份只锁 serializer() 入口，$$serializer 类自身的成员还会被改名/裁掉；
# envelope/payload 序列器一旦缺了 Object 桥接方法，导入备份解到即崩。整类锁死。
-keepclassmembers class zhiqiu.app.destiny.**$$serializer { *; }

# ---- Room3 ----
# builder 用 Class.forName("<Database 全名>_Impl") 反射拿生成实现，
# 生成的 *_Impl 必须保留包名+类名（允许成员改名）。
-if @androidx.room3.Database class ** { *; }
-keep class <1>_Impl {
    <init>(...);
}
