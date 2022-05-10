package tech.figure.asset.sdk

import com.google.protobuf.Message
import cosmos.tx.v1beta1.TxOuterClass
import io.provenance.client.protobuf.extensions.toAny
import io.provenance.client.protobuf.extensions.toTxBody
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
import io.provenance.metadata.v1.RecordInput
import io.provenance.metadata.v1.RecordInputStatus
import io.provenance.metadata.v1.RecordOutput
import io.provenance.metadata.v1.RecordSpecification
import io.provenance.metadata.v1.ResultStatus
import io.provenance.objectstore.proto.Objects
import io.provenance.scope.objectstore.client.OsClient
import io.provenance.scope.encryption.crypto.Pen
import io.provenance.scope.encryption.domain.inputstream.DIMEInputStream
import io.provenance.scope.encryption.ecies.ProvenanceKeyGenerator
import io.provenance.scope.encryption.model.DirectKeyRef
import io.provenance.scope.encryption.proto.Encryption
import io.provenance.scope.util.MetadataAddress
import io.provenance.scope.util.toByteString
import tech.figure.asset.sdk.extensions.getEncryptedPayload
import java.net.URI
import java.security.PrivateKey
import java.security.PublicKey
import java.util.concurrent.TimeUnit
import java.util.UUID
import kotlin.reflect.full.staticFunctions

data class RecordInputSpec(
    val name: String,
    val typeName: String,
    val hash: String,
)

class AssetUtils (
    val config: AssetUtilsConfig,
) {

    companion object {
        // Contract specification
        const val ContractSpecClassName = "tech.figure.asset.OnboardAsset"
        const val ContractSpecSourceHash = "AB43F752EBE5DC3E52EA2A9242136C35CD5C73C6E4EFCD44A70C32F8E43DC26F" // sha356(ContractSpecClassName)

        // Record specification
        const val RecordSpecName = "Asset"
        const val RecordSpecTypeName = "tech.figure.asset.v1beta1.Asset"
        val RecordSpecInputs = listOf(RecordInputSpec(
            name = "AssetHash",
            typeName = "String",
            hash = "4B6A6C36E8B2622334C244B46799A47DBEAAF94E9D5B7637BC12A3A4988A62C0", // sha356(RecordSpecInputs.name)
        ))

        // Record process
        const val RecordProcessName = "OnboardAssetProcess"
        const val RecordProcessMethod = "OnboardAsset"
        const val RecordProcessHash = "32D60974A2B2E9A9D9E93D9956E3A7D2BD226E1511D64D1EA39F86CBED62CE78" // sha356(RecordProcessMethod)
    }

    val osClient: OsClient = OsClient(URI.create(config.osConfig.url), config.osConfig.timeoutMs)

    /* TODO: requires new overrided `put` method in OSClient
    // Encrypt and store an asset using the specified signer public key and signature (caller must generate signature with keypair)
    fun encryptAndStore(
        asset: Message,
        encryptPublicKey: PublicKey,
        signerKey: PublicKey,
        signature: ByteArray,
        additionalAudiences: Set<PublicKey> = emptySet()
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

    // Encrypt and store a protobuf message using a random keypair for the signer
    fun encryptAndStore(
        message: Message,
        encryptPublicKey: PublicKey,
        additionalAudiences: Set<PublicKey> = emptySet()
    ): ByteArray {
        val future = osClient.put(
            message,
            encryptPublicKey,
            Pen(ProvenanceKeyGenerator.generateKeyPair(encryptPublicKey)),
            additionalAudiences
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

    // Builds the Provenance metadata transaction for writing contract/scope/record specifications to the chain
    fun buildAssetSpecificationMetadataTransaction(owner: String): TxOuterClass.TxBody {
        return listOf(

            // write-contract-specification
            MsgWriteContractSpecificationRequest.newBuilder().apply {
                specificationBuilder
                    .setSpecificationId(MetadataAddress.forContractSpecification(config.specConfig.contractSpecId).bytes.toByteString())
                    .setClassName(ContractSpecClassName)
                    .setHash(ContractSpecSourceHash)
                    .addAllOwnerAddresses(listOf(owner))
                    .addAllPartiesInvolved(listOf(
                        PartyType.PARTY_TYPE_OWNER
                    ))
            }.addAllSigners(listOf(owner)).build().toAny(),

            // write-scope-specification
            MsgWriteScopeSpecificationRequest.newBuilder().apply {
                specificationBuilder
                    .setSpecificationId(MetadataAddress.forScopeSpecification(config.specConfig.scopeSpecId).bytes.toByteString())
                    .addAllContractSpecIds(listOf(
                        MetadataAddress.forContractSpecification(config.specConfig.contractSpecId).bytes.toByteString()
                    ))
                    .addAllOwnerAddresses(listOf(owner))
                    .addAllPartiesInvolved(listOf(
                        PartyType.PARTY_TYPE_OWNER
                    ))
            }.addAllSigners(listOf(owner)).build().toAny(),

            // write-record-specification
            MsgWriteRecordSpecificationRequest.newBuilder().apply {
                specificationBuilder
                    .setName(RecordSpecName)
                    .setTypeName(RecordSpecTypeName)
                    .setSpecificationId(MetadataAddress.forRecordSpecification(config.specConfig.contractSpecId, RecordSpecName).bytes.toByteString())
                    .setResultType(DefinitionType.DEFINITION_TYPE_RECORD)
                    .addAllResponsibleParties(listOf(
                        PartyType.PARTY_TYPE_OWNER
                    ))
                    .addAllInputs(RecordSpecInputs.map { InputSpecification.newBuilder().apply {
                        name = it.name
                        typeName = it.typeName
                        hash = it.hash
                    }.build() })
            }.addAllSigners(listOf(owner)).build().toAny(),

        ).toTxBody()
    }

    // Builds the Provenance metadata transaction for writing a new scope to the chain
    @ExperimentalStdlibApi
    fun buildNewScopeMetadataTransaction(
        scopeId: UUID,
        scopeHash: String,
        owner: String,
        scopeSpecAddress: String? = null,
        contractSpecAddress: String? = null,
        recordSpec: RecordSpecification? = null,
        additionalAudiences: Set<String> = emptySet(),
    ): TxOuterClass.TxBody {
        // Generate a session identifier
        val sessionId: UUID = UUID.randomUUID()

        // Create the set of all audiences (including the owner)
        val allAudiences: Set<String> = buildSet(additionalAudiences.size + 1) {
            add(owner)
            addAll(additionalAudiences)
        }

        // Create the list of all parties (including the owner)
        val allParties: List<Party> = buildList(additionalAudiences.size + 1) {
            add(Party.newBuilder().apply {
                address = owner
                role = PartyType.PARTY_TYPE_OWNER
            }.build())
            /*
            addAll(additionalAudiences.map {
                Party.newBuilder().apply {
                    address = it
                    role = PartyType.PARTY_TYPE_UNSPECIFIED
                }.build()
            })
             */
        }

        // If a scope spec address was provided, use that value. Otherwise, default out to the
        // configuration's scope spec id
        val scopeSpecMetadataAddress = scopeSpecAddress
            ?.let(MetadataAddress::fromBech32)
            ?: MetadataAddress.forScopeSpecification(config.specConfig.scopeSpecId)

        // If a contract spec address was provided, use that value. Otherwise, default out to the
        // configuration's scope spec id
        val contractSpecMetadataAddress = contractSpecAddress
            ?.let(MetadataAddress::fromBech32)
            ?: MetadataAddress.forContractSpecification(config.specConfig.contractSpecId)

        // If a record spec name was provided, use that value. Otherwise, default out to the
        // default record name
        val resolvedRecordSpecName = recordSpec?.name ?: RecordSpecName

        val recordInputs = recordSpec?.inputsList?.map { input ->
            RecordInput.newBuilder()
                .setName(input.name)
                .setTypeName(input.typeName)
                .setHash(if (input.name == recordSpec.name) { // all record specs should have one input with the name of the spec
                    scopeHash
                } else {
                    ""
                })
                .setStatus(RecordInputStatus.RECORD_INPUT_STATUS_PROPOSED)
                .build()
        } ?: RecordSpecInputs.map {
            RecordInput.newBuilder().apply {
                name = it.name
                typeName = it.typeName
                hash = if (it.name == "AssetHash") {
                    scopeHash
                } else {
                    ""
                }
                status = RecordInputStatus.RECORD_INPUT_STATUS_PROPOSED
            }.build()
        }

        val recordOutputs = recordSpec?.inputsList?.map { input ->
            RecordOutput.newBuilder()
                .setHash(if (input.name == recordSpec.name) { // all record specs should have one input with the name of the spec
                    scopeHash
                } else {
                    ""
                }).setStatus(ResultStatus.RESULT_STATUS_PASS)
                .build()
        } ?: RecordSpecInputs.map {
            RecordOutput.newBuilder().apply {
                hash = if (it.name == "AssetHash") {
                    scopeHash
                } else {
                    ""
                }
                status = ResultStatus.RESULT_STATUS_PASS
            }.build()
        }

        // Build TX message body
        return listOf(

            // write-scope
            MsgWriteScopeRequest.newBuilder().apply {
                scopeUuid = scopeId.toString()
                specUuid = scopeSpecMetadataAddress.getPrimaryUuid().toString()
                scopeBuilder
                    .setScopeId(MetadataAddress.forScope(scopeId).bytes.toByteString())
                    .setSpecificationId(scopeSpecMetadataAddress.bytes.toByteString())
                    .setValueOwnerAddress(owner)
                    .addAllOwners(listOf(
                        Party.newBuilder().apply {
                            address = owner
                            role = PartyType.PARTY_TYPE_OWNER
                        }.build()
                    ))
                    .addAllDataAccess(allAudiences)
            }.addAllSigners(listOf(owner)).build().toAny(),

            // write-session
            MsgWriteSessionRequest.newBuilder().apply {
                sessionIdComponentsBuilder
                    .setScopeUuid(scopeId.toString())
                    .setSessionUuid(sessionId.toString())
                sessionBuilder
                    .setSessionId(MetadataAddress.forSession(scopeId, sessionId).bytes.toByteString())
                    .setSpecificationId(contractSpecMetadataAddress.bytes.toByteString())
                    .addAllParties(allParties)
                    .auditBuilder
                        .setCreatedBy(owner)
                        .setUpdatedBy(owner)
            }.addAllSigners(listOf(owner)).build().toAny(),

            // write-record
            MsgWriteRecordRequest.newBuilder().apply {
                contractSpecUuid = contractSpecMetadataAddress.getPrimaryUuid().toString()
                recordBuilder
                    .setSessionId(MetadataAddress.forSession(scopeId, sessionId).bytes.toByteString())
                    .setSpecificationId(MetadataAddress.forRecordSpecification(contractSpecMetadataAddress.getPrimaryUuid(), resolvedRecordSpecName).bytes.toByteString())
                    .setName(resolvedRecordSpecName)
                    .addAllInputs(recordInputs)
                    .addAllOutputs(recordOutputs)
                    .processBuilder
                        .setName(RecordProcessName)
                        .setMethod(RecordProcessMethod)
                        .setHash(RecordProcessHash)
            }.addAllSigners(listOf(owner)).build().toAny(),

        ).toTxBody()
    }

    // TODO: Can we query using the PB client without the private key? If so, let's provide some helpers for looking up
    //       a scope by scope UUID.

}
