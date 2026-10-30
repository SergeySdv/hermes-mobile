plugins { id("com.android.application") }
android {
    namespace = "com.m57.hermes.rokid.addon"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.m57.hermes.rokid.addon.debug"
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
    implementation("com.rokid.cxr:client-l:1.0.3")
    testImplementation("junit:junit:4.13.2")
}

abstract class BundleCompanion : DefaultTask() {
    @get:InputFile abstract val apk: RegularFileProperty

    @get:OutputDirectory abstract val outputDirectory: DirectoryProperty

    @TaskAction fun bundle() {
        val directory = outputDirectory.get().asFile
        directory.mkdirs()
        apk.get().asFile.copyTo(directory.resolve("companion.apk"), overwrite = true)
    }
}
val bundleCompanion =
    tasks.register<BundleCompanion>("bundleCompanion") {
        dependsOn(":companion:assembleDebug")
        apk.set(project(":companion").layout.buildDirectory.file("outputs/apk/debug/companion-debug.apk"))
        outputDirectory.set(layout.buildDirectory.dir("generated/companionAssets"))
    }
androidComponents.onVariants(androidComponents.selector().withBuildType("debug")) {
    it.sources.assets?.addGeneratedSourceDirectory(bundleCompanion, BundleCompanion::outputDirectory)
}

androidComponents.beforeVariants(androidComponents.selector().withBuildType("release")) { it.enable = false }
configurations.configureEach { exclude(group = "androidx.test") }

android.sourceSets
    .getByName("main")
    .kotlin.directories
    .add(file("../protocol").path)
