package tech.figure.asset.sdk

import com.google.protobuf.Message
import io.provenance.objectstore.proto.Objects
import io.provenance.scope.objectstore.client.OsClient
import io.provenance.scope.encryption.crypto.Pen
import io.provenance.scope.encryption.domain.inputstream.DIMEInputStream
import io.provenance.scope.encryption.ecies.ProvenanceKeyGenerator
import io.provenance.scope.encryption.model.DirectKeyRef
import java.io.ByteArrayInputStream
import java.net.URI
import java.security.PrivateKey
import java.security.PublicKey
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.reflect.full.staticFunctions

class AssetUtils (
    val config: AssetUtilsConfig,
) {

    val osClient: OsClient = OsClient(URI.create(config.osConfig.url), config.osConfig.timeoutMs)

    /*
    // Encrypt and store an asset using the specified signer public key and signature (caller must generate signature with keypair)
    fun encryptAndStore(
        asset: Message,
        encryptPublicKey: PublicKey,
        signerPublicKey: PublicKey,
        signature: ByteArray,
    ): ByteArray {
        val future = osClient.put(
            asset,
            encryptPublicKey,
            signerPublicKey,
            signature
        )
        val res: Objects.ObjectResponse = future.get(config.osConfig.timeoutMs, TimeUnit.MILLISECONDS)
        return res.hash.toByteArray()
    }
     */

    // Encrypt and store a byte array asset using a random keypair for the signer
    fun encryptAndStore(
        asset: ByteArray,
        encryptPublicKey: PublicKey,
    ): ByteArray {
        val future = osClient.put(
            ByteArrayInputStream(asset),
            encryptPublicKey,
            Pen(ProvenanceKeyGenerator.generateKeyPair(encryptPublicKey)),
            asset.size.toLong()
        )
        val res: Objects.ObjectResponse = future.get(config.osConfig.timeoutMs, TimeUnit.MILLISECONDS)
        return res.hash.toByteArray()
    }

    // Encrypt and store a protobuf asset using a random keypair for the signer
    fun encryptAndStore(
        asset: Message,
        encryptPublicKey: PublicKey,
    ): ByteArray {
        val future = osClient.put(
            asset,
            encryptPublicKey,
            Pen(ProvenanceKeyGenerator.generateKeyPair(encryptPublicKey))
        )
        val res: Objects.ObjectResponse = future.get(config.osConfig.timeoutMs, TimeUnit.MILLISECONDS)
        return res.hash.toByteArray()
    }

    fun retrieveAndDecrypt(hash: ByteArray, publicKey: PublicKey, privateKey: PrivateKey): ByteArray {
        val future = osClient.get(hash, publicKey)
        val res: DIMEInputStream = future.get(config.osConfig.timeoutMs, TimeUnit.MILLISECONDS)
        return res.getDecryptedPayload(DirectKeyRef(publicKey, privateKey)).readAllBytes()
    }

    inline fun <reified T : Message> retrieveAndDecrypt(hash: ByteArray, publicKey: PublicKey, privateKey: PrivateKey): T {
        val decryptedBytes = retrieveAndDecrypt(hash, publicKey, privateKey)
        val parser = T::class.staticFunctions.find { it.name == "parseFrom" && it.parameters.size == 1 && it.parameters[0].type.classifier == ByteArray::class }
            ?: throw IllegalStateException("Unable to find parseFrom function on ${T::class.java.name}")
        return parser.call(decryptedBytes) as T
    }

}
