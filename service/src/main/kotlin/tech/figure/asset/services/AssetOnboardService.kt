package tech.figure.asset.services

import com.google.protobuf.Message
import tech.figure.asset.config.ObjectStoreProperties
import tech.figure.asset.sdk.AssetUtils
import tech.figure.asset.sdk.AssetUtilsConfig
import tech.figure.asset.sdk.ObjectStoreConfig
import java.security.PrivateKey
import java.security.PublicKey

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

}