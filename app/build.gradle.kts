plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// The production API address; the release workflow passes -PapiBaseUrl=... once a host exists.
val prodApiBaseUrl = providers.gradleProperty("apiBaseUrl").orElse("https://api.ceta.vn/api/v1/").get()
// versionCode follows the CI run number when it is given (-PversionCode=...), so every release installs over the last.
val releaseVersionCode = providers.gradleProperty("versionCode").map(String::toInt).orElse(1).get()
val releaseVersionName = providers.gradleProperty("versionName").orElse("1.0").get()

android {
    namespace = "com.ceta.cinema"
    compileSdk { version = release(37) }

    defaultConfig {
        applicationId = "com.ceta.cinema"
        minSdk = 26
        targetSdk = 37
        versionCode = releaseVersionCode
        versionName = releaseVersionName
        buildConfigField("String", "PAYMENT_RETURN_SCHEME", "\"ceta\"")
    }

    // One flavor per Firebase app registered in ceta-dedff: .debug, .staging and the bare id.
    flavorDimensions += "env"
    productFlavors {
        create("dev") {
            dimension = "env"
            applicationIdSuffix = ".debug"
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080/api/v1/\"")
        }
        create("staging") {
            dimension = "env"
            applicationIdSuffix = ".staging"
            buildConfigField("String", "API_BASE_URL", "\"https://staging-api.ceta.vn/api/v1/\"")
        }
        create("prod") {
            dimension = "env"
            buildConfigField("String", "API_BASE_URL", "\"$prodApiBaseUrl\"")
        }
    }

    // Signed only when the keystore comes in through the environment (the release workflow); a local build stays unsigned.
    val keystorePath = System.getenv("CETA_KEYSTORE_FILE")
    signingConfigs {
        if (keystorePath != null) create("release") {
            storeFile = file(keystorePath)
            storePassword = System.getenv("CETA_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("CETA_KEY_ALIAS")
            keyPassword = System.getenv("CETA_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            if (keystorePath != null) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions { unitTests.isReturnDefaultValues = true }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.zxing.core)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play)
    implementation(libs.googleid)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
}
