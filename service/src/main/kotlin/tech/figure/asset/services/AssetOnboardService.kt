package tech.figure.asset.services

import com.google.protobuf.Message
import io.provenance.scope.encryption.domain.inputstream.DIMEInputStream
import io.provenance.scope.encryption.proto.Encryption
import tech.figure.asset.config.ObjectStoreProperties
import tech.figure.asset.sdk.AssetUtils
import tech.figure.asset.sdk.AssetUtilsConfig
import tech.figure.asset.sdk.ObjectStoreConfig
import tech.figure.asset.sdk.extensions.getEncryptedPayload
import tech.figure.asset.sdk.extensions.toJson
import java.security.PrivateKey
import java.security.PublicKey
import java.util.UUID
import java.util.concurrent.TimeUnit

class AssetOnboardService(
    private val objectStoreProperties: ObjectStoreProperties
) {

    val assetUtils: AssetUtils = AssetUtils(
        AssetUtilsConfig(
            osConfig = ObjectStoreConfig(
                url = objectStoreProperties.url,
                timeoutMs = objectStoreProperties.timeoutMs
            )
        )
    )

    // Encrypt and store a byte array asset using a random keypair for the signer
    fun encryptAndStore(
        asset: ByteArray,
        encryptPublicKey: PublicKey,
    ): ByteArray = assetUtils.encryptAndStore(asset, encryptPublicKey)

    // Encrypt and store a protobuf asset using a random keypair for the signer
    fun encryptAndStore(
        asset: Message,
        encryptPublicKey: PublicKey,
    ): ByteArray = assetUtils.encryptAndStore(asset, encryptPublicKey)

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
    fun buildNewScopeMetadataTransaction(
        owner: String,
        recordName: String,
        scopeInputs: Map<String, String>,
        scopeId: UUID
    ): String = assetUtils.buildNewScopeMetadataTransaction(owner, recordName, scopeInputs, scopeId).second.toJson()

}
