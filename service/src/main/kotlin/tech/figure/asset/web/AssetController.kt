package tech.figure.asset.web

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
// import tech.figure.asset.sdk.extensions.toJson
import tech.figure.asset.services.AssetOnboardService
import java.util.UUID

@RestController
@RequestMapping("/api/v1/asset")
@Api(value = "Assets", tags = ["Assets"], description = "Onboard asset endpoints")
class AssetController(
    private val assetOnboardService: AssetOnboardService
) {

    private var logger = LoggerFactory.getLogger(AssetController::class.java)

    @PostMapping("/{scopeId}")
    @ApiOperation(value = "Onboard an asset")
    @ApiResponse(
        message = "Returns JSON encoded TX messages for writing scope to Provenance.",
        code = 200
    )
    fun onboard(
        @PathVariable scopeId: UUID,
        @RequestBody asset: String,
        @RequestHeader(name = "Authorization") jwt: String
    ): String {
        logger.info("REST request to onboard asset $scopeId")

        // TODO: extract JWT from request and get publicKey + address
        // TODO: convert the JSON encoded asset to a proto (assume Asset base type?)
        // TODO: encrypt and store the asset in the object store (assetOnboardService.encryptAndStore)
        // TODO: build the scope metadata transaction messages (assetOnboardService.buildNewScopeMetadataTransaction)

        // println("${result.second.toJson()}")

        return "" // TODO: return the JSON encoded TX messages
    }

}
