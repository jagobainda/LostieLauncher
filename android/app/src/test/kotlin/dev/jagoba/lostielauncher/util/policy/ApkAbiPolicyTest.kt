package dev.jagoba.lostielauncher.util.policy

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@DisplayName("ApkAbiPolicy")
class ApkAbiPolicyTest {
    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(
        "Kirin-0.4.0-arm64-v8a.apk, arm64-v8a",
        "Kirin-0.3.5-arm64-v8a-www.playkirin.com.apk, arm64-v8a",
        "Kirin-0.4.0-beta8-ARM64-V8A.APK, arm64-v8a",
        "Kirin-0.4.2-armeabi-v7a.apk, armeabi-v7a",
        "Kirin-0.4.2-armeabi.apk, armeabi",
        "Kirin-0.4.2-x86_64.apk, x86_64",
        "Kirin-0.4.2-x86.apk, x86",
        "Kirin-0.4.2_x86_release.apk, x86",
        "Kirin-0.4.2-riscv64.apk, riscv64",
    )
    fun `extracts the declared ABI from an APK filename`(name: String, expectedAbi: String) {
        ApkAbiPolicy.extractAbis(name) shouldBe setOf(expectedAbi)
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(
        "Kirin-0.4.2.apk",
        "Kirin-0.4.2-universal.apk",
        "Kirin-0.4.2-myx86app.apk",
        "checksums-arm64-v8a.txt",
    )
    fun `treats an APK without an ABI token and non-APK files as declaring no ABI`(name: String) {
        ApkAbiPolicy.extractAbis(name) shouldBe emptySet()
    }

    @Test
    fun `extracts multiple ABIs from a combined APK filename`() {
        ApkAbiPolicy.extractAbis("Kirin-0.4.2-arm64-v8a-armeabi-v7a.apk") shouldBe setOf("arm64-v8a", "armeabi-v7a")
    }

    @Test
    fun `selects the highest-priority supported ABI regardless of asset order`() {
        val armeabi = ApkAsset("Kirin-0.4.2-armeabi-v7a.apk", "https://example.com/v7a.apk")
        val arm64 = ApkAsset("Kirin-0.4.2-arm64-v8a.apk", "https://example.com/arm64.apk")
        val universal = ApkAsset("Kirin-0.4.2-universal.apk", "https://example.com/universal.apk")

        val selection = ApkAbiPolicy.select(
            assets = listOf(armeabi, universal, arm64),
            supportedAbis = listOf("arm64-v8a", "armeabi-v7a"),
        )

        selection shouldBe ApkAssetSelection.Selected(arm64)
    }

    @Test
    fun `falls back to a universal APK when no ABI-specific asset matches`() {
        val arm64 = ApkAsset("Kirin-0.4.2-arm64-v8a.apk", "https://example.com/arm64.apk")
        val universal = ApkAsset("Kirin-0.4.2.apk", "https://example.com/universal.apk")

        val selection = ApkAbiPolicy.select(
            assets = listOf(arm64, universal),
            supportedAbis = listOf("x86_64", "x86"),
        )

        selection shouldBe ApkAssetSelection.Selected(universal)
    }

    @Test
    fun `does not match x86 to an x86_64 APK or armeabi to an armeabi-v7a APK`() {
        val x8664 = ApkAsset("Kirin-0.4.2-x86_64.apk", "https://example.com/x86_64.apk")
        val v7a = ApkAsset("Kirin-0.4.2-armeabi-v7a.apk", "https://example.com/v7a.apk")

        ApkAbiPolicy.select(listOf(x8664), listOf("x86")) shouldBe ApkAssetSelection.IncompatibleAbi
        ApkAbiPolicy.select(listOf(v7a), listOf("armeabi")) shouldBe ApkAssetSelection.IncompatibleAbi
    }

    @Test
    fun `reports an incompatible ABI when only other-ABI APKs are present`() {
        val assets = listOf(
            ApkAsset("checksums.txt", "https://example.com/checksums.txt"),
            ApkAsset("Kirin-0.4.2-arm64-v8a.apk", "https://example.com/arm64.apk"),
        )

        ApkAbiPolicy.select(assets, listOf("x86_64", "x86")) shouldBe ApkAssetSelection.IncompatibleAbi
    }

    @Test
    fun `reports no APK asset when none of the assets is an APK`() {
        val assets = listOf(
            ApkAsset("checksums.txt", "https://example.com/checksums.txt"),
            ApkAsset("Kirin-0.4.2-arm64-v8a.zip", "https://example.com/arm64.zip"),
        )

        ApkAbiPolicy.select(assets, listOf("arm64-v8a")) shouldBe ApkAssetSelection.NoApkAsset
    }
}
