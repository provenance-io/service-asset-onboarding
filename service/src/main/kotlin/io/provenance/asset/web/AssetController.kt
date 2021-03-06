package io.provenance.asset.web

import com.fasterxml.jackson.databind.MappingIterator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import com.google.common.io.BaseEncoding
import cosmos.tx.v1beta1.TxOuterClass
import io.provenance.asset.config.ProvenanceProperties
import io.provenance.asset.config.ServiceKeysProperties
import io.provenance.asset.sdk.extensions.toBase64String
import io.provenance.asset.sdk.extensions.toJson
import io.provenance.asset.services.AssetOnboardService
import io.provenance.client.protobuf.extensions.toTxBody
import io.provenance.scope.encryption.ecies.ECUtils
import io.provenance.scope.encryption.util.getAddress
import io.provenance.scope.util.toUuid
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import tech.figure.asset.v1beta1.Asset
import tech.figure.asset.v1beta1.AssetOuterClassBuilders.Asset
import tech.figure.proto.util.FileNFT
import tech.figure.proto.util.toProtoAny
import java.security.PublicKey
import java.util.*
import javax.servlet.http.HttpServletResponse


data class TxBody(
    val json: ObjectNode,
    val base64: List<String>
)

@RestController
@RequestMapping("/api/v1/asset")
@Api(value = "Assets", tags = ["Assets"], description = "Onboard asset endpoints")
class AssetController(
    private val assetOnboardService: AssetOnboardService,
    private val provenanceProperties: ProvenanceProperties,
    private val serviceKeysProperties: ServiceKeysProperties
) {

    private var logger = LoggerFactory.getLogger(AssetController::class.java)

    @ExperimentalStdlibApi
    @CrossOrigin(exposedHeaders = [ "x-asset-id", "x-asset-hash" ])
    @PostMapping
    @ApiOperation(value = "Onboard an asset (Store asset in EOS and build scope for blockchain submission.)")
    @ApiResponse(
        message = "Returns JSON encoded TX messages for writing scope to Provenance.",
        code = 200
    )
    fun onboard(
        @RequestBody asset: Asset,
        @ApiParam(value = "Asset type being onboarded", example = "heloc")
        @RequestParam(required = false) type: String?,
        @ApiParam(value = "Allow Provenance Blockchain Asset Manager to read this asset", defaultValue = "true", example = "true")
        @RequestParam(defaultValue = "true", required = true) permissionAssetManager: Boolean = true,
        @RequestHeader(name = "x-public-key", required = false) xPublicKey: String,
        @RequestHeader(name = "x-address", required = false) xAddress: String,
        response: HttpServletResponse
    ): TxBody {
        val assetId = asset.id.value.toUuid()
        logger.info("REST request to onboard asset $assetId${if (type != null) " and type $type" else ""}")

        // store in EOS
        val hash = storeAsset(asset, xPublicKey, xAddress, permissionAssetManager)

        // set the response headers
        response.addHeader("x-asset-id", assetId.toString())
        response.addHeader("x-asset-hash", hash)

        // create the metadata TX message
        return createScopeTx(
            scopeId = assetId,
            factHash = hash,
            xAddress = xAddress,
            permissionAssetManager = permissionAssetManager,
            assetType = type,
        )
    }

    @ExperimentalStdlibApi
    @CrossOrigin(exposedHeaders = [ "x-asset-id", "x-asset-hash" ])
    @PostMapping(value=["/file"],
        consumes = [MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE])
    @ApiOperation(value = "Create an Asset/NFT from a file")
    @ApiResponse(
        message = "Returns JSON encoded TX messages for writing scope to Provenance.",
        code = 200
    )
    fun onboardFileNFT(
        @RequestParam file: MultipartFile,
        @ApiParam(value = "Allow Provenance Blockchain Asset Manager to read this asset", defaultValue = "true", example = "true")
        @RequestParam(defaultValue = "true", required = true) permissionAssetManager: Boolean = true,
        @RequestHeader(name = "x-public-key", required = false) xPublicKey: String,
        @RequestHeader(name = "x-address", required = false) xAddress: String,
        response: HttpServletResponse
    ): TxBody {
        val assetId = UUID.randomUUID()
        logger.info("REST request to onboard a file as an asset. Using id:$assetId file:${file.originalFilename} content-type:${file.contentType}")

        val asset = Asset {
            idBuilder.value = assetId.toString()
            type = FileNFT.ASSET_TYPE
            description = file.name
            putKv(FileNFT.KEY_FILENAME, (file.originalFilename ?: file.name).toProtoAny())
            putKv(FileNFT.KEY_SIZE, file.size.toProtoAny())
            putKv(FileNFT.KEY_BYTES, file.bytes.toProtoAny())
            file.contentType?.let {
                putKv(FileNFT.KEY_CONTENT_TYPE, it.toProtoAny())
            }
        }

        // store in EOS
        val hash = storeAsset(asset, xPublicKey, xAddress, permissionAssetManager)

        // set the response headers
        response.addHeader("x-asset-id", assetId.toString())
        response.addHeader("x-asset-hash", hash)

        // create the metadata TX message
        return createScopeTx(assetId, hash, xAddress, permissionAssetManager)
    }

    @ExperimentalStdlibApi
    @CrossOrigin(exposedHeaders = [ "x-asset-ids", "x-asset-hashes" ])
    @PostMapping(value=["/mx"],
        consumes = [MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE])
    @ApiOperation(value = "Create one or more Asset(s)/NFT(s) from a CSV file")
    @ApiResponse(
        message = "Returns JSON encoded TX messages for writing scope(s) to Provenance.",
        code = 200
    )
    fun onboardMX(
        @RequestParam file: MultipartFile,
        @ApiParam(value = "Asset type being onboarded", example = "heloc")
        @RequestParam(required = true) type: String,
        @ApiParam(value = "Allow Provenance Blockchain Asset Manager to read this asset", defaultValue = "true", example = "true")
        @RequestParam(defaultValue = "true", required = true) permissionAssetManager: Boolean = true,
        @RequestHeader(name = "x-public-key", required = false) xPublicKey: String,
        @RequestHeader(name = "x-address", required = false) xAddress: String,
        response: HttpServletResponse
    ): TxBody {

        val schema = CsvSchema.emptySchema().withHeader()
        val csvMapper = CsvMapper()

        val assetEntriesIterator: MappingIterator<Map<String, String>> = csvMapper.reader().forType(Map::class.java)
            .with(schema)
            .readValues(file.inputStream)

        val assetEntries: List<Map<String, String>> = assetEntriesIterator.readAll()

        logger.info("REST request to onboard ${assetEntries.size} Asset(s)/NFT(s) from a CSV file for asset type $type.")

        val onboardedAssetMetadata: MutableList<Pair<UUID, String>> = mutableListOf()

        assetEntries.forEach { assetDataMap ->
            val assetId = UUID.randomUUID()

            val asset = Asset {
                idBuilder.value = assetId.toString()
                this.type = type
                description = "Asset $assetId"
                assetDataMap.forEach { key, value ->
                    putKv(key, value.toProtoAny())
                }
            }

            // store in EOS
            val assetHash = storeAsset(asset, xPublicKey, xAddress, permissionAssetManager)
            onboardedAssetMetadata.add(Pair(assetId, assetHash))
        }

        // set the response headers
        response.addHeader("x-asset-ids", onboardedAssetMetadata.joinToString { it.first.toString() })
        response.addHeader("x-asset-hashes", onboardedAssetMetadata.joinToString { it.second })

        // create the metadata TX message
        return createScopesTx(
            scopeIdAndHashes = onboardedAssetMetadata,
            xAddress = xAddress,
            permissionAssetManager = permissionAssetManager,
            assetsType = null/*type*/,
        )
    }

    @CrossOrigin
    @GetMapping("/{scopeId}")
    @ApiOperation(value = "Retrieve an asset")
    @ApiResponse(
        message = "Returns JSON encoded TX messages for writing scope to Provenance.",
        code = 200
    )
    fun getAsset(
        @PathVariable scopeId: UUID,
        @RequestHeader(name = "x-public-key", required = true) xPublicKey: String
    ): String {
        logger.info("REST request to get asset $scopeId")

        // get the public key & client PB address from the headers
        val publicKey: PublicKey = ECUtils.convertBytesToPublicKey(ECUtils.decodeString(xPublicKey))

        // TODO: locate hash by scope

        return assetOnboardService.retrieveWithDIME(ByteArray(0) /*TODO*/, publicKey).let { result ->
            val dime = result.first
            val encrypted = result.second

            // TODO: return the JSON encoded DIME and the encrypted payload (should we encode the payload in the dime itself? base64?)

            ""
        }
    }

    private fun storeAsset(
        asset: Asset,
        xPublicKey: String,
        xAddress: String,
        permissionAssetManager: Boolean,
    ): String {
        val scopeId = asset.id

        // get the public key & client PB address from the headers
        val publicKey: PublicKey = ECUtils.convertBytesToPublicKey(BaseEncoding.base64().decode(xPublicKey))
        val address: String = xAddress

        // assemble the list of additional audiences (allow Asset Manager to read data)
        val additionalAudiences: MutableSet<PublicKey> = mutableSetOf()
        if (permissionAssetManager) {
            additionalAudiences.add(
                ECUtils.convertBytesToPublicKey(
                    BaseEncoding.base64().decode(serviceKeysProperties.assetManager)
                )
            )
        }

        // encrypt and store the asset to the object-store using the provided public key
        val hash = assetOnboardService.encryptAndStore(asset, publicKey, additionalAudiences).toBase64String()
        logger.info("Stored asset $scopeId with hash $hash for client $address using key $publicKey")
        return hash
    }

    @ExperimentalStdlibApi
    private fun createScopeTx(
        scopeId: UUID,
        factHash: String,
        xAddress: String,
        permissionAssetManager: Boolean,
        assetType: String? = null,
    ): TxBody {
        // assemble the list of additional audiences (allow Asset Manager to read data)
        val additionalAudiences: MutableSet<String> = mutableSetOf()
        if (permissionAssetManager) {
            additionalAudiences.add(
                ECUtils.convertBytesToPublicKey(
                    BaseEncoding.base64().decode(serviceKeysProperties.assetManager)
                ).getAddress(provenanceProperties.mainnet)
            )
        }

        // create the metadata TX message
        val txBody = assetOnboardService.buildNewScopeMetadataTransaction(
            scopeId = scopeId,
            hash = factHash,
            owner = xAddress,
            assetType = assetType,
            additionalAudiences = additionalAudiences,
        )

        return TxBody(
            json = ObjectMapper().readValue(txBody.toJson(), ObjectNode::class.java),
            base64 = txBody.messagesList.map { it.toByteArray().toBase64String() }
        )
    }

    @ExperimentalStdlibApi
    private fun createScopesTx(
        scopeIdAndHashes: List<Pair<UUID, String>>,
        xAddress: String,
        permissionAssetManager: Boolean,
        assetsType: String? = null
    ): TxBody {
        // assemble the list of additional audiences (allow Asset Manager to read data)
        val additionalAudiences: MutableSet<String> = mutableSetOf()
        if (permissionAssetManager) {
            additionalAudiences.add(
                ECUtils.convertBytesToPublicKey(
                    BaseEncoding.base64().decode(serviceKeysProperties.assetManager)
                ).getAddress(provenanceProperties.mainnet)
            )
        }

        val txBodies = scopeIdAndHashes.map {
            assetOnboardService.buildNewScopeMetadataTransaction(
                scopeId = it.first,
                hash = it.second,
                owner = xAddress,
                assetType = assetsType,
                additionalAudiences = additionalAudiences,
            )
        }

        return TxBody(
            json = ObjectMapper().readValue(
                txBodies.flatMap { txBody ->
                    txBody.messagesList.map { it }
                }.toTxBody().toJson(), ObjectNode::class.java),
            base64 = txBodies.flatMap { txBody ->
                txBody.messagesList.map {
                    it.toByteArray().toBase64String()
                }
            }
        )
    }

}
