import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Release signing key: from keystore.properties locally (never committed), or from environment
// variables in CI. Without one, release builds fall back to the debug key (fine for your own device,
// but every machine's debug key differs, so such builds can't update each other).
val keystoreProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
fun signingValue(prop: String, env: String): String? = keystoreProps.getProperty(prop) ?: System.getenv(env)
val releaseStoreFile = signingValue("storeFile", "ZP_KEYSTORE_PATH")?.let { rootProject.file(it) }?.takeIf { it.exists() }

android {
    namespace = "com.zunepocket"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.zunepocket"
        minSdk = 33
        targetSdk = 34
        // CI passes these from the release tag; local builds use the defaults.
        versionCode = (findProperty("versionCode") as String?)?.toInt() ?: 3
        versionName = (findProperty("versionName") as String?) ?: "0.3.0"
    }

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = signingValue("storePassword", "ZP_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "ZP_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "ZP_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
