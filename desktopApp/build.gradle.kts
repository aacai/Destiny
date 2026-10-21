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

        buildTypes.release.proguard {
            configurationFiles.from(project.file("proguard-desktop.pro"))
        }
    }
}

// 打包只留当前操作系统用得上的二进制：
// skiko 的 runtime jar 里还压着另一半架构的 dylib（插件只解本机那份到同级目录），
// sqlite-jdbc 的 jar 里则压着 linux/osx/windows 三份 native，各平台各出各的包，多出来的纯属占地方。
val hostSqliteNative = when {
    System.getProperty("os.name").lowercase().startsWith("mac") -> "osx"
    System.getProperty("os.name").lowercase().startsWith("windows") -> "windows"
    else -> "linux"
}

tasks.matching { it.name == "createDistributable" || it.name == "createReleaseDistributable" }.configureEach {
    val binaries = layout.buildDirectory.dir("compose/binaries").get().asFile
    val sqliteNative = hostSqliteNative
    // 逻辑必须整个待在 doLast 里：引用脚本顶层函数会让 configuration cache 存不下来
    doLast {
        fun strip(jar: File, drop: (String) -> Boolean) {
            val stripped = File(jar.parentFile, "${jar.name}.stripped")
            var dropped = 0
            ZipInputStream(jar.inputStream()).use { input ->
                ZipOutputStream(stripped.outputStream()).use { output ->
                    while (true) {
                        val entry = input.nextEntry ?: break
                        if (drop(entry.name)) {
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

        binaries.walkTopDown()
            .filter { it.isDirectory && it.name == "app" && File(it, "Destiny.app").isDirectory }
            .flatMap { (File(it, "Destiny.app/Contents/app")).listFiles()?.asList() ?: emptyList() }
            .filter { it.isFile }
            .forEach { jar ->
                if (jar.name.startsWith("skiko-awt-runtime-")) {
                    // 启动参数 -Dskiko.library.path=$APPDIR 让运行时只读解出来的那份 dylib
                    val unpacked = jar.parentFile.list()?.toSet() ?: emptySet()
                    strip(jar) { it.startsWith("libskiko-") && it !in unpacked }
                }
                if (jar.name.startsWith("sqlite-bundled-jvm-")) {
                    strip(jar) { it.startsWith("natives/") && !it.startsWith("natives/$sqliteNative") }
                }
            }
    }
}

// jpackage 的 dmg 会往卷根塞背景图、卷图标、.DS_Store 和 .fseventsd，
// Applications 还是 Finder alias 而不是软链。app 镜像已经建好，最后这层壳自己用 hdiutil 打。
fun registerDmgFromAppImage(binaryDirName: String, taskName: String, createTask: String) =
    tasks.register<Exec>(taskName) {
        dependsOn(createTask)

        val appImages = layout.buildDirectory.dir("compose/binaries/$binaryDirName/app").get().asFile
        val staging = layout.buildDirectory.dir("compose/tmp/${binaryDirName}DmgStaging").get().asFile
        val dmg = layout.buildDirectory.file("compose/binaries/$binaryDirName/dmg/Destiny-$macPackageVersion.dmg").get().asFile

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

val dmgMain = registerDmgFromAppImage("main", "packageDmgFromAppImage", "createDistributable")
val dmgRelease = registerDmgFromAppImage("main-release", "packageReleaseDmgFromAppImage", "createReleaseDistributable")

tasks.matching { it.name == "packageDmg" || it.name == "packageReleaseDmg" }.configureEach {
    // 不关掉它的话，jpackage 会把上面刚打好的那份盖回去。
    onlyIf { false }
    dependsOn(if (name == "packageDmg") dmgMain else dmgRelease)
}
