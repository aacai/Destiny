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
        // 单机小工具的 JVM 默认值全按服务器调：-Xms 取物理内存的 1/64（16G 机器上 256M）一开机就占住，
        // G1 的 remember-set、240M code cache、不设限的 Metaspace 也一样留着，而实际堆只用十几 M。
        jvmArgs += listOf(
            "-Xms32m",
            "-Xmx512m",
            "-XX:SoftMaxHeapSize=192m",
            "-XX:MinHeapFreeRatio=1",
            "-XX:MaxHeapFreeRatio=10",
            "-XX:MaxMetaspaceSize=256m",
            "-XX:ReservedCodeCacheSize=96m",
            "-XX:+UseSerialGC",
        )

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            // ktor 的 java 引擎是反射加载的，jdeps 看不见，不点名就没有 java.net.http
            modules("java.net.http")
            packageName = "Destiny"
            packageVersion = appVersion
            linux { packageName = "destiny" }
            macOS { packageVersion = macPackageVersion }
        }

        // 只有 createReleaseDistributable / packageRelease* 这条任务链会过 ProGuard，
        // 跑 debug 的 packageDmg 时这份规则根本不参与。
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
    val outward = appVersion
    val packed = macPackageVersion
    // 逻辑必须整个待在 doLast 里：引用脚本顶层函数会让 configuration cache 存不下来
    doLast {
        fun runTool(vararg command: String) {
            val process = ProcessBuilder(command.toList()).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText()
            if (process.waitFor() != 0) throw GradleException("${command.first()} 失败：$output")
        }

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

        // jpackage 不接受首段为 0 的 app-version，只能先拿 1.x 过校验，再把版本号写回真实值。
        // 后处理动了包里的文件，签名 seal 会失效，所以按包内原有 entitlements 重签一次。
        binaries.walkTopDown()
            .filter { it.isDirectory && it.name.endsWith(".app") }
            .forEach { app ->
                val plist = File(app, "Contents/Info.plist").path
                for (key in listOf("CFBundleShortVersionString", "CFBundleVersion")) {
                    runTool("plutil", "-replace", key, "-string", outward, plist)
                }
                val cfg = File(app, "Contents/app/${app.nameWithoutExtension}.cfg")
                if (cfg.isFile) {
                    cfg.writeText(cfg.readText().replace("-Djpackage.app-version=$packed", "-Djpackage.app-version=$outward"))
                }
                val ent = File(app.parentFile, "${app.name}.entitlements.plist")
                // 转储 entitlements 时 stderr 不能并进文件：codesign 会打一行 "Executable=…"，进了就是非法 blob。
                val dumped = ProcessBuilder("codesign", "-d", "--entitlements", ":-", app.path)
                    .redirectOutput(ent)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start()
                    .waitFor() == 0 && ent.length() > 0
                val entitlements = if (dumped) listOf("--entitlements", ent.path) else emptyList()
                runTool("codesign", "--force", "--options", "runtime", "--sign", "-", *entitlements.toTypedArray(), app.path)
                ent.delete()
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
        val dmg = layout.buildDirectory.file("compose/binaries/$binaryDirName/dmg/Destiny-$appVersion.dmg").get().asFile

        inputs.dir(appImages)
        outputs.file(dmg)

        commandLine(
            "sh", "-c",
            """
            set -eu
            rm -rf '$staging' '${dmg.parent}'
            mkdir -p '${dmg.parent}' '${staging.parent}'
            cp -R '${appImages}/.' '$staging/'
            ln -s /Applications '$staging/Applications'
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
