package tech.figure.asset.sdk

data class ObjectStoreConfig(
    val host: String,
    val port: UShort,
    val secure: Boolean,
    val timeoutMs: Long
)

data class AssetUtilsConfig(
    val osConfig: ObjectStoreConfig,
)
