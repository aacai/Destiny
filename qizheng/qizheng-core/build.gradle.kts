import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    `maven-publish`
}

group = "io.github.zhiqiu"
version = "0.1.0-SNAPSHOT"

kotlin {
    android {
        namespace = "zhiqiu.qizheng.core"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    iosArm64()
    iosSimulatorArm64()

    // tyme4kt 有 wasm-js 变体，web 端可用
    wasmJs()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.tyme4kt)
            implementation(libs.kotlinx.datetime)
            // 出生节气标签来自八字核心（formatBirthTermLabel）
            implementation(project(":bazi-core"))
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.kotlin.testJunit)
            }
        }
    }
}

publishing {
    publications.withType<MavenPublication> {
        pom {
            name.set("qizheng-core")
            description.set("Kotlin Multiplatform 七政四余排盘库（星曜/十二宫/二十八宿/化曜/洞微大限）")
            url.set("https://github.com/zhiqiu/qizheng-kmp")
            licenses {
                license {
                    name.set("MIT")
                    url.set("https://opensource.org/licenses/MIT")
                }
            }
        }
    }
}
