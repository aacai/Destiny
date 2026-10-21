import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

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
        // 数据量小、分配慢，G1 的并发线程和辅助结构在这里不值那份常驻内存
        jvmArgs += listOf("-Xms32m", "-Xmx512m", "-XX:+UseSerialGC")

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

// macOS 的 skiko runtime jar 是双架构的：插件只把本机那份解成 Contents/app 里的 dylib，
// 另一半还留在 jar 里进包；启动参数里的 -Dskiko.library.path=$APPDIR 让运行时只读解出来的那份。
// Linux / Windows 的 jar 只有本机架构，解完就空了，那边没活可干。
tasks.matching { it.name == "createDistributable" }.configureEach {
    val appImagesDir = layout.buildDirectory.dir("compose/binaries/main/app")
    doLast {
        appImagesDir.get().asFile.walkTopDown()
            .filter { it.isFile && it.name.startsWith("skiko-awt-runtime-") && it.name.endsWith(".jar") }
            .forEach { jar ->
                val unpacked = jar.parentFile.list()?.toSet() ?: emptySet()
                val stripped = File(jar.parentFile, "${jar.name}.stripped")
                var dropped = 0
                ZipInputStream(jar.inputStream()).use { input ->
                    ZipOutputStream(stripped.outputStream()).use { output ->
                        while (true) {
                            val entry = input.nextEntry ?: break
                            if (entry.name.startsWith("libskiko-") && entry.name !in unpacked) {
                                dropped++
                            } else {
                                output.putNextEntry(ZipEntry(entry.name))
                                input.copyTo(output)
                                output.closeEntry()
                            }
                        }
                    }
                }
                if (dropped == 0) stripped.delete()
                else {
                    jar.delete()
                    stripped.renameTo(jar)
                }
            }
    }
}

// jpackage 的 dmg 会往卷根塞背景图、卷图标、.DS_Store 和 .fseventsd，
// Applications 还是 Finder alias 而不是软链。app 镜像已经建好，最后这层壳自己用 hdiutil 打。
val packageDmgFromAppImage = tasks.register<Exec>("packageDmgFromAppImage") {
    dependsOn("createDistributable")

    val appImages = layout.buildDirectory.dir("compose/binaries/main/app").get().asFile
    val staging = layout.buildDirectory.dir("compose/tmp/dmgStaging").get().asFile
    val dmg = layout.buildDirectory.file("compose/binaries/main/dmg/Destiny-$macPackageVersion.dmg").get().asFile

    inputs.dir(appImages)
    outputs.file(dmg)

    commandLine(
        "sh", "-c",
        """
        set -eu
        rm -rf '$staging'
        mkdir -p '${dmg.parent}' '${staging.parent}'
        cp -R '${appImages}/.' '$staging/'
        ln -s /Applications '$staging/Applications'
        rm -f '$dmg'
        hdiutil create -volname Destiny -srcfolder '$staging' -ov -format UDZO -imagekey zlib-level=9 '$dmg'
        """.trimIndent(),
    )
}

tasks.matching { it.name == "packageDmg" }.configureEach {
    // 不关掉它的话，jpackage 会把上面刚打好的那份盖回去。
    onlyIf { false }
    dependsOn(packageDmgFromAppImage)
}
