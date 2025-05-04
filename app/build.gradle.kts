plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.openapi.generator") version "7.12.0"
}


openApiGenerate {
    generatorName = "kotlin"
    inputSpec = "$rootDir/openapi.json"
    outputDir = "$projectDir/build"
    apiPackage = "hu.gyulakiri.ripvessel.api"
    modelPackage = "hu.gyulakiri.ripvessel.model"
    configOptions.put("serializableModel","true")
    configOptions.put("serializationLibrary","moshi")
    generateApiTests.set(false)
    generateApiDocumentation.set(false)
}

android {
    namespace = "hu.cock.ripvessel"
    compileSdk = 35

    defaultConfig {
        applicationId = "hu.cock.ripvessel"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("$projectDir/build/src/main/kotlin")
        }
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
    testImplementation(libs.junit)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.gson)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.moshi)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.hls)
    implementation(libs.androidx.media3.datasource)
}

tasks.named("preBuild") {
    dependsOn("openApiGenerate")
}

