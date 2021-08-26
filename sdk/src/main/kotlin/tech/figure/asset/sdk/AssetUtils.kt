package tech.figure.asset.sdk

import com.figure.wallet.pbclient.extension.toAny
import com.figure.wallet.pbclient.extension.toTxBody
import com.google.protobuf.ByteString
import com.google.protobuf.Message
import cosmos.tx.v1beta1.TxOuterClass
import io.provenance.metadata.v1.MsgWriteRecordRequest
import io.provenance.metadata.v1.MsgWriteScopeRequest
import io.provenance.metadata.v1.Party
import io.provenance.metadata.v1.PartyType
import io.provenance.metadata.v1.Record
import io.provenance.metadata.v1.RecordInput
import io.provenance.metadata.v1.RecordInputStatus
import io.provenance.metadata.v1.Scope
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
import java.util.concurrent.TimeUnit
import java.util.UUID
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

    // Retrieve an asset as a byte array by its hash and decrypt using the provided key-pair
    fun retrieveAndDecrypt(hash: ByteArray, publicKey: PublicKey, privateKey: PrivateKey): ByteArray {
        val future = osClient.get(hash, publicKey)
        val res: DIMEInputStream = future.get(config.osConfig.timeoutMs, TimeUnit.MILLISECONDS)
        return res.getDecryptedPayload(DirectKeyRef(publicKey, privateKey)).readAllBytes()
    }

    // Retrieve an asset as a protobuf by its hash and decrypt using the provided key-pair
    inline fun <reified T : Message> retrieveAndDecrypt(hash: ByteArray, publicKey: PublicKey, privateKey: PrivateKey): T {
        val decryptedBytes = retrieveAndDecrypt(hash, publicKey, privateKey)
        val parser = T::class.staticFunctions.find { it.name == "parseFrom" && it.parameters.size == 1 && it.parameters[0].type.classifier == ByteArray::class }
            ?: throw IllegalStateException("Unable to find parseFrom function on ${T::class.java.name}")
        return parser.call(decryptedBytes) as T
    }

    // Builds the Provenance metadata transaction for writing a new scope to the chain
    fun buildNewScopeMetadataTransaction(owner: String, scopeId: UUID = UUID.randomUUID()): Pair<UUID, TxOuterClass.TxBody> {
        /*  TODO ....
            So many questions:
                1. Should we ne using PARTY_TYPE_ORIGINATOR for the record party role?
                2. What should the record name be? Is it the same as the input names?
                3. Given an Asset proto, should each payload be a record in the scope?
                4. If yes to #3, then maybe the record names/hashes are passed into function as a map?
                5. Can a record input status skip the RECORD_INPUT_STATUS_PROPOSED state?
         */

        val newScope: Scope = Scope.newBuilder().apply {
            setScopeId(ByteString.copyFromUtf8(scopeId.toString()))
            //addAllDataAccess()
            addAllOwners(listOf(
                Party.newBuilder().apply {
                    address = owner
                    role = PartyType.PARTY_TYPE_OWNER
                }.build()
            ))
            valueOwnerAddress = owner
            //specificationId =
        }.build()

        return Pair(scopeId, listOf(
            // write-scope
            MsgWriteScopeRequest.newBuilder().apply {
                scope = newScope
                addAllSigners(listOf(owner))
            }.build().toAny(),

            // write-record
            MsgWriteRecordRequest.newBuilder().apply {
                addAllParties(listOf(
                    Party.newBuilder().apply {
                        address = owner
                        role = PartyType.PARTY_TYPE_OWNER
                    }.build()
                ))
                addAllSigners(listOf(owner))
                record = Record.newBuilder().apply {
                    name = ""
                    addInputs(RecordInput.newBuilder().apply {
                        name = ""
                        hash = ""
                        status = RecordInputStatus.RECORD_INPUT_STATUS_RECORD
                    }.build())
                }.build()
                //contractSpecUuid =
                //sessionIdComponents =
            }.build().toAny(),
        ).toTxBody())
    }

}
