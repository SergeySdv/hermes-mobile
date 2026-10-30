plugins { id("com.android.application") }
android {
    namespace = "com.m57.hermes.rokid.glasses"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.m57.hermes.rokid.glasses.debug"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "0.1-s3-probe"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildTypes {
        debug {
            isMinifyEnabled = true
            // R8 debug mode retains SDK logging; use optimized code with the debug signer.
            isDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "../sdk-logging.pro")
        }
    }
}
dependencies {
    implementation("com.rokid.cxr:cxr-service-bridge:1.0-20260417.063502-103")
    testImplementation("junit:junit:4.13.2")
}

androidComponents.beforeVariants(androidComponents.selector().withBuildType("release")) { it.enable = false }
configurations.configureEach { exclude(group = "androidx.test") }

android.sourceSets
    .getByName("main")
    .kotlin.directories
    .add(file("../protocol").path)
