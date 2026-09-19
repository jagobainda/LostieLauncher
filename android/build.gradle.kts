// Root build script. It declares the plugin versions for the whole build and
// applies only what is genuinely repository-wide: the formatting gate.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.android.junit) apply false
    alias(libs.plugins.spotless)
}

// ktlint's settings, and the reason they are here rather than in .editorconfig
// where they belong.
//
// ktlint is configured through .editorconfig, and .editorconfig is what the IDE
// reads, so one file ought to drive both. It does not: Spotless' ktlint step
// does not pick up ./.editorconfig, with or without `setEditorConfigPath`, and
// silently formats to ktlint's own defaults instead — verified by setting
// `max_line_length = 40` there and watching nothing happen. Declaring them here
// is therefore the only way the gate enforces what it claims to.
//
// ./.editorconfig carries the same values for the IDE's benefit and says so.
// Change one, change the other.
val ktlintSettings = mapOf(
    // The IntelliJ/Android Studio style, not ktlint_official: the two differ
    // mainly in wrapping, and ktlint_official splits every method chain of more
    // than one call onto its own line, which turns an ordinary expression like
    // `libs.versions.minSdk.get().toInt()` into five. This way what the IDE
    // writes is what the gate accepts.
    "ktlint_code_style" to "intellij_idea",
    "max_line_length" to "120",
    // Composables are PascalCase by convention — they read as declarations
    // rather than calls.
    "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
)

// Formatting gate. `spotlessCheck` is this side's `dotnet format
// --verify-no-changes`; `spotlessApply` is the one you run while iterating.
spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(ktlintSettings)
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(ktlintSettings)
    }
}
