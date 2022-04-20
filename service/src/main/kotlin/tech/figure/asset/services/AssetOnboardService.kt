package tech.figure.asset.services

import com.figure.classification.asset.client.client.base.ACClient
import com.google.protobuf.Message
import cosmos.tx.v1beta1.TxOuterClass
import io.provenance.scope.encryption.proto.Encryption
import tech.figure.asset.v1beta1.Asset
import tech.figure.asset.config.AssetSpecificationProperties
import tech.figure.asset.config.ObjectStoreProperties
import tech.figure.asset.sdk.AssetUtils
import tech.figure.asset.sdk.AssetUtilsConfig
import tech.figure.asset.sdk.ObjectStoreConfig
import tech.figure.asset.sdk.SpecificationConfig
import java.security.PrivateKey
import java.security.PublicKey
import java.util.UUID

class AssetOnboardService(
    private val acClient: ACClient,
    private val objectStoreProperties: ObjectStoreProperties,
    private val assetSpecificationProperties: AssetSpecificationProperties,
) {

    val assetUtils: AssetUtils = AssetUtils(
        AssetUtilsConfig(
            osConfig = ObjectStoreConfig(
                url = objectStoreProperties.url,
                timeoutMs = objectStoreProperties.timeoutMs,
            ),
            specConfig = SpecificationConfig(
                contractSpecId = UUID.fromString(assetSpecificationProperties.contractSpecId),
                scopeSpecId = UUID.fromString(assetSpecificationProperties.scopeSpecId),
            ),
        )
    )

    // Encrypt and store a protobuf message using a random keypair for the signer
    fun encryptAndStore(
        message: Message,
        encryptPublicKey: PublicKey,
        additionalAudiences: Set<PublicKey> = emptySet()
    ): ByteArray = assetUtils.encryptAndStore(message, encryptPublicKey, additionalAudiences)

    // Get a DIME by its hash and public key
    fun getDIME(
        hash: ByteArray,
        publicKey: PublicKey
    ): Encryption.DIME = assetUtils.getDIME(hash, publicKey)

    // Retrieve an encrypted asset as a byte array by its hash and public key
    fun retrieve(
        hash: ByteArray,
        publicKey: PublicKey
    ): ByteArray = assetUtils.retrieve(hash, publicKey)

    // Retrieve an encrypted asset as a byte array with its DIME by its hash and public key
    fun retrieveWithDIME(
        hash: ByteArray,
        publicKey: PublicKey
    ): Pair<Encryption.DIME, ByteArray> = assetUtils.retrieveWithDIME(hash, publicKey)

    // Retrieve the asset as a byte array and decrypt using the provided keypair
    fun retrieveAndDecrypt(
        hash: ByteArray,
        publicKey: PublicKey,
        privateKey: PrivateKey,
    ): ByteArray = assetUtils.retrieveAndDecrypt(hash, publicKey, privateKey)

    // Retrieve the asset as a protobuf and decrypt using the provided keypair
    inline fun <reified T : Message> retrieveAndDecrypt(
        hash: ByteArray,
        publicKey: PublicKey,
        privateKey: PrivateKey,
    ): T = assetUtils.retrieveAndDecrypt<T>(hash, publicKey, privateKey)

    // Create a metadata TX message for a new scope onboard
    @ExperimentalStdlibApi
    fun buildNewScopeMetadataTransaction(
        scopeId: UUID,
        hash: String,
        owner: String,
        assetType: String? = null,
        additionalAudiences: Set<String> = emptySet(),
    ): TxOuterClass.TxBody = assetUtils.buildNewScopeMetadataTransaction(
        scopeId = scopeId,
        // Query the Asset Classification Smart contract for a scope specification address for the given asset type.
        scopeSpecAddress = assetType?.let { acClient.queryAssetDefinitionByAssetType(it).scopeSpecAddress },
        scopeHash = hash,
        owner = owner,
        additionalAudiences = additionalAudiences
    )

}
