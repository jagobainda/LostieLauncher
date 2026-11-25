package dev.jagoba.lostielauncher.util.policy

import java.util.Locale

internal data class ApkAsset(val name: String, val downloadUrl: String)

internal sealed interface ApkAssetSelection {
    data class Selected(val asset: ApkAsset) : ApkAssetSelection

    data object IncompatibleAbi : ApkAssetSelection

    data object NoApkAsset : ApkAssetSelection
}

internal object ApkAbiPolicy {
    private val ABI_PATTERN = Regex(
        """(?<![a-z0-9])(armeabi-v7a|arm64-v8a|armeabi|riscv64|x86_64|mips64|mips|x86)(?![a-z0-9])""",
    )

    fun extractAbis(assetName: String): Set<String> {
        val lower = assetName.lowercase(Locale.ROOT)
        if (!lower.endsWith(".apk")) return emptySet()
        val stem = lower.removeSuffix(".apk")
        return ABI_PATTERN.findAll(stem).map { it.groupValues[1] }.toSet()
    }

    fun select(assets: List<ApkAsset>, supportedAbis: List<String>): ApkAssetSelection {
        val apks = assets.filter { it.name.lowercase(Locale.ROOT).endsWith(".apk") }
        if (apks.isEmpty()) return ApkAssetSelection.NoApkAsset

        val indexed = apks.map { it to extractAbis(it.name) }
        for (abi in supportedAbis) {
            val normalized = abi.lowercase(Locale.ROOT)
            val match = indexed.firstOrNull { (_, abis) -> normalized in abis }
            if (match != null) return ApkAssetSelection.Selected(match.first)
        }

        val universal = indexed.firstOrNull { (_, abis) -> abis.isEmpty() }
        if (universal != null) return ApkAssetSelection.Selected(universal.first)

        return ApkAssetSelection.IncompatibleAbi
    }
}
