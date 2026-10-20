plugins {
  alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.webviewshelldemo"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.webviewshelldemo"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    flavorDimensions += "env"
    productFlavors {
        create("prod") {
            dimension = "env"
        }
        create("localTest") {
            dimension = "env"
            applicationIdSuffix = ".localtest"
            versionNameSuffix = "-localTest"
        }
        create("competitionJudge") {
            dimension = "env"
            applicationIdSuffix = ".competitionjudge"
            versionNameSuffix = "-competitionJudge"
        }
        create("competitionJudgeLocal") {
            dimension = "env"
            applicationIdSuffix = ".competitionjudge.local"
            versionNameSuffix = "-competitionJudgeLocal"
        }
        create("competitionTheoryLocal") {
            dimension = "env"
            applicationIdSuffix = ".competitiontheory.local"
            versionNameSuffix = "-competitionTheoryLocal"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      aidl = false
      buildConfig = false
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

dependencies {
    implementation(files("libs/fplibs-reader.jar"))
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.5.3")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core:1.15.0")
}
