package tech.figure.asset.sdk

data class ObjectStoreConfig(
    val url: String,
    val timeoutMs: Long,
)

data class AssetUtilsConfig(
    val osConfig: ObjectStoreConfig,
)
