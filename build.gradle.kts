// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.google.firebase.crashlytics") version "3.0.3" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
    id("com.google.devtools.ksp")        version libs.versions.ksp    apply false
    id("com.google.dagger.hilt.android") version libs.versions.hiltAndroid    apply false  // use latest stable  [oai_citation:5‡Android Developers](https://developer.android.com/training/dependency-injection/hilt-android?utm_source=chatgpt.com)

}