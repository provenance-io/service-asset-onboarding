package tech.figure.asset.sdk

import com.figure.wallet.pbclient.extension.toAny
import com.figure.wallet.pbclient.extension.toTxBody
import com.google.protobuf.ByteString
import com.google.protobuf.Message
import cosmos.tx.v1beta1.TxOuterClass
import io.provenance.metadata.v1.AuditFields
import io.provenance.metadata.v1.ContractSpecification
import io.provenance.metadata.v1.DefinitionType
import io.provenance.metadata.v1.InputSpecification
import io.provenance.metadata.v1.MsgWriteContractSpecificationRequest
import io.provenance.metadata.v1.MsgWriteRecordRequest
import io.provenance.metadata.v1.MsgWriteRecordSpecificationRequest
import io.provenance.metadata.v1.MsgWriteScopeRequest
import io.provenance.metadata.v1.MsgWriteScopeSpecificationRequest
import io.provenance.metadata.v1.MsgWriteSessionRequest
import io.provenance.metadata.v1.Party
import io.provenance.metadata.v1.PartyType
import io.provenance.metadata.v1.Process
import io.provenance.metadata.v1.Record
import io.provenance.metadata.v1.RecordInput
import io.provenance.metadata.v1.RecordInputStatus
import io.provenance.metadata.v1.RecordOutput
import io.provenance.metadata.v1.RecordSpecification
import io.provenance.metadata.v1.ResultStatus
import io.provenance.metadata.v1.Scope
import io.provenance.metadata.v1.ScopeSpecification
import io.provenance.metadata.v1.Session
import io.provenance.metadata.v1.SessionIdComponents
import io.provenance.objectstore.proto.Objects
import io.provenance.scope.objectstore.client.OsClient
import io.provenance.scope.encryption.crypto.Pen
import io.provenance.scope.encryption.dime.ProvenanceDIME
import io.provenance.scope.encryption.domain.inputstream.DIMEInputStream
import io.provenance.scope.encryption.ecies.ProvenanceKeyGenerator
import io.provenance.scope.encryption.model.DirectKeyRef
import io.provenance.scope.encryption.proto.Encryption
import io.provenance.scope.util.MetadataAddress
import tech.figure.asset.sdk.extensions.getEncryptedPayload
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

    /* TODO: requires new overrided `put` method in OSClient
    // Encrypt and store an asset using the specified signer public key and signature (caller must generate signature with keypair)
    fun encryptAndStore(
        asset: Message,
        encryptPublicKey: PublicKey,
        signerKey: PublicKey,
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

    // Get a DIME by its hash and public key
    fun getDIME(hash: ByteArray, publicKey: PublicKey): Encryption.DIME {
        val future = osClient.get(hash, publicKey)
        val res: DIMEInputStream = future.get(config.osConfig.timeoutMs, TimeUnit.MILLISECONDS)
        return res.dime
    }

    // Retrieve an encrypted asset as a byte array by its hash and public key
    fun retrieve(hash: ByteArray, publicKey: PublicKey): ByteArray {
        val future = osClient.get(hash, publicKey)
        val res: DIMEInputStream = future.get(config.osConfig.timeoutMs, TimeUnit.MILLISECONDS)
        return res.getEncryptedPayload()
    }

    // Retrieve an encrypted asset as a byte array with its DIME by its hash and public key
    fun retrieveWithDIME(hash: ByteArray, publicKey: PublicKey): Pair<Encryption.DIME, ByteArray> {
        val future = osClient.get(hash, publicKey)
        val res: DIMEInputStream = future.get(config.osConfig.timeoutMs, TimeUnit.MILLISECONDS)
        return Pair(res.dime, res.getEncryptedPayload())
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
    fun buildNewScopeMetadataTransaction(owner: String, recordName: String, scopeInputs: Map<String, String>, scopeId: UUID = UUID.randomUUID()): Pair<UUID, TxOuterClass.TxBody> {
        // Create a new contract specification
        val contractSpecId: UUID = UUID.randomUUID()
        val newContractSpec: ContractSpecification = ContractSpecification.newBuilder().apply {
            specificationId = ByteString.copyFrom(MetadataAddress.forContractSpecification(contractSpecId).bytes)
            hash = "dummyContractSpecHash" // TODO?
            className = "dummyContractSpecClassName"  // TODO?
            //description =
            addAllOwnerAddresses(listOf(owner))
            addAllPartiesInvolved(listOf(
                PartyType.PARTY_TYPE_OWNER
            ))
        }.build()

        // Create a new scope specification
        val scopeSpecId: UUID = UUID.randomUUID()
        val newScopeSpec: ScopeSpecification = ScopeSpecification.newBuilder().apply {
            addAllContractSpecIds(listOf(ByteString.copyFrom(MetadataAddress.forContractSpecification(contractSpecId).bytes)))
            addAllOwnerAddresses(listOf(owner))
            addAllPartiesInvolved(listOf(
                PartyType.PARTY_TYPE_OWNER
            ))
        }.build()

        // Generate a scope session identifier
        val scopeSessionId: UUID = UUID.randomUUID()

        // Create a new record specification
        val newRecordSpecification: RecordSpecification = RecordSpecification.newBuilder().apply {
            name = recordName
            specificationId = ByteString.copyFrom(MetadataAddress.forRecordSpecification(contractSpecId, recordName).bytes)
            typeName = "${recordName}Type"
            resultType = DefinitionType.DEFINITION_TYPE_RECORD // TODO?
            addAllResponsibleParties(listOf(
                PartyType.PARTY_TYPE_OWNER
            ))
            scopeInputs.forEach { (inputName, inputHash) ->
                addInputs(InputSpecification.newBuilder().apply {
                    name = inputName
                    typeName = "${inputName}Type"
                    hash = "dummyRecordSpecHash" // TODO?
                }.build())
            }
        }.build()

        // Create the new scope
        val newScope: Scope = Scope.newBuilder().apply {
            valueOwnerAddress = owner
            addAllOwners(listOf(
                Party.newBuilder().apply {
                    address = owner
                    role = PartyType.PARTY_TYPE_OWNER
                }.build()
            ))
        }.build()

        // Build TX message body
        return Pair(scopeId, listOf(
            // write-contract-specification
            MsgWriteContractSpecificationRequest.newBuilder().apply {
                specUuid = contractSpecId.toString()
                specification = newContractSpec
                addAllSigners(listOf(owner))
            }.build().toAny(),

            // write-scope-specification
            MsgWriteScopeSpecificationRequest.newBuilder().apply {
                specUuid = scopeSpecId.toString()
                specification = newScopeSpec
                addAllSigners(listOf(owner))
            }.build().toAny(),

            // write-scope
            MsgWriteScopeRequest.newBuilder().apply {
                scopeUuid = scopeId.toString()
                specUuid = scopeSpecId.toString()
                scope = newScope
                addAllSigners(listOf(owner))
            }.build().toAny(),

            // write-session
            MsgWriteSessionRequest.newBuilder().apply {
                //specUuid =
                sessionIdComponents = SessionIdComponents.newBuilder().apply {
                    scopeUuid = scopeId.toString()
                    sessionUuid = scopeSessionId.toString()
                }.build()
                addAllSigners(listOf(owner))
                session = Session.newBuilder().apply {
                    sessionId =  ByteString.copyFrom(MetadataAddress.forSession(scopeId, scopeSessionId).bytes)
                    specificationId = ByteString.copyFrom(MetadataAddress.forContractSpecification(contractSpecId).bytes)
                    addAllParties(listOf(
                        Party.newBuilder().apply {
                            address = owner
                            role = PartyType.PARTY_TYPE_OWNER
                        }.build()
                    ))
                    audit = AuditFields.newBuilder().apply {
                        createdBy = owner
                        updatedBy = owner
                        version = 1 // TODO?
                        //message =
                    }.build()
                }.build()
            }.build().toAny(),

            // write-record-specification
            MsgWriteRecordSpecificationRequest.newBuilder().apply {
                contractSpecUuid = contractSpecId.toString()
                specification = newRecordSpecification
                addAllSigners(listOf(owner))
            }.build().toAny(),

            // write-record
            MsgWriteRecordRequest.newBuilder().apply {
                contractSpecUuid = contractSpecId.toString()
                addAllParties(listOf(
                    Party.newBuilder().apply {
                        address = owner
                        role = PartyType.PARTY_TYPE_OWNER
                    }.build()
                ))
                addAllSigners(listOf(owner))
                record = Record.newBuilder().apply {
                    sessionId = ByteString.copyFrom(MetadataAddress.forSession(scopeId, scopeSessionId).bytes)
                    //specificationId =
                    name = recordName
                    process = Process.newBuilder().apply {
                        name = "dummyProcessName" // TODO?
                        hash = "dummyProcessHash" // TODO?
                        method = "dummyProcessMethod" // TODO?
                    }.build()
                    scopeInputs.forEach { (inputName, inputHash) ->
                        addInputs(RecordInput.newBuilder().apply {
                            name = inputName
                            typeName = "${inputName}Type"
                            hash = inputHash
                            status = RecordInputStatus.RECORD_INPUT_STATUS_PROPOSED
                        }.build())
                        addOutputs(RecordOutput.newBuilder().apply {
                            hash = inputHash // TODO?
                            status = ResultStatus.RESULT_STATUS_PASS // TODO?
                        }.build())
                    }
                }.build()
            }.build().toAny()
        ).toTxBody())
    }

    // TODO: Can we query using the PB client without the private key? If so, let's provide some helpers for looking up
    //       a scope by scope UUID.

}
