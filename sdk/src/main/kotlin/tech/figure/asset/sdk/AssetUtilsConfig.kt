package tech.figure.asset.sdk

import java.util.UUID

data class ObjectStoreConfig(
    val url: String,
    val timeoutMs: Long,
)

data class SpecificationConfig(
    val contractSpecId: UUID,
    val scopeSpecId: UUID,
)

data class AssetUtilsConfig(
    val osConfig: ObjectStoreConfig,
    val specConfig: SpecificationConfig,
)
