rootProject.name = "Destiny"

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":androidApp")
include(":desktopApp")
include(":shared")

include(":bazi-core")
project(":bazi-core").projectDir = file("third_party/ComposeIztro/bazi/bazi-core")

include(":bazi-ui")
project(":bazi-ui").projectDir = file("third_party/ComposeIztro/bazi/bazi-ui")

include(":qizheng-core")
project(":qizheng-core").projectDir = file("qizheng/qizheng-core")

include(":qizheng-ui")
project(":qizheng-ui").projectDir = file("qizheng/qizheng-ui")

include(":crypto-core")
project(":crypto-core").projectDir = file("crypto/crypto-core")

include(":iztro-core")
project(":iztro-core").projectDir = file("third_party/ComposeIztro/iztro-core")

include(":iztro-ui")
project(":iztro-ui").projectDir = file("third_party/ComposeIztro/shared")
