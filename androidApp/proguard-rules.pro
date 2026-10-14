# androidApp 的 R8 规则：只放 App 级（崩溃可读性、Kotlin 通用坑）。
# 业务模块自己的规则写在模块的 consumer rules 里（见 shared/consumer-rules.pro），
# 随 AAR 自动合并到这里；Manifest 里的 Activity/Application 由 AGP 生成的 aapt_rules 兜底。

# ---- 崩溃堆栈可读 ----
# 保留文件名+行号，源码文件名统一改名避免泄露路径；
# 反混淆用 build/outputs/mapping/release/mapping.txt。
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin 编译 when-over-enum 生成的映射表：类被内联/改名后字段名仍需可读。
-keepclassmembers class **$WhenMappings {
    <fields>;
}
