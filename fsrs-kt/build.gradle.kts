plugins {
    alias(libs.plugins.kotlin.multiplatform)
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
