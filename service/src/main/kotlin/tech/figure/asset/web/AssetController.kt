package tech.figure.asset.web

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/asset")
@Api(value = "Assets", tags = ["Assets"], description = "Onboard asset endpoints")
class AssetController() {

    private var logger = LoggerFactory.getLogger(AssetController::class.java)

    @PostMapping
    @ApiOperation(value = "Onboard an asset")
    @ApiResponse(
        message = "Returns the asset hash.",
        code = 200
    )
    fun onboard(@RequestBody data: String): String {
        logger.info("REST request to onboard asset")

        // TODO: extract JWT from request and get publicKey
        // TODO:

        return ""
    }

}
