import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.room)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "io.github.prithvimk.gymtracker"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.prithvimk.gymtracker"
        minSdk = 26
        targetSdk = 36
        // CI passes the workflow run number so every build installs over the one before it.
        versionCode = providers.environmentVariable("VERSION_CODE").orNull?.toInt() ?: 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // CI signs debug builds with one fixed key (from repository secrets) so a newer build can update
    // the installed one. Local builds without the key keep the usual per-machine debug key.
    val devKeystore = providers.environmentVariable("DEV_KEYSTORE_PATH").orNull?.let { file(it) }
    if (devKeystore != null) {
        signingConfigs.getByName("debug") {
            storeFile = devKeystore
            storePassword = providers.environmentVariable("DEV_KEYSTORE_PASSWORD").get()
            keyAlias = "gymtracker-dev"
            keyPassword = providers.environmentVariable("DEV_KEYSTORE_PASSWORD").get()
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    // Lets Robolectric migration tests read the exported Room schemas. Robolectric only sees the
    // variant's merged assets (test source set assets never reach it), so the schemas ride along in
    // debug builds. Release builds don't include them.
    sourceSets.getByName("debug").assets.srcDir("$projectDir/schemas")

    lint {
        warningsAsErrors = false
        abortOnError = true
        textReport = true
        textOutput = file("stdout")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

// Screenshot tests record into and compare against images committed here. Re-record after a UI change
// with ./gradlew recordRoborazziDebug; CI runs verifyRoborazziDebug and fails on any difference.
roborazzi {
    outputDir.set(file("src/test/screenshots"))
}

ktlint {
    version.set("1.5.0")
    android.set(true)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.kotlinx.serialization.json)
    ksp(libs.androidx.room.compiler)

    debugImplementation(libs.androidx.compose.ui.tooling)
    // Registers the empty activity that Compose and screenshot tests host their content in.
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.androidx.test.ext.junit.ktx)
    testImplementation(libs.androidx.room.testing)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
}
