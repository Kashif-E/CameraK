import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.compose.compose

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.android.library)
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
            implementation(libs.coil3.compose)
            implementation(libs.coil3.ktor)
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
            implementation(libs.androidx.activityCompose)
            implementation (libs.androidx.startup.runtime)
        }

    }

    //https://kotlinlang.org/docs/native_objc_interop.html#export_of_kdoc_comments_to_generated_objective_c_headers
//    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
//        compilations["main"].compilerOptions.options.freeCompilerArgs.add("_Xexport_kdoc")
//    }

}

android {
    namespace = "com.kashif.cameraK"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
    }

    publishing {
        singleVariant("release") {
            withJavadocJar()
            withSourcesJar()
        }

        // For debug variant, we exclude Javadoc and sources to prevent conflicts
        singleVariant("debug") {
            // Exclude Javadoc and sources JARs for debug variant
        }
    }
}

mavenPublishing {
    coordinates(
        groupId = "io.github.kashif-mehmood-km",
        artifactId = "camerak",
        version = "0.0.4"
    )



    pom {
        name.set("CameraK")
        description.set("Camera Library to work on both Android/iOS.")
        inceptionYear.set("2024")
        url.set("https://github.com/kashif-e/CameraK")

        licenses {
            license {
                name.set("MIT")
                url.set("https://opensource.org/licenses/MIT")
            }
        }

        developers {
            developer {
                id.set("Kashif-E")
                name.set("Kashif")
                email.set("kashismails@gmail.com")
            }
        }

        scm {
            url.set("https://github.com/kashif-e/CameraK")
        }
    }

    // Configure publishing to Maven Central
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    // Enable GPG signing for all publications
    signAllPublications()
}
