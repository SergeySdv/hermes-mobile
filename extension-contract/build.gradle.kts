plugins { id("com.android.library") }
android {
    namespace = "com.m57.hermes.extension"
    compileSdk = 37
    defaultConfig { minSdk = 26 }
    buildFeatures { aidl = true }
}
