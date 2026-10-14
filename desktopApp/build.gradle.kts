import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)
    implementation(libs.filekit.dialogs.compose)

    implementation(libs.compose.uiToolingPreview)
}

val appVersion: String by rootProject.extra
// jpackage 拒绝主版本为 0 的 app-version，0.x 阶段派生成 1.x；产物文件名由 CI 按 appVersion 重命名
val macPackageVersion = "1." + appVersion.substringAfter('.')

compose.desktop {
    application {
        mainClass = "zhiqiu.app.destiny.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            // ktor 的 java 引擎是反射加载的，jdeps 看不见，不点名就没有 java.net.http
            modules("java.net.http")
            packageName = "Destiny"
            packageVersion = appVersion
            linux { packageName = "destiny" }
            macOS { packageVersion = macPackageVersion }
        }
    }
}