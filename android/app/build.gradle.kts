import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// 签名凭据从 local.properties 读取（已 gitignore，勿提交密钥）
val keystoreProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val hasSigning = !keystoreProps.getProperty("lelebox.keystore.path").isNullOrBlank()

android {
    namespace = "com.lelebox.app"
    compileSdk = 35

    signingConfigs {
        if (hasSigning) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("lelebox.keystore.path"))
                storePassword = keystoreProps.getProperty("lelebox.keystore.pass")
                keyAlias = keystoreProps.getProperty("lelebox.key.alias")
                keyPassword = keystoreProps.getProperty("lelebox.key.pass")
            }
        }
    }

    defaultConfig {
        applicationId = "com.lelebox.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 22
        versionName = "0.4.6-m1"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    lint {
        // AGP 8.7.x 的 lint 与 Kotlin 2.1 UAST 存在已知崩溃（NonNullableMutableLiveDataDetector），
        // release 构建跳过 lintVital；CI 的 lintDebug 若同样崩溃再统一关闭
        checkReleaseBuilds = false
        abortOnError = false
    }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)
    testImplementation("junit:junit:4.13.2")
}
