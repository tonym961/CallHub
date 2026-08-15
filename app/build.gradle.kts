plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "it.iotatec.callhub"
    compileSdk = 35

    defaultConfig {
        applicationId = "it.iotatec.callhub"
        minSdk = 26
        targetSdk = 35

        // Semantic versioning MAJOR.MINOR.PATCH:
        //   MAJOR — cambiamenti radicali/incompatibili (per ora fermo a 1)
        //   MINOR — nuove funzionalità retrocompatibili
        //   PATCH — solo correzioni di bug
        // versionCode = MAJOR*10000 + MINOR*100 + PATCH (monotòno crescente per lo store).
        versionCode = 10000
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Ship only real-device ABIs (Linphone native libs are large).
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    // Two distribution builds from one codebase.
    //  - "full"  → sideload: all messenger packages monitored by default.
    //  - "play"  → Play Store: conservative, opt-in per app + disclosure required.
    flavorDimensions += "distribution"
    productFlavors {
        create("full") {
            dimension = "distribution"
            applicationIdSuffix = ".full"
            versionNameSuffix = "-full"
            resValue("string", "app_name", "CallHub Full")
            // GitHub repo used by the sideload auto-updater (full flavor only).
            buildConfigField("String", "GITHUB_OWNER", "\"tonym961\"")
            buildConfigField("String", "GITHUB_REPO", "\"CallHub\"")
        }
        create("play") {
            dimension = "distribution"
            resValue("string", "app_name", "CallHub")
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)

    // Real SIP stack (GPLv3). The app is licensed GPLv3 accordingly.
    implementation(libs.linphone.sdk)
}
