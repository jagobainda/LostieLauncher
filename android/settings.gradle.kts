// Settings for the Android side of the monorepo. The desktop side under
// ../desktop/ has its own toolchain and is not part of this build.

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

// Lets Gradle fetch the JDK the build asks for when the machine has no matching
// one installed. `desktop/global.json` pins the .NET SDK for the other side;
// the Java toolchain in app/build.gradle.kts is this side's equivalent, and
// this plugin is what makes that pin resolvable rather than a prerequisite.
// The version is a literal because a settings script cannot read the version
// catalogue that it is itself declaring.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    // A project that declares its own repositories is a build that resolves
    // dependencies from somewhere nobody reviewed. Fail instead.
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "LostieLauncher"

include(":app")
