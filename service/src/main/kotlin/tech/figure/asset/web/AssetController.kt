package tech.figure.asset.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.provenance.scope.encryption.ecies.ECUtils
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import tech.figure.asset.Asset
import tech.figure.asset.config.ServiceKeysProperties
import tech.figure.asset.extensions.base64ToPublicKey
import tech.figure.asset.extensions.toUUID
import tech.figure.asset.sdk.extensions.toBase64String
import tech.figure.asset.services.AssetOnboardService
import java.security.PublicKey
import java.util.UUID

data class TxBody(
    val json: ObjectNode,
    val base64: String
)

@RestController
@RequestMapping("/api/v1/asset")
@Api(value = "Assets", tags = ["Assets"], description = "Onboard asset endpoints")
class AssetController(
    private val assetOnboardService: AssetOnboardService,
    private val serviceKeysProperties: ServiceKeysProperties
) {

    private var logger = LoggerFactory.getLogger(AssetController::class.java)

    @CrossOrigin
    @PostMapping
    @ApiOperation(value = "Onboard an asset (Store asset in EOS and build scope for blockchain submission.)")
    @ApiResponse(
        message = "Returns JSON encoded TX messages for writing scope to Provenance.",
        code = 200
    )
    fun onboard(
        @RequestBody asset: Asset,
        @RequestParam(defaultValue = "true") permissionAssetManager: Boolean,
        @RequestHeader(name = "x-public-key", required = true) xPublicKey: String,
        @RequestHeader(name = "x-address", required = true) xAddress: String
    ): TxBody {
        val scopeId = asset.id
        logger.info("REST request to onboard asset $scopeId")

        // get the public key & client PB address from the headers
        val publicKey: PublicKey = xPublicKey.base64ToPublicKey()
        val address: String = xAddress

        // assemble the list of additional audiences
        val additionalAudiences: MutableSet<PublicKey> = mutableSetOf()
        if (permissionAssetManager) {
            additionalAudiences.add(serviceKeysProperties.assetManager.base64ToPublicKey())
        }

        // encrypt and store the asset to the object-store using the provided public key
        val hash = assetOnboardService.encryptAndStore(asset, publicKey, additionalAudiences).toBase64String()
        logger.info("Stored asset $scopeId with hash $hash for client $address using key $publicKey")

        // create the metadata TX message
        val txBody = assetOnboardService.buildNewScopeMetadataTransaction(address, "AssetRecord", mapOf("Asset" to hash), scopeId.toUUID())
        return TxBody(
            json = ObjectMapper().readValue(txBody.first, ObjectNode::class.java),
            base64 = txBody.second.toBase64String()
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

}
