import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

dependencies {
    // Whiteboard UPS (DrawBox): drawing engine + reusable toolbars.
    implementation(project(":DrawBox"))
    implementation(project(":drawbox-ui"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.material.icons.core)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "com.hyperkit"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.hyperkit"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
            storeFile = file(keystorePath)
            storePassword = System.getenv("STORE_PASSWORD")
            keyAlias = "upload"
            keyPassword = System.getenv("KEY_PASSWORD")
        }
        create("debugConfig") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debugConfig")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

// drawbox-ui ships its toolbar icons as Compose Resources (package
// "io.ak1.drawbox.ui.resources"). The Kotlin-Multiplatform Android library
// plugin does not bundle those into a consuming app's assets, so mirror them
// by hand. This is the same workaround DrawBox's own sample app uses.
tasks.register<Copy>("copyDrawBoxUiComposeResources") {
    delete("src/main/assets/composeResources/io.ak1.drawbox.ui.resources")
    from("../ups/drawbox/drawbox-ui/src/commonMain/composeResources")
    into("src/main/assets/composeResources/io.ak1.drawbox.ui.resources")
}

tasks.named("preBuild") {
    dependsOn("copyDrawBoxUiComposeResources")
}
