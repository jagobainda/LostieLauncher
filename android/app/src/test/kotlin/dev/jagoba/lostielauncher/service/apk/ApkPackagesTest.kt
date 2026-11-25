package dev.jagoba.lostielauncher.service.apk

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ApkPackages")
class ApkPackagesTest {
    @Test
    fun `selects the system package installer and ignores third-party apps and the Android resolver`() {
        val selected = selectSystemInstallerPackage(
            listOf(
                InstallerCandidate("com.evil.fakeinstaller", isSystemApp = false, holdsInstallPermission = true),
                InstallerCandidate("android", isSystemApp = true, holdsInstallPermission = true),
                InstallerCandidate(
                    "com.google.android.packageinstaller",
                    isSystemApp = true,
                    holdsInstallPermission = true,
                ),
            ),
        )

        selected shouldBe "com.google.android.packageinstaller"
    }

    @Test
    fun `prefers a system candidate holding INSTALL_PACKAGES over other system handlers`() {
        val selected = selectSystemInstallerPackage(
            listOf(
                InstallerCandidate("com.oem.files", isSystemApp = true, holdsInstallPermission = false),
                InstallerCandidate("com.android.packageinstaller", isSystemApp = true, holdsInstallPermission = true),
            ),
        )

        selected shouldBe "com.android.packageinstaller"
    }

    @Test
    fun `falls back to a system candidate when none reports INSTALL_PACKAGES`() {
        val selected = selectSystemInstallerPackage(
            listOf(
                InstallerCandidate("com.thirdparty.viewer", isSystemApp = false, holdsInstallPermission = false),
                InstallerCandidate("com.oem.packageinstaller", isSystemApp = true, holdsInstallPermission = false),
            ),
        )

        selected shouldBe "com.oem.packageinstaller"
    }

    @Test
    fun `returns null when no system package installer candidate is available`() {
        selectSystemInstallerPackage(emptyList()) shouldBe null
        selectSystemInstallerPackage(
            listOf(
                InstallerCandidate("com.thirdparty.viewer", isSystemApp = false, holdsInstallPermission = false),
                InstallerCandidate("android", isSystemApp = true, holdsInstallPermission = true),
                InstallerCandidate("", isSystemApp = true, holdsInstallPermission = true),
            ),
        ) shouldBe null
    }
}
