package tech.figure.asset.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.figure.extensions.uuid.toUUID
import com.google.common.io.BaseEncoding
import io.provenance.scope.encryption.ecies.ECUtils
import io.provenance.scope.encryption.util.getAddress
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import tech.figure.asset.Asset
import tech.figure.asset.config.ProvenanceProperties
import tech.figure.asset.config.ServiceKeysProperties
import tech.figure.asset.sdk.extensions.toBase64String
import tech.figure.asset.sdk.extensions.toJson
import tech.figure.asset.services.AssetOnboardService
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
        @ApiParam( value = "Allow Figure Tech Asset Manager to read this asset", defaultValue = "true", example = "true")
        @RequestParam(defaultValue = "true", required = true) permissionAssetManager: Boolean = true,
        @RequestHeader(name = "x-public-key", required = false) xPublicKey: String,
        @RequestHeader(name = "x-address", required = false) xAddress: String,
        response: HttpServletResponse
    ): TxBody {
        val scopeId = asset.id.toUUID()
        logger.info("REST request to onboard asset $scopeId")

        // store in EOS
        val hash = storeAsset(asset, xPublicKey, xAddress, permissionAssetManager)

        // set the response headers
        response.addHeader("x-asset-id", scopeId.toString())
        response.addHeader("x-asset-hash", hash)

        // create the metadata TX message
        return createScopeTx(scopeId, hash, xAddress, permissionAssetManager)
    }

    @CrossOrigin(exposedHeaders = [ "x-asset-id", "x-asset-hash" ])
    @PostMapping("/eos")
    @ApiOperation(value = "Store asset in EOS and return asset hash")
    @ApiResponse(
        message = "Returns hash (checksum) of asset stored in EOS.",
        code = 200
    )
    fun storeAssetInEOS(
        @RequestBody asset: Asset,
        @ApiParam( value = "Allow Figure Tech Asset Manager to read this asset", defaultValue = "true", example = "true")
        @RequestParam(defaultValue = "true", required = true) permissionAssetManager: Boolean = true,
        @RequestHeader(name = "x-public-key", required = false) xPublicKey: String,
        @RequestHeader(name = "x-address", required = false) xAddress: String,
        response: HttpServletResponse
    ): String {
        logger.info("REST request to store asset in EOS $asset.id")
        return storeAsset(asset, xPublicKey, xAddress, permissionAssetManager).also {
            // set the response headers
            response.addHeader("x-asset-id", asset.id)
            response.addHeader("x-asset-hash", it)
        }
    }

    @ExperimentalStdlibApi
    @CrossOrigin
    @PostMapping("/scope")
    @ApiOperation(value = "Create Metadata (scope) transaction for submission to blockchain")
    @ApiResponse(
        message = "Returns JSON encoded TX messages for writing scope to Provenance.",
        code = 200
    )
    fun submitScope(
        @RequestParam(name = "scope-id", required = true) scopeId: UUID,
        @RequestParam(name = "fact-hash", required = true) factHash: String,
        @RequestParam(defaultValue = "true", required = true) permissionAssetManager: Boolean = true,
        @RequestHeader(name = "x-address", required = false) xAddress: String
    ): TxBody {
        logger.info("REST request to create scope for asset $scopeId from hash $factHash")
        return createScopeTx(scopeId, factHash, xAddress, permissionAssetManager)
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
    ): TxBody {
        // assemble the list of additional audiences (allow Asset Manager to read data)
        val additionalAudiences: MutableSet<String> = mutableSetOf()
        if (permissionAssetManager) {
            additionalAudiences.add(
                ECUtils.convertBytesToPublicKey(
                    BaseEncoding.base64().decode(serviceKeysProperties.assetManager)
                ).getAddress(provenanceProperties.isMainnet)
            )
        }

        // create the metadata TX message
        val txBody = assetOnboardService.buildNewScopeMetadataTransaction(
            scopeId,
            factHash,
            xAddress,
            additionalAudiences,
        )

        return TxBody(
            json = ObjectMapper().readValue(txBody.toJson(), ObjectNode::class.java),
            base64 = txBody.messagesList.map { it.toByteArray().toBase64String() }
        )
    }

}
