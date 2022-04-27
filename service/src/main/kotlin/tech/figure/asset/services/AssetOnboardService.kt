package tech.figure.asset.services

import com.figure.classification.asset.client.client.base.ACClient
import com.google.protobuf.Message
import cosmos.tx.v1beta1.TxOuterClass
import io.provenance.client.grpc.PbClient
import io.provenance.metadata.v1.ContractSpecificationRequest
import io.provenance.metadata.v1.RecordSpecification
import io.provenance.metadata.v1.ScopeSpecificationRequest
import io.provenance.scope.encryption.proto.Encryption
import io.provenance.scope.util.MetadataAddress
import org.slf4j.LoggerFactory
import tech.figure.asset.config.AssetSpecificationProperties
import tech.figure.asset.config.ObjectStoreProperties
import tech.figure.asset.exceptions.ContractSpecNotFoundException
import tech.figure.asset.exceptions.RecordSpecNotFoundException
import tech.figure.asset.sdk.AssetUtils
import tech.figure.asset.sdk.AssetUtilsConfig
import tech.figure.asset.sdk.ObjectStoreConfig
import tech.figure.asset.sdk.SpecificationConfig
import java.security.PrivateKey
import java.security.PublicKey
import java.util.UUID

class AssetOnboardService(
    private val acClient: ACClient,
    private val pbClient: PbClient,
    objectStoreProperties: ObjectStoreProperties,
    assetSpecificationProperties: AssetSpecificationProperties,
) {

    private val logger = LoggerFactory.getLogger(AssetOnboardService::class.java)

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
    ): TxOuterClass.TxBody {
        val (scopeSpecAddress, contractSpecAddress, recordSpec) = assetType?.let { type ->
            acClient
                // All entries into the classification smart contract are lowercase. Coerce the provided value to ensure
                // casing doesn't cause false negatives
                .queryAssetDefinitionByAssetType(type.lowercase())
                .also { def -> logger.info("Found asset definition for type [${def.assetType}] with scope spec address [${def.scopeSpecAddress}]") }
                .scopeSpecAddress
        }?.let {
            val contractSpecAddress = it.getContractSpecAddress()
            Triple(it, contractSpecAddress, contractSpecAddress.getFirstRecordSpec())
        } ?: Triple(null, null, null)

        return assetUtils.buildNewScopeMetadataTransaction(
            scopeId = scopeId,
            // Query the Asset Classification Smart contract for a scope specification address for the given asset type.
            scopeSpecAddress = scopeSpecAddress,
            contractSpecAddress = contractSpecAddress,
            recordSpec = recordSpec,
            scopeHash = hash,
            owner = owner,
            additionalAudiences = additionalAudiences
        )
    }

    private fun String.getContractSpecAddress(): String = pbClient.metadataClient
        .scopeSpecification(ScopeSpecificationRequest.newBuilder().setSpecificationId(this).build())
        .scopeSpecification.specification.contractSpecIdsList.firstOrNull()
        ?.toByteArray()
        ?.let(MetadataAddress::fromBytes)
        ?.toString() ?: throw ContractSpecNotFoundException("Could not resolve a contract spec address associated with scope spec address $this")

    private fun String.getFirstRecordSpec(): RecordSpecification = pbClient.metadataClient.contractSpecification(
        ContractSpecificationRequest.newBuilder().setSpecificationId(this).setIncludeRecordSpecs(true).build()).recordSpecificationsList
        .firstOrNull()
        ?.specification
        ?: throw RecordSpecNotFoundException("Could not resolve a record spec associated with contract spec address $this")
}
