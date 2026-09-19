plugins {
    alias(libs.plugins.android.application)
    // No `org.jetbrains.kotlin.android`: AGP 9 compiles Kotlin itself.
    alias(libs.plugins.kotlin.compose)
    // kotlin.serialization is pinned in the catalogue but not applied here:
    // nothing is @Serializable yet. Step 04 applies it with the DTOs.
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.android.junit)
}

android {
    namespace = "dev.jagoba.lostielauncher"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "dev.jagoba.lostielauncher"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()

        // The Android app versions itself. It is nowhere near parity with the
        // desktop launcher, so it does not borrow its version number, and this
        // is an initial value rather than a bump of anything.
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
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
        val java = JavaVersion.toVersion(libs.versions.javaTarget.get())
        sourceCompatibility = java
        targetCompatibility = java
    }

    kotlin {
        compilerOptions {
            // Warnings are failures on both sides of this monorepo.
            allWarningsAsErrors = true
        }
    }

    buildFeatures {
        compose = true
    }

    lint {
        // The desktop side treats every warning as a build break; so does this
        // one. `checkDependencies` makes the app module's report the whole
        // report, which matters once there is more than one module.
        warningsAsErrors = true
        abortOnError = true
        checkDependencies = true
        checkReleaseBuilds = false
    }

    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "/META-INF/LICENSE.md",
            "/META-INF/LICENSE-notice.md",
        )
    }
}

// Pins the JDK the build compiles with, so the bytecode does not depend on
// whichever JDK happens to be running Gradle. Gradle downloads a matching one
// if the machine has none — see the foojay resolver in settings.gradle.kts.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.javaTarget.get().toInt())
    }
}

dependencies {
    implementation(platform(libs.compose.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    debugImplementation(libs.compose.ui.tooling)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
