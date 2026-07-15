plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.app_movil_gastronomia"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.app_movil_gastronomia"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.example.app_movil_gastronomia.HiltTestRunner"

        buildConfigField("String", "API_BASE_URL", "\"https://tu-api-url/\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.material)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.recyclerview)
    implementation(libs.hilt.android)
    annotationProcessor(libs.hilt.compiler)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    implementation(libs.security.crypto)
    implementation(libs.signalr)
    testImplementation(libs.junit)
    testImplementation(libs.arch.core.testing)
    testImplementation(libs.org.json)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.hilt.android.testing)
    // Hilt generates *_HiltComponents + *_TestComponentDataSupplier for
    // every @HiltAndroidTest class. Without this annotation processor on
    // the androidTest source set, instrumented Hilt tests fail at runtime
    // with "missing generated file: ..._TestComponentDataSupplier".
    add("androidTestAnnotationProcessor", libs.hilt.compiler)
}