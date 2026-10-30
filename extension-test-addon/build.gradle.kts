plugins { alias(libs.plugins.android.application) }
android {
    namespace = "com.m57.hermes.extensiontest"
    compileSdk = 37
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        applicationId = "com.m57.hermes.extensiontest"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1-s1-test"
        buildConfigField("String", "CORE_SIGNER", "\"c3053abf8d6f71fea74c0f2c9ae7dc693a6a08e3fa8b1a09c3763a52495f49f0\"")
    }
    signingConfigs {
        create("s1") {
            storeFile = file(providers.gradleProperty("s1HarnessKeystore").get())
            storePassword = "android"
            keyAlias = "s1"
            keyPassword = "android"
        }
    }
    buildTypes {
        debug { signingConfig = signingConfigs.getByName("s1") }
    }
    buildFeatures { buildConfig = true }
}
dependencies {
    implementation(project(":extension-contract"))
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
}
