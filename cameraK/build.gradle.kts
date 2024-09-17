import org.jetbrains.compose.compose

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.android.library)
    //  id("convention.publication")
    id("org.jetbrains.compose")
    alias(libs.plugins.compose.compiler)
    id("com.vanniktech.maven.publish") version "0.28.0"
}

group = "com.kashif.camera_compose"
version = "1.0"

kotlin {
    jvmToolchain(11)
    androidTarget {
        publishLibraryVariants("release", "debug")
    }


    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach {
        it.binaries.framework {
            baseName = "cameraK"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kermit)
            implementation(compose.ui)
            implementation(compose.foundation)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))

        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.camera.core)
            implementation(libs.camera.camera2)
            implementation(libs.androidx.camera.view)
            implementation(libs.camera.lifecycle)
            implementation(libs.camera.extensions)
            implementation(libs.androidx.activity.compose)
        }

    }

    //https://kotlinlang.org/docs/native_objc_interop.html#export_of_kdoc_comments_to_generated_objective_c_headers
//    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
//        compilations["main"].compilerOptions.options.freeCompilerArgs.add("_Xexport_kdoc")
//    }

}

android {
    namespace = "com.kashif.camera_compose"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
    }
}
