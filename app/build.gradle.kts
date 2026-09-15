import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val signingProperties = Properties()
val signingPropertiesFile = rootProject.file("other/signing/keystore.properties")
if (signingPropertiesFile.exists()) {
    signingPropertiesFile.inputStream().use(signingProperties::load)
}

// CI (GitHub Actions) passes the keystore path and passwords via env.
// Local builds keep using other/signing/keystore.properties.
val ciKeystore = System.getenv("HC_KEYSTORE")
val ciStorePassword = System.getenv("APP_PASSWD")
val ciKeyAlias = System.getenv("APP_SIG_KEY_ALIAS")
val ciKeyPassword = System.getenv("APP_SIG_PASSWD")

android {
    namespace = "com.aut.hypercapsule"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.aut.hypercapsule"
        minSdk = 34
        targetSdk = 37
        versionCode = 14
        versionName = "1.0.4"
    }

    signingConfigs {
        create("release") {
            val storePath = ciKeystore ?: signingProperties.getProperty("storeFile")
            storePath?.let { storeFile = file(it) }
            storePassword = ciStorePassword ?: signingProperties.getProperty("storePassword")
            keyAlias = ciKeyAlias ?: signingProperties.getProperty("keyAlias")
            keyPassword = ciKeyPassword ?: signingProperties.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
                .takeIf { it.storeFile?.exists() == true }
                ?: signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    packaging {
        resources {
            merges += "META-INF/xposed/*"
            excludes += "**"
        }
    }
    lint {
        abortOnError = true
        checkReleaseBuilds = true
        // Android Studio in the target environment supports AGP up to 9.1.0.
        disable += "AndroidGradlePluginVersion"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.libxposed.service)
    implementation(libs.miuix.ui)
    implementation(libs.miuix.preference)
    implementation(libs.miuix.icons)
    implementation(libs.miuix.blur)
    compileOnly(libs.libxposed.api)
    testImplementation(libs.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
