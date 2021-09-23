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
import org.springframework.web.bind.annotation.RestController
import tech.figure.asset.Asset
import tech.figure.asset.exceptions.MissingPublicKeyException
import tech.figure.asset.extensions.jsonToAsset
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
    private val assetOnboardService: AssetOnboardService
) {

    private var logger = LoggerFactory.getLogger(AssetController::class.java)

    @CrossOrigin
    @PostMapping("/{scopeId}")
    @ApiOperation(value = "Onboard an asset")
    @ApiResponse(
        message = "Returns JSON encoded TX messages for writing scope to Provenance.",
        code = 200
    )
    fun onboard(
        @PathVariable scopeId: UUID,
        @RequestBody asset: ByteArray,
        @RequestHeader(name = "x-auth-jwt", required = false) xAuthJWT: String?,
        @RequestHeader(name = "x-public-key", required = false) xPublicKey: String?,
        @RequestHeader(name = "x-address", required = false) xAddress: String?
    ): TxBody {
        logger.info("REST request to onboard asset $scopeId")

        val publicKey: PublicKey
        val address: String

        // get the public key & client PB address from the headers
        if (xAuthJWT != null) {
            // TODO: decode the JWT and extract the public key and address
            throw IllegalStateException("JWT authentication unimplemented")
        } else if (xPublicKey != null && xAddress != null) {
            publicKey = ECUtils.convertBytesToPublicKey(ECUtils.decodeString(xPublicKey))
            address = xAddress
        } else {
            throw MissingPublicKeyException()
        }

        // encrypt and store the asset to the object-store using the provided public key
        val hash = asset.jsonToAsset().fold(
            { t: Throwable ->
                // TODO: if blobs are enabled, onboard as blob, otherwise throw exception
                ""
            },
            { assetObj: Asset ->
                assetOnboardService.encryptAndStore(assetObj, publicKey).toBase64String()
            }
        )
        logger.info("Stored asset $scopeId with hash $hash for client $address using key $publicKey")

        // create the metadata TX message
        val txBody = assetOnboardService.buildNewScopeMetadataTransaction(address, "AssetRecord", mapOf("Asset" to hash), scopeId)
        return TxBody(
            json = ObjectMapper().readValue(txBody.first, ObjectNode::class.java),
            base64 = txBody.second.toBase64String()
        )
    }

    @GetMapping("/{scopeId}")
    @ApiOperation(value = "Retrieve an asset")
    @ApiResponse(
        message = "Returns JSON encoded TX messages for writing scope to Provenance.",
        code = 200
    )
    fun getAsset(
        @PathVariable scopeId: UUID,
        @RequestHeader(name = "x-auth-jwt", required = false) xAuthJWT: String?,
        @RequestHeader(name = "x-public-key", required = false) xPublicKey: String?
    ): String {
        logger.info("REST request to get asset $scopeId")

        val publicKey: PublicKey

        // get the public key & client PB address from the headers
        if (xAuthJWT != null) {
            // TODO: decode the JWT and extract the public key
            throw IllegalStateException("JWT authentication unimplemented")
        } else if (xPublicKey != null) {
            publicKey = ECUtils.convertBytesToPublicKey(ECUtils.decodeString(xPublicKey))
        } else {
            throw MissingPublicKeyException()
        }

        // TODO: locate hash by scope

        return assetOnboardService.retrieveWithDIME(ByteArray(0) /*TODO*/, publicKey).let { result ->
            val dime = result.first
            val encrypted = result.second

            // TODO: return the JSON encoded DIME and the encrypted payload (should we encode the payload in the dime itself? base64?)

            ""
        }
    }

}
