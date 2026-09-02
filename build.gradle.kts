plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.androidx.room3) apply false
    alias(libs.plugins.kotlinSerialization) apply false
}

subprojects {
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.compose.material3") {
                useVersion(libs.versions.material3.get())
                because("Align Material3 with Compose Multiplatform ${libs.versions.composeMultiplatform.get()}")
            } else if (requested.group.startsWith("org.jetbrains.compose")) {
                useVersion(libs.versions.composeMultiplatform.get())
                because("Align Compose Multiplatform artifacts with plugin ${libs.versions.composeMultiplatform.get()}")
            }
        }
    }
}