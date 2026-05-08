import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// ===== Tinker hotfix configuration =====
// Build identifier baked into both base apk and patch — they MUST match for a patch to be applied.
val tinkerVersionName: String = "1.0.0"
val tinkerBuildFlavor: String =
    "base-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())}"

// Patch-task generation requires the Tinker Gradle plugin, which has limited AGP 8 compatibility.
// Default OFF — enable on demand via:  ./gradlew assembleRelease -PtinkerEnabled=true
val tinkerEnabled: Boolean =
    (project.findProperty("tinkerEnabled") as String?)?.toBoolean() ?: false

// When building a patch, point these to the baseline apk / mapping / r.txt produced by the base build.
val tinkerOldApkPath: String = "${project.projectDir}/tinker/old-apk/old.apk"
val tinkerApplyMappingPath: String = "${project.projectDir}/tinker/old-apk/mapping.txt"
val tinkerApplyResourcePath: String = "${project.projectDir}/tinker/old-apk/R.txt"

// TINKER_ID is what links a patch to a specific base build. Override on the CLI when building a patch:
//   ./gradlew tinkerPatchRelease -PTINKER_ID=<same-id-as-base-build> -PtinkerEnabled=true
val tinkerId: String = (project.findProperty("TINKER_ID") as String?)
    ?: "$tinkerVersionName-$tinkerBuildFlavor"

// Inject TINKER_ID into BuildConfig so the runtime can compare against patch metadata.
android {
    namespace = "com.mian.accountrecord"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mian.accountrecord"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = tinkerVersionName

        // Tinker requires multiDex
        multiDexEnabled = true

        buildConfigField("String", "TINKER_ID", "\"$tinkerId\"")

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

// Apply + configure the Tinker patch plugin only when explicitly enabled. The plugin currently has
// known issues on AGP 8.x; gating prevents day-to-day builds from breaking.
if (tinkerEnabled) {
    apply(plugin = "com.tencent.tinker.patch")

    configure<com.tencent.tinker.build.gradle.TinkerPatchExtension> {
        oldApk = tinkerOldApkPath
        ignoreWarning = false
        useSign = true
        tinkerEnable = true
    }
    extensions.configure<com.tencent.tinker.build.gradle.extension.TinkerBuildConfigExtension>("buildConfig") {
        applyMapping = tinkerApplyMappingPath
        applyResourceMapping = tinkerApplyResourcePath
        this.tinkerId = tinkerId
        keepDexApply = false
    }
    extensions.configure<com.tencent.tinker.build.gradle.extension.TinkerDexExtension>("dex") {
        dexMode = "jar"
        pattern = listOf("classes*.dex", "assets/secondary-dex-?.jar")
        // Loader classes are NEVER patched. Keep your Application + Tinker's own loader here.
        loader = listOf(
            "com.mian.accountrecord.AccountRecordApp",
            "com.mian.accountrecord.tinker.SampleApplicationLike",
            "com.tencent.tinker.loader.*"
        )
    }
    extensions.configure<com.tencent.tinker.build.gradle.extension.TinkerLibExtension>("lib") {
        pattern = listOf("lib/armeabi/*.so", "lib/armeabi-v7a/*.so", "lib/arm64-v8a/*.so")
    }
    extensions.configure<com.tencent.tinker.build.gradle.extension.TinkerResourceExtension>("res") {
        pattern = listOf("res/*", "r/*", "assets/*", "resources.arsc", "AndroidManifest.xml")
        ignoreChange = listOf()
        largeModSize = 100
    }
    extensions.configure<com.tencent.tinker.build.gradle.extension.TinkerPackageConfigExtension>("packageConfig") {
        configField("patchVersion", "1.0")
        configField("patchMessage", "tinker patch")
        configField("platform", "all")
    }
    extensions.configure<com.tencent.tinker.build.gradle.extension.TinkerSevenZipExtension>("sevenZip") {
        zipArtifact = "com.tencent.mm:SevenZip:1.1.10"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.androidx.compiler)

    // Navigation
    implementation(libs.navigation.compose)

    // LiveData Compose
    implementation(libs.runtime.livedata)

    // Charts
    implementation(libs.vico.compose.m3)

    // CSV
    implementation(libs.opencsv)

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // MMKV
    implementation(libs.mmkv)

    // Coil (image loading)
    implementation(libs.coil.compose)

    // Lottie (animations)
    implementation(libs.lottie.compose)

    // WeChat OpenSDK
    implementation("com.tencent.mm.opensdk:wechat-sdk-android:6.8.28")

    // Alipay SDK
    implementation("com.alipay.sdk:alipaysdk-android:15.8.17")

    // Tinker hotfix runtime (always shipped — patch loading must be available in production)
    implementation(libs.tinker.android.lib)
    // androidx.multidex (required for minSdk < 21; harmless on 21+)
    implementation(libs.androidx.multidex)
    // Tinker annotation processor — Tinker still ships javac apt
    annotationProcessor(libs.tinker.android.anno)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
