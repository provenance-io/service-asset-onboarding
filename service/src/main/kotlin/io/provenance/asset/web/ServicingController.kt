package io.provenance.asset.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.common.io.BaseEncoding
import io.provenance.scope.encryption.ecies.ECUtils
import io.provenance.scope.encryption.util.getAddress
import io.provenance.scope.util.toUuid
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import java.security.PublicKey
import java.util.UUID
import javax.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import io.provenance.asset.config.ProvenanceProperties
import io.provenance.asset.config.ServiceKeysProperties
import io.provenance.asset.sdk.extensions.toBase64String
import io.provenance.asset.sdk.extensions.toJson
import io.provenance.asset.services.AssetOnboardService
import tech.figure.servicing.v1beta1.LoanStateOuterClass.LoanState

@RestController
@RequestMapping("/api/v1/servicing")
@Api(value = "Servicing", tags = ["servicing"], description = "Onboard servicing protos")
class ServicingController(
    private val assetOnboardService: AssetOnboardService,
    private val provenanceProperties: ProvenanceProperties,
    private val serviceKeysProperties: ServiceKeysProperties
) {

    private var logger = LoggerFactory.getLogger(ServicingController::class.java)

    @ExperimentalStdlibApi
    @CrossOrigin(exposedHeaders = ["x-asset-id", "x-asset-hash"])
    @PostMapping
    @ApiOperation(value = "Onboard an asset (Store asset in EOS and build scope for blockchain submission.)")
    @ApiResponse(
        message = "Returns JSON encoded TX messages for writing scope to Provenance.",
        code = 200
    )
    fun onboard(
        @RequestBody loanState: LoanState,
        @ApiParam(value = "Allow Provenance Blockchain Asset Manager to read this asset", defaultValue = "true", example = "true")
        @RequestParam(defaultValue = "true", required = true) permissionAssetManager: Boolean = true,
        @RequestHeader(name = "x-public-key", required = false) xPublicKey: String,
        @RequestHeader(name = "x-address", required = false) xAddress: String,
        response: HttpServletResponse
    ): TxBody {
        logger.info("REST request to onboard loan state: ${loanState.loanId.value}")

        // store in EOS
        val hash = storeAsset(loanState, xPublicKey, xAddress, permissionAssetManager)

        // set the response headers
        response.addHeader("x-asset-id", loanState.loanId.value)
        response.addHeader("x-asset-hash", hash)

        // create the metadata TX message
        return createScopeTx(loanState.loanId.value.toUuid(), hash, xAddress, permissionAssetManager)
    }

    private fun storeAsset(
        loanState: LoanState,
        xPublicKey: String,
        xAddress: String,
        permissionAssetManager: Boolean,
    ): String {

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
        val hash = assetOnboardService.encryptAndStore(loanState, publicKey, additionalAudiences).toBase64String()
        logger.info("Stored loan state ${loanState.loanId.value} with hash $hash for client $address using key $publicKey")
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
                ).getAddress(provenanceProperties.isMainnet.toBoolean())
            )
        }

        // create the metadata TX message
        val txBody = assetOnboardService.buildNewScopeMetadataTransaction(
            scopeId = scopeId,
            hash = factHash,
            owner = xAddress,
            additionalAudiences = additionalAudiences,
        )

        return TxBody(
            json = ObjectMapper().readValue(txBody.toJson(), ObjectNode::class.java),
            base64 = txBody.messagesList.map { it.toByteArray().toBase64String() }
        )
    }
}
