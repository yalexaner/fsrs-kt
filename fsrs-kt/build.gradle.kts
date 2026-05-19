plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
}

group = "io.github.yalexaner"
version = "0.0.1-SNAPSHOT"

kotlin {
    explicitApi()
    jvmToolchain(25)

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

detekt {
    config.setFrom("$rootDir/config/detekt/detekt.yml")
    buildUponDefaultConfig = true
    parallel = true
    autoCorrect = false
    // KMP source sets aren't picked up by detekt's default scanning — wire
    // every kotlin source dir so each target's code is analysed.
    source.setFrom(kotlin.sourceSets.flatMap { it.kotlin.srcDirs })
}

dependencies {
    detektPlugins(libs.detekt.formatting)
}
